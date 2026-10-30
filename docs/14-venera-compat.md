# 14 Venera 源零改验证

> 2026-10-30 · 状态：**验证手段已建立并跑完两轮**；`Convert`、`Network.fetchBytes`、cookie API 与异步 rejection 兜底均已补齐，**13/13 构造成功、13/13 静态兼容**；hitomi 已在桌面 JVM 跑通真站 search → details → pages，Android 与其余源待验。
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

# Convert 宿主实现的自测（RFC 1321 / FIPS 180 / RFC 4231 向量）
./gradlew :shared:source-runtime:jvmTest --tests '*ConvertRuntimeTest*'
```

## 实测结果（2026-10-30，13 个真实源）

**13 / 13 在未作任何修改的情况下构造成功**；静态审计 **13 个真实源全部兼容（0 缺口）**。

| 源 | 构造 | 静态审计 | 说明 |
|---|---|---|---|
| comick.js | ok | OK | 读路径 API 齐全 |
| komiic.js | ok | OK | 同上 |
| manga_dex.js | ok | OK | 同上（MangaDex 公开 API） |
| shonenjumpplus.js | ok | OK | 同上 |
| copy_manga.js | ok | OK | 补 `Convert` 后由 BLOCKED 转 OK |
| picacg.js | ok | OK | 同上（`Convert.hmacString`） |
| baozi / wnacg / ikmmh / nhentai / ehentai | ok | OK | 补 cookie API 后由 RISK 转 OK；登录态读写路径均可运行 |
| jm.js | ok | OK | `init()` 中 fire-and-forget 的图片线路刷新失败会被 quickjs-kt 当成致命错误；宿主包装源实例的异步方法后不再触发，构造成功 |
| hitomi.js | ok | OK | 补 `Network.fetchBytes` 后由 BLOCKED 转 OK（Range 请求 + 二进制 body） |

结论：**兼容性的瓶颈不是语言层，而是宿主 API 的覆盖面**。QuickJS + 预加载脚本足以承载社区源的实际写法。

## 真站冒烟（2026-10-30，桌面 JVM）

| 源 | 结果 | 说明 |
|---|---|---|
| hitomi.js | **PASS** | search 25 条 → details 1 章 → pages 30 页；本机网络可达 |
| comick.js | 环境阻塞 | HTTP 522（站点/边缘） |
| manga_dex.js | 环境阻塞 | TLS `ProtocolVersion` alert（本机网络） |

hitomi 这次真站往返暴露并修复了两个适配缺陷：源不调用 `dispose()` 时会撞上 16 个
`HtmlDocument` 硬上限（现在到上限淘汰最旧文档）；单画廊源只返回 `maxPage`/`thumbnails`、
不返回章节（现在合成一个默认章节，并在取页时映射回 `epId=null`）。
comick / manga_dex 的失败发生在网络/站点层，与适配无关；换网络后可重跑。

## 已补齐的 API

| 补的东西 | 影响 | 备注 |
|---|---|---|
| `Convert`（22 个方法） | 7 个源 | 宿主实现（`MessageDigest` / `javax.crypto`）。摘要返回原始 `ArrayBuffer`（源里显式 `hexEncode`），二进制结果统一包成真 `ArrayBuffer` 以满足 `instanceof` |
| `console.*` | 4 个源 | 转 `__hostLog` 宿主绑定，源日志可见 |
| `Comment` | 4 个源 | 仅评论页展示，数据载体 |
| `Image` + `Image.empty` | 1 个源 | 图像变换仍未实现，句柄足够 |
| `APP.version/locale` | 2 个源 | `APP.platform` 故意不提供：preamble 由 JVM/Android 共用，报错平台名会静默改变源的分支 |
| `UI.showMessage/showDialog` | 1 个源 | 由"抛异常"改为写日志——headless 下抛错会让源在流程中途中断 |
| `randomInt/randomDouble/createUuid/log` | 1 个源 | 上游 `init.js` 的全局小工具 |
| `fetch` | 1 个源 | `Network.sendRequest` 的 Response 形状适配；`arrayBuffer()` 仍显式报错 |
| `Network.fetchBytes` | 1 个源（hitomi） | 二进制响应；宿主返回 `status`/小写 `headers`/`body(ArrayBuffer)`，body 经 token 暂存避免把二进制塞进 JSON |
| `Network.setCookies/getCookies/deleteCookies` | 6 个源 | 复用持久 cookie jar；URL 支持裸域名，`deleteCookies` 按 URL 匹配清除 |
| 未处理 Promise rejection | 1 个源（jm） | 包装源实例的异步方法，给返回的 Promise 挂 no-op catch；`await` 方仍能收到 rejection |

## 剩余缺口

| 缺口 | 被引用次数 | 代价 | 判断 |
|---|---|---|---|
| `setTimeout/setInterval` | 本轮未出现 | 中：需要宿主定时器 | 按需 |
| `Convert.decryptRsa` | 本轮未出现 | 高：RSA 私钥解密 | 按需 |
| `getClipboard/setClipboard/compute` | 本轮未出现 | 低：可 stub | 按需 |

## 工具自身的两个坑（已修，避免复现）

1. **桩要算作缺口**。第一版把"声明了但抛异常"的成员算成支持，`baozi.js` 因此被判假通过。判定必须是三档：已实现 / 抛异常的桩 / 缺失。
2. **成员归属要按边界切**。定位"桩"时向前找下一个 `{` 会把相邻成员的函数体算进来——`Convert.decryptRsa` 的 `throw` 一度让 `encodeUtf8`、`sha256` 都被标成桩。现在先收集 depth-0 的成员起始行，再按行切片。

## 已知边界

- 构造与静态 API 面已覆盖；真站往返目前只验了 hitomi（桌面 JVM，2026-10-30）。其余源与 Android 端仍待验；真站可用性受站点稳定性、反爬与地区影响，不属于"兼容性"。逐源冒烟仍是手动 canary 的事。
- 参考源集合在 `reference/` 本地保存，**不提交**（第三方脚本，许可证与内容均不属于本项目）。文档只记录测量结果与文件名。
- 清单会随上游 `init.js` 变化失效；`tools/venera_compat.py` 每次从当前 preamble 重新推导，因此换 preamble 后重跑即可得到新结论。
