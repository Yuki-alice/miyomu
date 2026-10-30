# 架构概览

> 这是代码结构说明，不是可用性声明。当前真实状态见 [00-current-status.md](00-current-status.md)。

## 产品范围

- 三种媒介：漫画 / 视频 / 小说，共用库、源与进度模型。
- 平台：Android + 桌面（Compose Desktop）为首发目标；iOS 未开始，也没有 `iosMain` 目标。
- 技术栈：Kotlin Multiplatform 共享核心，Compose Multiplatform 构建 UI。
- 扩展模型：JavaScript 源，QuickJS 运行时；不内置、不托管任何内容源。

## 模块

| 模块 | 作用 |
|---|---|
| `:shared:model` | 领域模型、源描述、章节/页面 |
| `:shared:network` | Ktor 3.6、OkHttp/CIO、持久 Cookie |
| `:shared:source-api` | `ComicSource` / `AnimeSource` 及其托管接口 |
| `:shared:source-runtime` | 每源 QuickJS、Ktor、Ksoup DOM、KV、配额 |
| `:shared:source-repo` | 源仓库索引、安装/更新/卸载与本地清单 |
| `:shared:source-manager` | 已安装源的运行时加载与搜索/详情/页面/图片/视频解析 |
| `:shared:source-novel` | legado 书源 JSON 导入、规则求值器、在线内容源 |
| `:shared:download` | 漫画章节下载队列、离线页源 |
| `:shared:danmaku` | dandanplay 弹幕客户端（用户自配凭据） |
| `:shared:data` | Room 2.8.5 + KSP（schema v4）；库/进度/章节/剧集/分类/更新/下载/解析缓存 |
| `:shared:local` | 本地文件抽象、书库扫描、目录/ZIP 页加载 |
| `:shared:novel` | 小说解析：TXT/EPUB/FB2/HTML/Markdown、小说库扫描 |
| `:shared:reader-comic` | 漫画阅读状态、模式、页配对 |
| `:shared:reader-novel` | 小说阅读偏好、分页数学、阅读控制器 |
| `:shared:player` | mediamp 播放器、播放器 chrome、音轨/字幕轨选择 |
| `:shared:domain` | Aniyomi 域模型、偏好 API、排序/集号识别 |
| `:shared:i18n` / `:shared:i18n-aniyomi` | moko-resources 字符串资源（en + zh-CN） |
| `:shared:presentation-core` / `:shared:presentation` | 移植的 Aniyomi 组件与屏幕层 |
| `:shared:ui` | 主题、App 壳、书架/详情/阅读器/播放器/源仓库/设置 |
| `:androidApp` / `:desktopApp` | Android / 桌面入口 |

## 数据与状态

- Room schema 版本为 4；改实体需同步 schema 与迁移。
- 小说书签、阅读偏好与 legado 书源保存在 JSON `PreferenceStore`，不占 Room schema。
- 漫画/视频的库、进度、章节、剧集、分类、下载、解析缓存走 Room。

## 源运行时

- 每源一个 QuickJS 实例，带超时、内存上限、DOM/响应配额。
- Venera 风格读路径：`Network`、`Convert`、`HtmlDocument`、cookie、图片配置等由宿主提供。
- legado 小说源是独立的规则求值器，不复用 QuickJS。
- anime 源使用自有 `AnimeSource` 格式（`search` / `detail` / `resolve`）。

## 播放器

- 统一抽象为 mediamp；Android 使用 ExoPlayer，桌面 macOS/Windows 使用 MPV 运行时。
- Linux 桌面不随应用分发 MPV（GPL 许可边界），打开视频会显示说明页。

## 构建布局

- 生产构建在仓库根目录：`androidApp/`、`desktopApp/`、`shared/`。
- `experiments/` 是独立的 Phase 0/1 验证构建，使用 `./gradlew -p experiments ...`。

当前哪些路径真的被验证、哪些没有，见 [03-verification.md](03-verification.md) 与 [00-current-status.md](00-current-status.md)。
