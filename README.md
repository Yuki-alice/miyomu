# Miyomu

**跨平台三合一内容阅读器** · 名称来自 見る（看）+ 読む（读）

漫画、小说、视频，一套库、一套源、一套进度。以 Aniyomi 的 Android 体验为设计基准，用 Kotlin Multiplatform 共享核心、Compose Multiplatform 构建跨平台 UI，用 JavaScript 扩展源替代 Android-only 的 APK 扩展模型。

> 状态：**Phase A/B/C/D 完成 + 小说端合入主线（2026-10-30）**：书架、漫画/视频详情、阅读器、播放器跑在移植的 Aniyomi 界面上；在线漫画源与 anime 源（自有 JS 格式，ADR-0014）可浏览/搜索/详情/阅读/播放；下载队列、备份与 WebDAV 同步、弹幕（用户自配弹弹play 凭据）已落地；小说端（TXT/EPUB/FB2/HTML/Markdown 与 legado 在线阅读）已合入 main，三种媒介同仓可用。路线见 [ADR-0011](docs/adr/0011-kmp-aniyomi-port.md)、[10 anime 源与播放器计划](docs/10-anime-source-and-player-plan.md) 与 [12 小说端计划](docs/12-novel-plan.md)。
> 许可证：Apache-2.0（2026-10-28 确认，见 [ADR-0004](docs/adr/0004-licensing-and-clean-room.md)）
> 生产构建位于仓库根目录；Phase 0/1 验证代码在 `experiments/`，使用独立 Gradle 构建。

## 模块

| 模块 | 作用 |
|---|---|
| `:shared:model` | 领域模型、源描述、章节/页面 |
| `:shared:network` | Ktor 3.6、OkHttp/CIO、持久 Cookie |
| `:shared:source-api` | `ComicSource` / `ManagedComicSource`、`AnimeSource` / `ManagedAnimeSource` |
| `:shared:source-runtime` | 每源 QuickJS、Ktor、Ksoup DOM、KV、配额 |
| `:shared:source-repo` | 源仓库索引、安装/更新/卸载与本地清单 |
| `:shared:source-manager` | 已安装源的运行时加载与搜索/详情/页面/图片/视频解析 |
| `:shared:source-novel` | legado 书源 JSON 导入、规则求值器、在线阅读内容源（搜索/详情/目录/正文） |
| `:shared:download` | 漫画章节下载队列、离线页源 |
| `:shared:danmaku` | dandanplay 弹幕客户端（用户自配凭据） |
| `:shared:data` | Room 2.8.5 + KSP（schema v4）；库/进度/章节/剧集/分类/更新/下载/解析缓存 repository |
| `:shared:local` | 本地文件抽象、书库扫描、目录/ZIP 页加载 |
| `:shared:novel` | 小说解析：TXT（编码回退/章节正则）、EPUB、FB2、HTML、Markdown、小说库扫描 |
| `:shared:reader-comic` | 漫画阅读状态、模式、页配对 |
| `:shared:reader-novel` | 小说阅读偏好（字号/行距/边距/主题/模式）、分页数学、阅读控制器 |
| `:shared:player` | mediamp 播放器、播放器 chrome、音轨/字幕轨选择 |
| `:shared:domain` | Aniyomi 域模型、偏好 API、排序/集号识别服务 |
| `:shared:presentation-core` | 移植的 Aniyomi presentation-core 组件与主题 |
| `:shared:presentation` | 移植的 Aniyomi 屏幕层（库/详情/阅读器/anime） |
| `:shared:i18n` / `:shared:i18n-aniyomi` | moko-resources 字符串资源（en + zh-CN） |
| `:shared:ui` | 主题、App 壳、书架/详情/阅读器/播放器/源仓库/设置（漫画 + 小说 + 视频） |
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

Phase 1 / M1 的里程碑排序已被 [ADR-0011](docs/adr/0011-kmp-aniyomi-port.md) 与 [ADR-0014](docs/adr/0014-anime-source-site-rewrite-and-player-alignment.md) 取代：Phase A/B/C/D 已全部完成。当前状态与缺口见 [11 号交接](docs/11-handoff-2026-10-30.md)；[Phase 1 计划](docs/05-phase-1-plan.md) 与 [M1 任务重排](docs/08-m1-task-plan.md) 仅保留为历史范围与验收参考。

## 仓库结构

