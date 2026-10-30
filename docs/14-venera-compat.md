# 14 Venera 源零改验证

> 2026-10-30 · 状态：**已建立可重复的验证手段，首轮结果见下**；`Convert` 仍是主要缺口。
> 关联：[ADR-0008](adr/0008-venera-source-runtime.md)（源运行时）、[ADR-0003](adr/0003-js-extension-runtime.md)、[03 风险登记](03-risk-register.md) R4

## 为什么需要这份验证

v1 成功标准里有一条是硬指标：**「一个真实 Venera 兼容漫画源可在 Android + 桌面运行，无需修改源脚本」**。此前项目只有自写 fixture（`demo-resources/sources/demo_source.js`）跑通，自写源证明不了兼容性——它本来就是按我们的实现写的。ADR-0008 因此把这条标为"待用户提供合法测试源"。

本轮把它拆成两层可重复的检查，不再依赖个案。

## 两层验证

| 层 | 手段 | 回答的问题 | 是否可 CI |
|---|---|---|---|
| 静态 | `tools/venera_compat.py` | 这个源引用到的宿主 API，我们实现了吗？ | 是（纯文本比对，不执行、不联网） |
| 动态 | `VeneraSourceCompatTest` | 这个源能不能在 QuickJS 里真的解析、实例化、注册？ | 本机有 `reference/` 时运行 |

静态层从 `SourcePreamble.kt` 里解析出**实际注入的全局对象及其成员**，并区分"已实现"与"抛异常的桩"——这一步很重要：`Network.setCookies` 这类桩如果被算作"支持"，审计会给出假阳性。

动态层只做构造（不发请求），但构造本身会覆盖类字段、箭头函数、可选链、模板字符串、`ComicSource` 子类注册等语言与机制差异。

### 复现命令

```bash
# 取一份本地源集合（仅本地验证，不进仓库；reference/ 已 gitignore）
git clone --depth 1 https://github.com/yzlvhell/venera-yuan reference/venera-yuan

python3 tools/venera_compat.py reference/venera-yuan/*.js --summary
./gradlew :shared:source-runtime:jvmTest --tests '*VeneraSourceCompatTest*'
# 报告写到 build/reports/venera-compat.md
```

## 首轮结果（2026-10-30，13 个真实源）

**12 / 13 在未作任何修改的情况下构造成功**，只有 `jm.js` 失败。

| 源 | 构造 | 静态审计 | 说明 |
|---|---|---|---|
| comick.js | ok | OK | 读路径应当可以直接跑 |
| komiic.js | ok | OK | 同上 |
| manga_dex.js | ok | OK | 同上（MangaDex 公开 API） |
| shonenjumpplus.js | ok | OK | 同上 |
| baozi.js | ok | RISK | 只用 `Network.setCookie*`（登录用），读路径不受影响 |
| wnacg / ikmmh / nhentai / ehentai | ok | RISK | 同上 |
| copy_manga / picacg | ok | BLOCKED | 构造没问题，首次用到 `Convert.encodeUtf8` 才失败 |
| hitomi | ok | BLOCKED | `Convert.sha256` + `Network.fetchBytes` |
| jm | **FAIL** | BLOCKED | `initialize()` 里就调 `Convert`，直接 ReferenceError |

结论：**兼容性的瓶颈不是语言层，而是宿主 API 的覆盖面**。QuickJS + 我们的预加载脚本足以承载社区源的实际写法。

## 本轮补齐的 API

| 补的东西 | 影响 | 备注 |
|---|---|---|
| `console.*` | 4 个源 | 转 `__hostLog` 宿主绑定，源日志可见 |
| `Comment` | 4 个源 | 仅评论页展示，数据载体 |
| `Image` + `Image.empty` | 1 个源 | 图像变换仍未实现，句柄足够 |
| `APP.version/locale` | 2 个源 | `APP.platform` 故意不提供：preamble 由 JVM/Android 共用，报错平台名会静默改变源的分支 |
| `UI.showMessage/showDialog` | 1 个源 | 由"抛异常"改为写日志——headless 下抛错会让源在流程中途中断 |
| `randomInt/randomDouble/createUuid/log` | 1 个源 | 上游 `init.js` 的全局小工具 |
| `fetch` | 1 个源 | 基于 `Network.sendRequest` 的 Response 形状适配；`arrayBuffer()` 仍显式报错 |

补齐后静态全兼容的源从 3 个增加到 5 个，构造成功从 10/13 提升到 12/13。

## 剩余缺口与代价

| 缺口 | 被引用次数 | 代价 | 判断 |
|---|---|---|---|
| `Convert`（utf8/base64/hex/md5/sha256/hmac/aesEcb） | 4 个源，其中 3 个是唯一阻塞项 | 中高：二进制跨 JS 边界 + 双向加密实现。JVM 与 Android 同为 JVM 目标，可共用一份 `jvmShared` 桥，无需 expect/actual | **下一批优先做**，收益最集中 |
| `Network.setCookies/getCookies/deleteCookies` | 5 个源 | 中：只影响登录态，读路径不阻塞 | 第二批 |
| `Network.fetchBytes` | 1 个源 | 低中：二进制响应 + `arrayBuffer()` | 跟随 Convert |
| `setTimeout/setInterval` | 本轮未出现 | 中：需要宿主定时器 | 按需 |
| `getClipboard/setClipboard/compute` | 本轮未出现 | 低：可 stub | 按需 |

## 已知边界

- 本验证**只覆盖构造与静态 API 面**，不包含真实站点往返。真站可用性受站点稳定性、反爬与地区影响，不属于"兼容性"。逐源真站冒烟仍是手动 `-Pcanary` 的事。
- 参考源集合在 `reference/` 本地保存，**不提交**（第三方脚本，许可证与内容均不属于本项目）。文档只记录测量结果与文件名。
- 清单会随上游 `init.js` 变化失效；`tools/venera_compat.py` 每次从当前 preamble 重新推导，因此换 preamble 后重跑即可得到新结论。
