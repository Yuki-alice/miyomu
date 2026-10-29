# Miyomu

**跨平台三合一内容阅读器** · 名称来自 見る（看）+ 読む（读）

漫画、小说、视频，一套库、一套源、一套进度。以 Aniyomi 的 Android 体验为设计基准，用 Kotlin Multiplatform 共享核心、Compose Multiplatform 构建跨平台 UI，用 JavaScript 扩展源替代 Android-only 的 APK 扩展模型。

> 状态：**Phase A2 + Phase B 完成**：书架、漫画/视频详情、阅读器、播放器全部跑在移植的 Aniyomi 界面上；Android 与桌面（macOS/Windows）都能播放本地视频，按目录多集/季组织、切音轨/字幕、逐集续播。桌面运行时方案见 [ADR-0012](docs/adr/0012-desktop-video-backend-licensing.md)（方案 A）。路线见 [ADR-0011](docs/adr/0011-kmp-aniyomi-port.md)。
> 许可证：Apache-2.0（2026-10-28 确认，见 [ADR-0004](docs/adr/0004-licensing-and-clean-room.md)）
> 生产构建位于仓库根目录；Phase 0/1 验证代码在 `experiments/`，使用独立 Gradle 构建。

## 模块

| 模块 | 作用 |
|---|---|
| `:shared:model` | 领域模型、源描述、章节/页面 |
| `:shared:network` | Ktor 3.6、OkHttp/CIO、持久 Cookie |
| `:shared:source-api` | `ComicSource` / `ManagedComicSource` |
| `:shared:source-runtime` | 每源 QuickJS、Ktor、Ksoup DOM、KV、配额 |
| `:shared:source-repo` | 源仓库索引、安装/更新/卸载与本地清单 |
| `:shared:source-manager` | 已安装源的运行时加载与搜索/详情/页面/图片 |
| `:shared:data` | Room 2.8.5 + KSP；库/进度 schema 与 repository |
| `:shared:local` | 本地文件抽象、书库扫描、目录/ZIP 页加载 |
| `:shared:reader-comic` | 漫画阅读状态、模式、页配对 |
| `:shared:player` | mediamp 播放器、播放器 chrome、音轨/字幕轨选择 |
| `:shared:ui` | 主题、App 壳、书架/详情/阅读器/播放器 |
| `:androidApp` | Android 应用入口 |
| `:desktopApp` | 桌面应用入口 |

工具链：AGP 9.4.1 + Kotlin 2.3.21 + Gradle 9.6 + compileSdk 37 + JDK 21。

## Phase 0 回顾

Phase 0 不做产品功能，只回答三个问题（已完成）：

| Spike | 要回答的问题 | 规格 |
|---|---|---|
| S1 播放器 | mediamp 能否在 Android / 桌面 / iOS 三端撑起播放器？ | [docs/spikes/s1-player.md](docs/spikes/s1-player.md) |
| S2 源运行时 | QuickJS 能否在 KMP 里安全、稳定地跑 Venera 兼容源？ | [docs/spikes/s2-js-runtime.md](docs/spikes/s2-js-runtime.md) |
| S3 UI | Aniyomi 的 Compose 屏幕迁移到 CMP + MD3E 的成本是否可接受？ | [docs/spikes/s3-cmp-ui.md](docs/spikes/s3-cmp-ui.md) |

决策门与详细规格见 [Phase 0 计划](docs/02-phase-0.md)。

Phase 0 已完成：决策 **Go（缩范围）** —— Android + 桌面首发，iOS 后置。S1 桌面/Android 播放通过（iOS 待 App 壳），S2 三端源运行时通过，S3 三屏 headless 验证通过。

Phase 1 M0 已完成：单轨工具链（AGP 9.4.1 + Kotlin 2.3.21 + compileSdk 37）跑通 QuickJS、mediamp、Compose、Ktor；Ktor + 持久 Cookie 网络层通过 JVM 测试与 Android 运行时验证。

Phase 1 M1 进行中：M1 重排为**表现层 + 本地漫画/小说阅读器**。共享 UI、本地库导入、漫画阅读器（目录/ZIP/CBZ/7Z/PDF）和小说阅读器（TXT/EPUB）是主线；源运行时读路径与 Room 数据层已完成并冻结，真实 Venera 源与源仓库移到 M2。详见 [M1 任务重排](docs/08-m1-task-plan.md)、[M1 清单](docs/checklists/phase-1-m1.md) 与 [ADR-0010](docs/adr/0010-aniyomi-local-reader-reuse.md)。

## 仓库结构

```text
androidApp/     Android 应用入口（Compose）
desktopApp/     桌面应用入口（Compose Desktop）
shared/         共享 KMP 模块
  model/ network/ source-api/ source-runtime/ data/
  local/ reader-comic/ ui/
demo-resources/ 本地自造 HTML 源（源运行时 M2）
tools/          fixture server 与本地样例库生成器
evidence/       M1 双端读路径截图
docs/           产品、架构、Phase 1 计划、ADR、风险
experiments/    Phase 0/1 验证代码（独立 Gradle 构建）
  phase1/       M0 工具链与网络层探针
  spikes/       Phase 0 S1/S2/S3
reference/      本地参考仓库（gitignored）
```