```text
androidApp/     Android 应用入口（Compose）
desktopApp/     桌面应用入口（Compose Desktop）
shared/         共享 KMP 模块（21 个）
  model/ network/ domain/ source-api/ source-runtime/ source-repo/ source-manager/ source-novel/
  download/ danmaku/ data/ local/ novel/ reader-comic/ reader-novel/ player/ ui/
  presentation/ presentation-core/ i18n/ i18n-aniyomi/
demo-resources/ 自造漫画/anime/小说源（含苹果CMS 模板）
tools/          fixture server 与样例库生成器
evidence/       各阶段双端截图与验证证据
docs/           产品、架构、交接、ADR、计划
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
./gradlew :shared:source-runtime:jvmTest :shared:source-repo:jvmTest :shared:source-manager:jvmTest :shared:data:jvmTest :shared:domain:jvmTest
./gradlew :shared:download:jvmTest :shared:danmaku:jvmTest :shared:network:jvmTest
./gradlew :shared:novel:jvmTest :shared:reader-novel:jvmTest :shared:source-novel:jvmTest
# 真站源冒烟（手动，第三方站点不进 CI）：
./gradlew :shared:source-runtime:jvmTest -Pcanary
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
- 播放器：mediamp Surface + 播放器 chrome（顶栏/底栏、自动隐藏、双击 ±10 秒、倍速、音轨/字幕、亮度/音量手势、下一集/连播、弹幕）。
- 在线源：源仓库管理、浏览/全局搜索、源详情（漫画/anime）、在线阅读与在线播放。
- 小说：独立的「小说」Tab（对应 Aniyomi 的 anime/manga 双 Tab 结构），详情/阅读器为自写层，详见下文「小说端」。
- 书架/数据：分类 Tab、批量分类、更新检查、历史/更新、下载队列、备份/WebDAV。
- 设置：外观（主题/动态色）、阅读、播放、下载、数据与同步、弹幕。
- 窄屏用底部导航，宽屏（≥720dp）用侧边 NavigationRail。

截图：[书架](evidence/a2-library-port.png) · [漫画详情](evidence/a2-detail-port.png) · [阅读器](evidence/a2-reader-port.png) · [视频详情](evidence/b2-video-detail.png) · [播放器](evidence/b1-player-controls.png) · [播放设置](evidence/b1-player-settings.png)

### 小说端

小说 UI 是 Miyomu 自写层：复用移植组件的方式与漫画一致（合成 `Manga` 接入网格与详情），阅读器渲染层按小说场景全新实现。

- 解析：TXT（编码回退、章节正则）、EPUB（spine→内容块、封面）、FB2（分节章节、base64 插画与封面）、HTML、Markdown；文内插画按内容块渲染（EPUB `<img>`、FB2 `<binary>`），分页预留图片空间、滚动模式混排。
- 阅读器：分页（TextMeasurer 按段落度量、超长段落按行切片）与滚动（LazyColumn）双模式；字符偏移进度，续读精确到页。
- 排版设置：字号、行距、页边距、段落间距、四档主题（浅色/羊皮纸/深色/纯黑），即改即生效，挂全局偏好。
- 目录/书签/书内搜索：阅读器内呼出章节列表（标题过滤、当前章高亮）与书签列表；全书跨章节搜索直达「章节 + 偏移」；书签按书存 JSON 偏好（零 schema 冲突）。
- 护眼暖色滤镜四档（Play Books Night Light 同类）与音量键翻页（Android 生效）；chrome：顶栏（书名/章节/书签）、底栏（位置滑条、上一章/下一章、目录、设置）、点击翻页区、桌面键盘翻页。
- 在线阅读（legado 书源）：浏览 Tab 导入书源 JSON → 搜索 → 加入书架 → 目录/正文在线阅读，进度与书签与本地共用同一套存储；规则引擎为 legado 语法的独立实现（无 GPL 代码）。

「浏览 → 全局搜索」一次查询同时打散到已安装的 JS 源（漫画 / anime）与已导入的 legado 小说书源，三种媒介的结果分节并列；点小说结果直接进在线详情。

截图：[小说库](evidence/novel-library.png) · [小说详情](evidence/novel-detail.png) · [小说阅读器](evidence/novel-reader.png)

## 本地阅读

生成自造样例库（纯色 PNG，无第三方内容）：

```bash
python3 tools/make_sample_library.py
```

桌面默认库目录是 `~/.miyomu/library`；点“刷新”后打开目录或 CBZ：

```bash
./gradlew :desktopApp:run
```

Android 可在应用内通过 SAF 选择库目录（持久权限）；debug 包也可用 `run-as` 放入样例书：

```bash
./gradlew :androidApp:assembleDebug
adb install -r androidApp/build/outputs/apk/debug/*.apk
adb shell run-as app.miyomu.android mkdir -p files/library
adb push "Sample Zip Book.cbz" /data/local/tmp/
adb shell run-as app.miyomu.android cp "/data/local/tmp/Sample Zip Book.cbz" files/library/
```

当前切片支持目录与 ZIP/CBZ；7Z/PDF 在后续任务接入。小说支持 TXT/EPUB：把 `.txt` 或 `.epub` 放进同一库目录，「小说」Tab 即可导入阅读。

Room schema 导出在 `shared/data/schemas/`；修改实体后需同时更新 schema/迁移。

## 视频播放

库里的视频（mp4/m4v/mkv/webm/avi/mov/ts/m2ts）有三种布局：

- 单个视频文件 = 单集视频书；
- 目录里直接放视频 = 多集视频书（按文件名解析集号，如 `S01E02`、`第 3 集`）；
- 目录下每个子目录放一季的视频 = 系列（详情页列出季，点季进入该季的剧集）。

点开进入 anime 风格的详情页，点“开始/继续观看”进入播放器，支持播放/暂停、进度条、双击左右 ±10 秒、倍速、音轨与内嵌字幕轨选择；进度按集保存，退出后再打开从上次位置继续。封面会优先使用目录里的 `cover`/`folder`/`poster` 图片（或与视频同名的图片）。详情页的筛选按钮可设置过滤、排序、显示方式与季徽标，设置按书保存。

Android 使用 ExoPlayer（`mediamp-exoplayer`）。桌面端 macOS（arm64/x64）与 Windows（x64/arm64）内置 MPV 运行时（Apache-2.0 + LGPL-2.1+，见 [ADR-0012](docs/adr/0012-desktop-video-backend-licensing.md)）；Linux 的 MPV 构建是 GPL-3.0，不随应用分发，打开视频会显示说明页而不是崩溃。

## 在线源（anime + 漫画）

anime 源用自有的 `AnimeSource` JS 格式（ADR-0014）：`search` / `detail` / `resolve`，resolve 返回流地址列表与防盗链 headers（mediamp `UriMediaData.headers` 直达 ExoPlayer/MPV）。`demo-resources/sources/anime/` 内有已验证的 [omofun 源](demo-resources/sources/anime/omofun.js)（canary 通过）与 [苹果CMS 模板](demo-resources/sources/anime/applecms_template.js)（一个模板覆盖全部标准 /api.php/provide/vod/ 采集站）。真站冒烟：`./gradlew :shared:source-runtime:jvmTest -Pcanary`。

## 下载 / 备份 / 弹幕

- 漫画章节可从详情页下载（顺序队列、断点续传），离线后自动用本地页源阅读；「更多 → 下载队列」管理。
- 「更多 → 备份与同步」：书库/进度/标记导出 JSON、本地恢复，以及 WebDAV 上传/下载（用户自配 URL 与凭据）。
- 弹幕走弹弹play 开放 API，需在 dev.dandanplay.com 注册 AppId/AppSecret 并在「更多 → 弹幕设置」填入；播放设置内可开关。

## 源仓库（Phase C）

「更多 → 源仓库」可以添加索引 JSON 的 URL，浏览仓库里的 JS 源并安装/更新/卸载。安装会把脚本
落到应用的 `sources/<id>/source.js`，元数据与仓库地址保存在 `sources/manifest.json`；格式与
安全边界见 [ADR-0013](docs/adr/0013-js-source-repository-format.md)。Miyomu 不内置、不推荐任何仓库。
安装后的源会由 `shared:source-manager` 加载进 QuickJS 运行时，“浏览”Tab 列出可用源：
点源进入搜索，点结果进入详情（移植的 `MangaScreen`），点章节进入阅读器（复用移植的阅读
chrome，页面与图片经源运行时加载）。

## 源运行时

源运行时已接入产品链路：安装的 JS 源由 `source-manager` 加载，浏览/搜索/详情/阅读与 anime 播放都走这一套。真实第三方源（Venera 风格漫画源与自写 anime 站源）只用于本地验证，不提交；兼容范围见 [ADR-0008](docs/adr/0008-venera-source-runtime.md) 与 [ADR-0014](docs/adr/0014-anime-source-site-rewrite-and-player-alignment.md)。

`demo-resources/sources/demo_source.js` 是自写的 Venera 风格源，使用：

- `class ... extends ComicSource`（宿主构造并注册，不需要手动注册）
- `search.load` / `comic.loadInfo` / `comic.loadEp`
- `Network.get`、`HtmlDocument`、`this.loadData/saveData`

真实第三方源只用于本地验证，不提交；兼容范围见 [ADR-0008](docs/adr/0008-venera-source-runtime.md)。

## 已知限制

- 本地漫画：目录/ZIP/CBZ；7Z/PDF 加载器尚未接入。
- 本地视频：混合目录（直接视频 + 季子目录）只取直接视频；本地剧集书签未持久化（已看由播放进度推导）；“设为默认”暂无全局存储。
- 播放器：音轨/字幕选择不持久化；无锁屏/画中画/MediaSession；外挂字幕（同名 .srt/.ass）受 mediamp 0.5.3 限制暂不加载，待上游支持或自研；Linux 桌面无视频（ADR-0012）。
- 小说：EPUB 只取纯文本与封面（图片/脚注/CSS 后置）；TXT 章节启发式对正文以“第X章”开头的行会误判；单行高于一页的极端段落会被裁剪；无章节标记的超大 TXT 整本单章，首次打开偏慢；MOBI/AZW3 不支持。
- 小说在线阅读（legado 书源）：发现/分类页、`nextContentUrl` 分页目录抓取、在线书源的书内全文搜索待补；真实第三方书源兼容性需实测（引擎对自造 fixture 全绿）。
- 在线源：Venera 兼容性已量化（13 个社区源 12 个零改构造通过，见 [14](docs/14-venera-compat.md)），但 `Convert` 编码/加密未实现，命中它的源调用时失败；第一波为 omofun + 苹果CMS 采集站家族；canary 需手动 `-Pcanary`；站点改版维护由项目承担；tracker 未实现；WebDAV 仅 Basic 认证；弹幕需自配弹弹play AppId/AppSecret。
- 平台：Windows 端 MPV 播放与安装包（jpackage msi）尚未实机验证；桌面暂无自动更新；iOS 生产壳/AVKit 未开始。
- CI：KSP 2.3.12 在 GitHub runner 上解析失败导致所有 job 红（本地可解析）；`download`/`danmaku` 测试尚未加入；无 Windows job 与安装包冒烟。

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
- [M1 任务重排（历史）](docs/08-m1-task-plan.md)
- [09 交接（历史，B1–C2）](docs/09-handoff-2026-10-29.md)
- [10 anime 源与播放器计划](docs/10-anime-source-and-player-plan.md)
- [11 当前交接（Phase D）](docs/11-handoff-2026-10-30.md)
- [12 小说端计划](docs/12-novel-plan.md)
- [13 小说端对标调研](docs/13-novel-benchmark.md)
- [品牌备忘](docs/06-brand.md)
- [Skills 与参考仓库（历史）](docs/07-skills-and-references.md)
- ADR：[KMP/CMP](docs/adr/0001-kmp-cmp-baseline.md) · [Fork + Strangler](docs/adr/0002-fork-and-strangler.md) · [JS 扩展运行时](docs/adr/0003-js-extension-runtime.md) · [许可证边界](docs/adr/0004-licensing-and-clean-room.md) · [mediamp](docs/adr/0005-mediamp-player.md) · [单轨工具链](docs/adr/0006-unified-android-toolchain.md) · [网络与 Cookie](docs/adr/0007-network-and-cookie-storage.md) · [Venera 源运行时](docs/adr/0008-venera-source-runtime.md) · [Room 数据层](docs/adr/0009-room-kmp-data-layer.md) · [Aniyomi 本地阅读器复用](docs/adr/0010-aniyomi-local-reader-reuse.md) · [KMP 移植](docs/adr/0011-kmp-aniyomi-port.md) · [桌面运行时许可](docs/adr/0012-desktop-video-backend-licensing.md) · [源仓库格式](docs/adr/0013-js-source-repository-format.md) · [anime 源与播放器](docs/adr/0014-anime-source-site-rewrite-and-player-alignment.md)

## 原则

- **一个源引擎**：JS/QuickJS，不再维护两套扩展模型。
- **一个播放器抽象**：mediamp，按平台后端实现，功能差异显式记录。
- **一套数据层**：库、进度、下载、同步共享；三种媒介各自拥有阅读/播放界面。
- **增量交付**：迁移期间 Android 版必须持续可发布。
- **清洁实现**：不复制 GPL 代码，不内置任何内容源，不托管内容。

## 免责声明

本项目只提供阅读器与扩展运行环境，不提供、内置、托管或推荐任何内容源。第三方源由用户自行配置，其内容与可用性由相应服务决定；使用者需自行承担合规责任。
