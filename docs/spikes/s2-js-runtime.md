# Spike S2：QuickJS 源运行时验证

> 时间盒：8 个工作日 · 依赖：quickjs-kt 1.0.15、Android、Xcode
> 代码目录：`experiments/spikes/s2-js-runtime`（待创建）

## 1. 要回答的问题

1. `quickjs-kt` 能否在 Android、JVM 桌面、iOS 三个目标上稳定执行源脚本？
2. 宿主 API（Network、HtmlDocument、KV）是否足够跑通 Venera 兼容源的读路径？
3. 超时、取消、死循环中断是否可靠？性能与内存是否可接受？
4. Venera 兼容层需要多少 shim，能否做到"源脚本零修改"？

## 2. 范围：读路径子集

首版只验证：

```text
search / explore → comic details → chapter list → page image URLs
```

不在本 Spike 实现：登录、评论、评分、网络收藏、UI 对话框、图片二进制处理。相关 API 缺失时必须显式报错，不允许静默返回空值。

## 3. 运行时宿主 API v0

| 能力 | 接口 | 说明 |
|---|---|---|
| HTTP | `Network.get/post/put/patch/delete/fetchBytes` | 返回 status/headers/body；Cookie 由宿主管理 |
| fetch | `fetch(url, {method, headers, body})` | 包装为 ok/status/text/json/arrayBuffer |
| HTML | `new HtmlDocument(html)` | querySelector(All)、text、attributes、children、dispose |
| 源数据 | `loadData/saveData/deleteData` | 每源独立 KV，配额待定 |
| App | `APP.version/locale/platform` | 只读 |
| 日志 | `log/info/warn/error` | 写入源日志缓冲区 |
| 计时 | 定时器/`sleep`（如源依赖） | 需限制最长等待 |
| 中断 | 宿主 watchdog | 超时/取消/死循环 |

## 4. 测试用源与 fixture

- 自建 `fixtures/server`：本地 HTTP 服务，提供 JSON 搜索/详情/章节与简单 HTML 页面；内容自造。
- 自写 `fixtures/example_source.js`：按 Venera 文档的读路径契约实现，**不复制其示例或 `assets/init.js`**。
- 第二份源：实现相同能力的"脏"版本，用于测试错误处理、无限循环、超大响应、编码异常。
- 兼容性验证：如使用真实 Venera 源做对照，只作为本地测试，不提交到仓库；记录修改点。

## 5. 测试矩阵

| 场景 | 桌面 JVM | Android | iOS |
|---|---|---|---|
| 加载合法源 + 元数据 | | | |
| 搜索/详情/章节/图片 URL | | | |
| Cookie 读写 | | | |
| HTML 解析 | | | |
| 源 KV 持久化 | | | |
| 5s 超时中断 | | | |
| 死循环中断（`while(true)`） | | | |
| 取消正在进行的请求 | | | |
| 10 并发请求 | | | |
| 5MB 响应上限 | | | |
| 加载/卸载 100 次 | | | |
| 非法脚本（语法错误/异常） | | | |

## 6. 指标（记录实际值，不预设结论）

- 运行时冷启动时间（desktop / Android 中端 / iOS 模拟器）。
- 单次调用宿主 API 的额外开销（不含网络）。
- 每实例常驻内存；10 并发时峰值。
- 死循环中断耗时（目标 < 1s）。
- 100 次加载/卸载后的内存增长。
- 二进制体积增量（按平台）。

## 7. 通过标准

- 三端读路径全部跑通；桌面 + Android 必须真实设备/模拟器验证。
- 超时、取消、死循环中断可靠，宿主进程不崩溃。
- 单源常驻内存 < 32MB（暂定），100 次加载/卸载无持续增长。
- 一个自写 Venera 兼容源无需修改即可运行；若需 shim，记录 shim 清单与原因。
- quickjs-kt 1.0.15 的阻塞问题有明确 workaround 或 vendor 方案。

## 8. 实施步骤

1. 建 KMP 模块（android/jvm/ios），接入 quickjs-kt，写 `SourceEngine` 抽象与 expect/actual。
2. 桌面先跑通：加载脚本 → 定义宿主绑定 → 调用搜索。
3. 补 HTML、Cookie、KV；接 fixture server。
4. 接 Android；再补 iOS（cinterop 与链接问题重点记录）。
5. 加 watchdog 与资源限额，跑异常与并发测试。
6. 写 Venera 兼容适配层最小版本，记录 coverage。
7. 写报告与 Go / Conditional / Stop 建议。

## 9. 输出

`docs/spikes/results/s2-js-runtime.md`：

- quickjs-kt 实际版本、平台接入方式、编译/链接问题。
- API 覆盖表、shim 清单。
- 性能/内存数据与测试日志。
- 沙箱缺口与限额建议。
- 许可证检查：无 GPL 代码复制。

## 10. 明确不做

- 不实现完整源 API，不做视频源，不做 UI 对话框。
- 不做源仓库签名/分发；Phase 1 再设计。
- 不评估真实第三方源的合规性；测试用源全部自建。