## 快速开始

前提：JDK 21。首次运行需要联网下载依赖。

```bash
python3 tools/make_sample_library.py
./gradlew :desktopApp:run
```

编译产品模块：

```bash
./gradlew :desktopApp:build
./gradlew :androidApp:assembleDebug
./gradlew :shared:local:jvmTest :shared:reader-comic:jvmTest :shared:ui:jvmTest :shared:player:jvmTest
./gradlew :shared:source-runtime:jvmTest :shared:data:jvmTest
```

验证代码是独立构建，从仓库根目录执行：

```bash
./gradlew -p experiments :spikes:s3-ui:run
```

## 界面

界面不是"照着画"，而是**逐文件移植** Aniyomi（Apache-2.0）的 `presentation` 层：保留上游包名与文件结构，只替换 Android 绑定点。见 [NOTICE](NOTICE) 与 [ADR-0011](docs/adr/0011-kmp-aniyomi-port.md)。

- 书架：移植的 `LibraryToolbar` + `MangaLibraryContent`，紧凑/舒适网格、列表、显示模式与列数设置、下拉刷新、多选。
- 详情：移植的 `MangaScreen`（宽屏双栏、窄屏单栏），信息头、章节列表、筛选、继续阅读 FAB。
- 阅读器：移植的 `ReaderAppBars`（顶栏、章节导航、页码滑条、模式/方向/裁边/设置）、阅读设置三页、阅读模式与方向选择弹窗。
- 视频详情：移植的 `AnimeScreen`（信息头、剧集/季列表、继续观看 FAB、多选），本地视频映射为单集 anime。
- 播放器：mediamp Surface + 播放器 chrome（顶栏/底栏、自动隐藏、双击 ±10 秒、倍速与音轨/字幕选择）。
- 窄屏用底部导航，宽屏（≥720dp）用侧边 NavigationRail。

截图：[书架](evidence/a2-library-port.png) · [漫画详情](evidence/a2-detail-port.png) · [阅读器](evidence/a2-reader-port.png) · [视频详情](evidence/b2-video-detail.png) · [播放器](evidence/b1-player-controls.png) · [播放设置](evidence/b1-player-settings.png)

## 本地阅读

生成自造样例库（纯色 PNG，无第三方内容）：

```bash
python3 tools/make_sample_library.py
```

桌面默认库目录是 `~/.miyomu/library`；点“刷新”后打开目录或 CBZ：

```bash
./gradlew :desktopApp:run
```

Android 使用应用私有目录，debug 包可通过 `run-as` 放入样例书：

```bash
./gradlew :androidApp:assembleDebug
adb install -r androidApp/build/outputs/apk/debug/*.apk
adb shell run-as app.miyomu.android mkdir -p files/library
adb push "Sample Zip Book.cbz" /data/local/tmp/
adb shell run-as app.miyomu.android cp "/data/local/tmp/Sample Zip Book.cbz" files/library/
```

当前切片支持目录与 ZIP/CBZ；7Z/PDF/EPUB/TXT 和小说阅读器在后续任务接入。

Room schema 导出在 `shared/data/schemas/`；修改实体后需同时更新 schema/迁移。

## 视频播放

库里的视频（mp4/m4v/mkv/webm/avi/mov/ts/m2ts）有三种布局：

- 单个视频文件 = 单集视频书；
- 目录里直接放视频 = 多集视频书（按文件名解析集号，如 `S01E02`、`第 3 集`）；
- 目录下每个子目录放一季的视频 = 系列（详情页列出季，点季进入该季的剧集）。

点开进入 anime 风格的详情页，点“开始/继续观看”进入播放器，支持播放/暂停、进度条、双击左右 ±10 秒、倍速、音轨与内嵌字幕轨选择；进度按集保存，退出后再打开从上次位置继续。封面会优先使用目录里的 `cover`/`folder`/`poster` 图片（或与视频同名的图片）。详情页的筛选按钮可设置过滤、排序、显示方式与季徽标，设置按书保存。

Android 使用 ExoPlayer（`mediamp-exoplayer`）。桌面端 macOS（arm64/x64）与 Windows（x64/arm64）内置 MPV 运行时（Apache-2.0 + LGPL-2.1+，见 [ADR-0012](docs/adr/0012-desktop-video-backend-licensing.md)）；Linux 的 MPV 构建是 GPL-3.0，不随应用分发，打开视频会显示说明页而不是崩溃。

## 源仓库（Phase C）

