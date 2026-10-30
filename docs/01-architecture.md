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
| `:shared:contract` | UI 依赖的端口：仓储接口 + 它们交换的记录（无 Room / 无网络 / 无平台代码） |
| `:shared:data` | Room 2.8.5 + KSP（schema v4）；实现 `:shared:contract` 的仓储 |
| `:shared:local` | 本地文件抽象、书库扫描、目录/ZIP 页加载 |
| `:shared:novel` | 小说解析：TXT/EPUB/FB2/HTML/Markdown、小说库扫描 |
| `:shared:reader-comic` | 漫画阅读状态、模式、页配对 |
| `:shared:reader-novel` | 小说阅读偏好、分页数学、阅读控制器 |
| `:shared:player` | mediamp 播放器、播放器 chrome、音轨/字幕轨选择 |
| `:shared:domain` | Aniyomi 域模型、偏好 API、排序/集号识别 |
| `:shared:i18n` / `:shared:i18n-aniyomi` | moko-resources 字符串资源（en + zh-CN） |
| `:shared:presentation-core` / `:shared:presentation` | 移植的 Aniyomi 组件与屏幕层 |
| `:shared:ui-core` | `MiyomuGraph`、主题、共享组件、Navigation 3 键与导航器、库更新与视频解析助手 |
| `:shared:ui-library` | 书架、分类、历史、更新（`library/` `history/` `updates/`） |
| `:shared:ui-entries` | 漫画/视频详情页 |
| `:shared:ui-novel` | 小说书架、详情、在线阅读与发现 |
| `:shared:ui-reader` | 漫画阅读器（含 `ComicReaderScreen`） |
| `:shared:ui-sources` | 已安装源的搜索/详情、全局搜索、源仓库 |
| `:shared:ui-player` | 本地/在线播放器路由与播放器 chrome |
| `:shared:ui-settings` | 设置、备份同步、下载队列 |
| `:shared:ui` | App 壳：`MiyomuApp`、`MiyomuNavHost`、`HomeScreen` 与六个 Tab |
| `:shared:application` | 组合根：构造数据/偏好/源运行时/下载/小说抓取，接收平台 `MiyomuAppEnvironment` |
| `:app:android` / `:app:desktop` | Android / 桌面入口，只提供平台环境与回调 |

## UI 模块分层

```
:app:android / :app:desktop    平台入口：数据库工厂、文件根目录、SAF / 打开文件夹、退出
  └── :shared:application      组合根：MiyomuAppEnvironment → 数据/偏好/源/下载/抓取 → MiyomuApp
        └── :shared:ui         shell：MiyomuApp + MiyomuNavHost + HomeScreen（唯一能看见全部功能模块）
              ├── :shared:ui-settings      ← core
              ├── :shared:ui-player        ← core
              ├── :shared:ui-sources       ← core, library
              ├── :shared:ui-reader        ← core, library, entries
              ├── :shared:ui-novel         ← core, library
              ├── :shared:ui-entries       ← core, library
              └── :shared:ui-library       ← core
                    :shared:ui-core        graph + theme + NavKey + 共享组件（暴露整个 shared 栈）
```

规则：

- 功能模块之间**只允许单向**：`library → entries → reader`，`library → novel`，`library → sources`。反向依赖会成环。
- 只有 shell 可以同时依赖全部功能模块；`MiyomuNavHost` 是唯一的键→界面映射。
- `:shared:application` 是唯一的组合根：它构造 `MiyomuData`、`JsonPreferenceStore`、源仓库/运行时、
  下载队列和小说的 HTTP 客户端。平台入口不再重复这套装配，只提供 `MiyomuAppEnvironment`
  （`dataDir`、`libraryRoot`、`MiyomuDatabaseFactory`、`deviceId`、`nowMillis`、平台回调）。
- `ui-core` 的 `api` 只留 6 个：`model`、`contract`、`presentation`、`presentation-core`、`i18n`、`i18n-aniyomi`。
  其余 11 个实现模块都是 `implementation`，功能模块要用哪个就自己声明哪个。
- 持久化只能通过 `:shared:contract` 的端口访问：`MiyomuGraph.data` 的类型是 `DataPort`，不是 `MiyomuData`。
  Room、DAO、实体类型不会出现在任何功能模块的 classpath 上。
- 测试跟着代码走：单元测试放在所属模块的 `jvmTest`，跨模块的端到端流程测试留在 shell。
- Android 的 SAF 目录 URI 存在平台专用的 `SharedPreferences`（`miyomu-platform`），不再和 JSON
  `PreferenceStore` 写同一个文件。

## 导航

- 使用 **Navigation 3**（`androidx.navigation3:navigation3-runtime` + Compose Multiplatform 版
  `org.jetbrains.androidx.navigation3:navigation3-ui`）。没有 NavController：回退栈是一个可观察列表，
  导航就是列表操作。
- 每个目的地是 `app.miyomu.ui.navigation` 下的一个 `@Serializable` `MiyomuKey`，只携带 id / 标题 /
  索引等可序列化数据，不携带 `MiyomuGraph`、`LocalBook` 实例或 lambda。
- 键到界面的唯一映射在 `MiyomuNavHost` 的 `entryProvider` 里；新增页面必须先加 Key 再加 entry。
- 界面通过 `LocalMiyomuNavigator` 拿到 `push` / `pop` / `replace` / `popUpTo`。
- 底部六个 Tab 是 `HomeScreen` 的内部状态，不进回退栈；Tab 打开的详情/阅读/播放才 push 到根栈。
- `:shared:presentation` 仍保留 Voyager 的 `ScreenModel` 作为状态持有者，**不再**用于路由。

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

## 构建约定（build-logic）

- `build-logic/` 是 included build，提供四个约定插件：
  - `miyomu.kmp-library`：KMP + Android KMP library、`jvmToolchain(21)`、`compileSdk 37` / `minSdk 26` /
    `jvmTarget 17`、JUnit 5、`src/jvmShared/kotlin` 共享源集、`-Xexpect-actual-classes`。
  - `miyomu.kmp-compose`：在 `kmp-library` 之上应用 Compose Multiplatform 与编译器插件，统一 Compose 依赖。
  - `miyomu.android-application`：Android application、compileSdk/targetSdk/Java 17、Compose。
  - `miyomu.jvm-compose`：桌面 JVM + Compose Desktop。
- 模块脚本只保留插件、`namespace`、模块特有配置和依赖；不再重复 `jvmToolchain`、`compileSdk`、
  Android 测试配置。
- 依赖引用使用 typesafe project accessors（`projects.shared.uiCore`）；字符串
  `project(":shared:...")` 不再出现。
- 源集保持 KMP 标准布局 `src/<sourceSet>/kotlin`，没有采用 animeko 的扁平源集；JVM 与 Android
  共用的 JVM 实现放在 `src/jvmShared/kotlin`，由约定插件同时挂到两个目标。

## 构建布局

- 生产构建在仓库根目录：`app/`（`android/`、`desktop/`）、`shared/`、`build-logic/`。
- `experiments/` 是独立的 Phase 0/1 验证构建，使用 `./gradlew -p experiments ...`。

当前哪些路径真的被验证、哪些没有，见 [03-verification.md](03-verification.md) 与 [00-current-status.md](00-current-status.md)。
