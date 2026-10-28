# Phase 1 M1 清单：核心与单媒介

- 状态：**进行中**（2026-10-28）；源运行时读路径与双端调试壳已通，核心数据层/阅读器/真实源待完成
- 关联：[ADR-0008](../adr/0008-venera-source-runtime.md) · [Phase 1 计划](../05-phase-1-plan.md)

## 1. 生产工程

- [x] 仓库根目录生产构建：KGP 2.3.21 + AGP 9.4.1 + Gradle 9.6 + compileSdk 37
- [x] `:shared:model`：MediaType、SourceDescriptor、ComicSummary/Details、Chapter、PageRef、Paged
- [x] `:shared:network`：Ktor 3.6 + OkHttp/CIO + 持久 Cookie（迁移自 M0，8/8 测试）
- [x] `:shared:source-api`：ComicSource / ManagedComicSource / SourceException
- [x] `:shared:source-runtime`：QuickJS + Ktor + Ksoup + 源 KV + 配额
- [x] `:desktopApp`：Compose Desktop 调试壳
- [x] `:androidApp`：Compose Android 调试壳（AGP 9.4 内置 Kotlin + Compose 1.12）
- [x] `:shared:data`：Room 2.8.5 + KSP 2.3.12 + bundled SQLite；库/进度 schema 与 repository
- [x] 双端壳接入库/进度：打开作品写库、打开章节保存进度
- [ ] `:shared:library` / `:shared:download` / `:shared:sync`
- [ ] `:shared:reader-comic` 正式阅读器

验证命令：

```bash
./gradlew :shared:source-runtime:jvmTest
./gradlew :shared:source-runtime:assemble
./gradlew :shared:data:jvmTest
./gradlew :desktopApp:compileKotlin
./gradlew :androidApp:assembleDebug
```

## 2. 源运行时（Venera 读路径）

- [x] 每源独立 QuickJS 实例、HttpClient、Cookie 文件、KV 文件、DOM 桥、调用锁
- [x] 脚本构造：`new ClassName()` → 校验 key/name/version → `ComicSource.sources` 注册
- [x] `search` → `details`（扁平/分组章节）→ `pages` → `loadImage`
- [x] `Network.get/post/put/patch/delete/sendRequest`，返回 `{status, headers, body}`
- [x] `HtmlDocument` / `HtmlElement` 查询、属性、父子/兄弟、dispose
- [x] 源级 KV 持久化（JSON、原子写、损坏恢复）
- [x] 自动 Cookie 持久化与请求携带
- [x] 配额：30s 超时、64 MiB QuickJS 内存、8 MiB 响应、20 MiB 图片、16 文档 / 20k 元素
- [x] JVM 测试 5/5（搜索/详情/图片、扁平/分组章节、Cookie、KV、超时/key 校验）
- [ ] 真实 HTML Venera 兼容源零修改验证（需合法本地测试源，不提交）
- [ ] `Network.setCookies/getCookies/deleteCookies` 同步 JS 语义
- [ ] `fetch`、search options、explore、categories、settings、login
- [ ] 源仓库索引/校验/安装/更新 UI；每源日志与配额配置

## 3. 双端读路径（M1 验收）

- [x] 桌面 Compose Desktop 壳：搜索 → 结果 → 详情 → 章节 → 页面 URL + 首图
- [x] Android Compose 壳：同一 source 完成同一流程
- [x] Room 接入：打开作品后 `library 1`，打开章节后 `progress saved`
- [x] 证据：`evidence/m1-desktop-flow.png`、`m1-android-flow.png`、`m1-desktop-room.png`、`m1-android-room.png`
- [x] 本地 fixture server：`tools/fixture_server.py`（自造 HTML/图片，无第三方内容）
- [ ] 真实源（非 fixture）在双端跑通
- [ ] 正式漫画阅读器：分页/连续、RTL/LTR、双页、缩放、预取、进度
- [ ] 库/历史/分类/下载接入

## 4. 播放器轨道选择

- [ ] mediamp 0.5.1 音轨/内嵌字幕选择 spike
- [ ] 定义 `TrackSelector` / `PlayerCapabilities`
- [ ] ExoPlayer 与 MPV 后端直连验证

## 5. 当前结论

源运行时的“读路径垂直切片”已可运行：Android 与桌面共享同一 QuickJS 源、同一 Ktor/Cookie/KV 实现，fixture 源完成搜索→详情→章节→图片；Room 数据层已接入，作品与进度会持久化。下一优先级是：

1. 真实 Venera 源对照（阻塞生态假设）；
2. 正式漫画阅读器视图（Aniyomi loader 管线复用）+ 库/历史 UI；
3. 播放器轨道选择；
4. 下载/同步与源仓库产品化。

调试壳不是最终 UI；不把 fixture 源当作真实源或内容推荐。