「更多 → 源仓库」可以添加索引 JSON 的 URL，浏览仓库里的 JS 源并安装/更新/卸载。安装会把脚本
落到应用的 `sources/<id>/source.js`，元数据与仓库地址保存在 `sources/manifest.json`；格式与
安全边界见 [ADR-0013](docs/adr/0013-js-source-repository-format.md)。Miyomu 不内置、不推荐任何仓库。
安装后的源会由 `shared:source-manager` 加载进 QuickJS 运行时，“浏览”Tab 列出可用源：
点源进入搜索，点结果进入详情（移植的 `MangaScreen`），点章节进入阅读器（复用移植的阅读
chrome，页面与图片经源运行时加载）。

## 源运行时（M2）

源运行时读路径已实现，但当前 UI 不加载 JS 源；M2 继续真实源与源仓库。

`demo-resources/sources/demo_source.js` 是自写的 Venera 风格源，使用：

- `class ... extends ComicSource`（宿主构造并注册，不需要手动注册）
- `search.load` / `comic.loadInfo` / `comic.loadEp`
- `Network.get`、`HtmlDocument`、`this.loadData/saveData`

真实第三方源只用于本地验证，不提交；兼容范围见 [ADR-0008](docs/adr/0008-venera-source-runtime.md)。

## 已知限制

- 本地阅读器当前支持目录与 ZIP/CBZ；7Z/PDF/EPUB/TXT 尚未接入。
- 阅读器支持 LTR/RTL/纵向/Webtoon、双页、缩放平移、进度续读；裁边开关会记偏好但尚未按它裁图。
- **章节的已读/书签标记没有持久化**：本地库没有章节表，详情页的"标记已读""书签""删除章节"和阅读器的书签按钮只改界面状态。
- 分类系统未移植：书架只有一个合成分类，分类 Tab 恒隐藏；多选后的批量操作没有数据层支撑。
- 阅读器主题未移植（`readerTheme`），chrome 跟随 App 浅色主题而背景为纯黑。
- 本地小说（TXT/EPUB）尚未开始。
- Android 本地库暂用应用私有目录，SAF 文件选择器待补。
- 真实 Venera 源尚未零修改验证；源运行时兼容层已冻结到 M2。
- 视频（Android）：倍速与轨道选择不持久化；不加载外挂字幕文件；无集数导航、手势调亮度/音量、锁屏与画中画。
- 视频详情：混合目录（直接视频 + 季子目录）只取直接视频；没有下一集/自动连播；下载与追踪入口未接线；剧集书签无存储（已看由播放进度 ≥85% 推导）；设置对话框的“设为默认”暂为空实现。
- Linux 桌面不提供视频：MPV 的 Linux 运行时是 GPL-3.0，与 Apache-2.0 定位冲突（ADR-0012 方案 A）。
- 在线源（C2）：搜索/详情/阅读已打通；在线章节的已读/书签标记与“加入书架”尚未接数据层，下载与备份/同步仍待后续切片。

## 文档

- [产品简报](docs/00-product-brief.md)
- [目标架构](docs/01-architecture.md)
- [Phase 0 计划](docs/02-phase-0.md)
- [风险登记](docs/03-risk-register.md)
- [开放问题](docs/04-open-questions.md)
- [Phase 0 启动清单](docs/checklists/phase-0.md)
- [Phase 1 计划](docs/05-phase-1-plan.md)
- [Phase 1 M0 清单](docs/checklists/phase-1-m0.md)
- [Phase 1 M1 清单](docs/checklists/phase-1-m1.md)
- [M1 任务重排](docs/08-m1-task-plan.md)
- [品牌备忘](docs/06-brand.md)
- [Skills 与参考仓库](docs/07-skills-and-references.md)
- ADR：[KMP/CMP](docs/adr/0001-kmp-cmp-baseline.md) · [Fork + Strangler](docs/adr/0002-fork-and-strangler.md) · [JS 扩展运行时](docs/adr/0003-js-extension-runtime.md) · [许可证边界](docs/adr/0004-licensing-and-clean-room.md) · [mediamp](docs/adr/0005-mediamp-player.md) · [单轨工具链](docs/adr/0006-unified-android-toolchain.md) · [网络与 Cookie](docs/adr/0007-network-and-cookie-storage.md) · [Venera 源运行时](docs/adr/0008-venera-source-runtime.md) · [Room 数据层](docs/adr/0009-room-kmp-data-layer.md) · [Aniyomi 本地阅读器复用](docs/adr/0010-aniyomi-local-reader-reuse.md)

## 原则

- **一个源引擎**：JS/QuickJS，不再维护两套扩展模型。
- **一个播放器抽象**：mediamp，按平台后端实现，功能差异显式记录。
- **一套数据层**：库、进度、下载、同步共享；三种媒介各自拥有阅读/播放界面。
- **增量交付**：迁移期间 Android 版必须持续可发布。
- **清洁实现**：不复制 GPL 代码，不内置任何内容源，不托管内容。

## 免责声明

本项目只提供阅读器与扩展运行环境，不提供、内置、托管或推荐任何内容源。第三方源由用户自行配置，其内容与可用性由相应服务决定；使用者需自行承担合规责任。
