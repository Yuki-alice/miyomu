# ADR-0008：Venera 风格 JS 源运行时的读路径实现

- 状态：接受（M1 read-path 切片；真实第三方源已验证构造，见 [14 号文档](../14-venera-compat.md)）
- 日期：2026-10-28
- 关联：[ADR-0003](0003-js-extension-runtime.md) · [ADR-0007](0007-network-and-cookie-storage.md) · [M1 清单](../checklists/phase-1-m1.md)

## 背景

Phase 0 的 S2 只在自写 fixture 上验证了 QuickJS + Ksoup + 内存 Cookie。M1 需要把源运行时产品化：每个源独立生命周期、正式网络层、持久 Cookie、源级 KV、DOM、配额，并让 Android 与桌面共享同一套读路径。

Venera-Next 的源脚本约定（仅参考行为，不复制 GPL 代码）：

- 脚本声明一个 `class X extends ComicSource`；
- 宿主构造实例，读取 `key/name/version` 并注册到 `ComicSource.sources`；
- `search.load(keyword, options, page)` 返回 `{comics, maxPage}`；
- `comic.loadInfo(id)` 返回 `ComicDetails`（chapters 可为扁平或分组对象）；
- `comic.loadEp(comicId, epId)` 返回 `{images: string[]}`；
- 网络通过 `Network.*`，HTML 通过 `HtmlDocument`，源数据通过 `this.loadData/saveData`。

## 决策

实现 `:shared:source-runtime`，每个源一个独立运行时：

- 每个源创建独立 `QuickJs`、`HttpClient`、`FileCookiesStorage`、`FileSourceKeyValueStore`、DOM 桥和调用 `Mutex`。
- 脚本加载：正则找到第一个 `class … extends ComicSource`，在脚本同一 eval 作用域执行 `new ClassName()`，交给 `__miyomuCreateSource` 校验/注册/调用可选 `init()`；不依赖 class 出现在 `globalThis`。
- Kotlin 适配层 `ManagedComicSource` 暴露 `search/details/pages/loadImage/close`；JS 只面对受控 host API。
- 已实现 host API：
  - `Network.get/post/put/patch/delete/sendRequest` → Ktor，返回 `{status, headers, body}`；HTTP 4xx/5xx 作为响应返回，不抛异常。
  - `HtmlDocument`/`HtmlElement` → Ksoup；文档数上限 16、元素句柄上限 20,000。
  - `this.loadData/saveData/deleteData/loadSetting` → 每源 JSON KV，操作结束后原子落盘。
  - `Network` 自动 Cookie 持久化（ADR-0007）；JS 同步 Cookie API 暂未实现。
  - 外链图片由 `ManagedComicSource.loadImage` 受控加载，带源 Cookie。
- 配额：单次操作 30s、QuickJS 内存 64 MiB、HTTP 响应 8 MiB、图片 20 MiB、DOM 句柄上限。
- 失败语义：`SourceException` / `SourceTimeoutException`；取消异常原样传播。

## 验证

- `:shared:source-runtime:jvmTest` 5/5：
  - HTML 搜索→详情→图片 URL→图片字节；
  - 扁平与分组章节、tags 解析；
  - 搜索响应 Set-Cookie 后，详情/图片请求自动携带；
  - 源 KV 跨 source 重载持久化；
  - key 不匹配、无限 `init()` 超时可控。
- Android KMP library `assemble` 通过（QuickJS + Ksoup Android 变体）。
- 桌面与 Android 壳加载同一个 `demo_source.js`，对本地 fixture 完成
  `3 results → 3 chapters → 3 pages + first image`；截图见 `evidence/`。

## 后果

正面：

- Android 与桌面共享同一源实现，不依赖平台 WebView 或 APK 扩展。
- 每源隔离的 Cookie/KV/超时/内存边界已落地，可继续加源仓库和配额策略。

负面 / 未完成：

- 真实 Venera 兼容源的验证手段已建立（静态 API 审计 + 构造探针，见 [14](../14-venera-compat.md)）：13 个社区源中 12 个零改构造成功。`Convert`（编码/加密）仍是主要缺口，命中它的源在调用时会失败。
- `Network.setCookies/getCookies/deleteCookies` 的同步 JS 语义未实现；登录态目前仅靠 HTTP 自动 Cookie。
- 未实现 `fetch`、search options、explore/categories、settings、login、comments、UI；这些按“显式报错/stub”处理。
- QuickJS 每源常驻有内存开销；尚未测多源并发与真实站点长尾 DOM。
- 桌面/Android 当前是调试壳，不是最终阅读器；分页/连续阅读、缩放、预取、库/进度在后续 M1/M2。
