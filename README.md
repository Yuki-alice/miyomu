# Miyomu

**跨平台三合一内容阅读器** · 名称来自 見る（看）+ 読む（读）

漫画、小说、视频，一套库、一套源、一套进度。以 Aniyomi 的 Android 体验为设计基准，用 Kotlin Multiplatform 共享核心、Compose Multiplatform 构建跨平台 UI，用 JavaScript 扩展源替代 Android-only 的 APK 扩展模型。

> 状态：**Phase A2 — Aniyomi 界面移植（A2-1 ~ A2-5 完成）+ 小说端并行开发（2026-10-30）**：书架、详情、阅读器三条主链路已跑在移植的 Aniyomi 界面上；小说端（`feature/novel` 分支）完成 TXT/EPUB 解析、双模式阅读器与库/详情闭环。小说路线见 [docs/10](docs/10-novel-plan.md)，移植路线见 [ADR-0011](docs/adr/0011-kmp-aniyomi-port.md)。
> 许可证：Apache-2.0（2026-10-28 确认，见 [ADR-0004](docs/adr/0004-licensing-and-clean-room.md)）
> 生产构建位于仓库根目录；Phase 0/1 验证代码在 `experiments/`，使用独立 Gradle 构建。

## 模块

| 模块 | 作用 |
|---|---|
| `:shared:model` | 领域模型、源描述、章节/页面 |
| `:shared:network` | Ktor 3.6、OkHttp/CIO、持久 Cookie |
| `:shared:source-api` | `ComicSource` / `ManagedComicSource` |
| `:shared:source-runtime` | 每源 QuickJS、Ktor、Ksoup DOM、KV、配额 |
| `:shared:data` | Room 2.8.5 + KSP；库/进度 schema 与 repository |
| `:shared:local` | 本地文件抽象、书库扫描、目录/ZIP 页加载 |
| `:shared:novel` | 小说解析：TXT（编码回退/章节正则）、EPUB（OPF spine/XHTML/封面）、小说库扫描 |
| `:shared:reader-comic` | 漫画阅读状态、模式、页配对 |
| `:shared:reader-novel` | 小说阅读偏好（字号/行距/边距/主题/模式）、分页数学、阅读控制器 |
| `:shared:ui` | 主题、App 壳、书架/详情/阅读器（漫画 + 小说） |
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
  local/ novel/ reader-comic/ reader-novel/ ui/
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
./gradlew :shared:local:jvmTest :shared:reader-comic:jvmTest :shared:ui:jvmTest
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
- 窄屏用底部导航，宽屏（≥720dp）用侧边 NavigationRail；小说拥有独立的「小说」Tab（对应 Aniyomi 的 anime/manga 双 Tab 结构）。

截图：[书架](evidence/a2-library-port.png) · [详情](evidence/a2-detail-port.png) · [阅读器](evidence/a2-reader-port.png)

### 小说端（feature/novel）

小说 UI 是 Miyomu 自写层：复用移植组件的方式与漫画一致（合成 `Manga` 接入网格与详情），阅读器渲染层按小说场景全新实现。

- 解析：TXT（UTF-8/BOM/GB18030 回退、中文章节正则、卷行折叠、128MB 上限）、EPUB（container→OPF spine→XHTML 段落、EPUB2/EPUB3 封面）。
- 阅读器：分页（TextMeasurer 按段落度量、超长段落按行切片）与滚动（LazyColumn）双模式；字符偏移进度，续读精确到页。
- 排版设置：字号、行距、页边距、段落间距、四档主题（浅色/羊皮纸/深色/纯黑），即改即生效，挂全局偏好。
- chrome：顶栏（书名/章节）、底栏（位置滑条、上一章/下一章、设置）、点击翻页区、桌面键盘翻页。

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

Android 使用应用私有目录，debug 包可通过 `run-as` 放入样例书：

```bash
./gradlew :androidApp:assembleDebug
adb install -r androidApp/build/outputs/apk/debug/*.apk
adb shell run-as app.miyomu.android mkdir -p files/library
adb push "Sample Zip Book.cbz" /data/local/tmp/
adb shell run-as app.miyomu.android cp "/data/local/tmp/Sample Zip Book.cbz" files/library/
```

当前切片支持目录与 ZIP/CBZ；7Z/PDF 在后续任务接入。小说支持 TXT/EPUB：把 `.txt` 或 `.epub` 放进同一库目录，「小说」Tab 即可导入阅读。

Room schema 导出在 `shared/data/schemas/`；修改实体后需同时更新 schema/迁移。

## 源运行时（M2）

源运行时读路径已实现，但当前 UI 不加载 JS 源；M2 继续真实源与源仓库。

`demo-resources/sources/demo_source.js` 是自写的 Venera 风格源，使用：

- `class ... extends ComicSource`（宿主构造并注册，不需要手动注册）
- `search.load` / `comic.loadInfo` / `comic.loadEp`
- `Network.get`、`HtmlDocument`、`this.loadData/saveData`

真实第三方源只用于本地验证，不提交；兼容范围见 [ADR-0008](docs/adr/0008-venera-source-runtime.md)。

## 已知限制

- 本地漫画阅读器当前支持目录与 ZIP/CBZ；7Z/PDF 尚未接入。
- 阅读器支持 LTR/RTL/纵向/Webtoon、双页、缩放平移、进度续读；裁边开关会记偏好但尚未按它裁图。
- **章节的已读/书签标记没有持久化**：本地库没有章节表，详情页的"标记已读""书签""删除章节"和阅读器的书签按钮只改界面状态。
- 分类系统未移植：书架只有一个合成分类，分类 Tab 恒隐藏；多选后的批量操作没有数据层支撑。
- 阅读器主题未移植（`readerTheme`），漫画阅读器 chrome 跟随 App 浅色主题而背景为纯黑（小说阅读器有自己的四档主题）。
- 小说端已知限制：EPUB 只取纯文本与封面（图片/脚注/CSS 后置）；TXT 章节启发式对正文以"第X章"开头的行会误判（legado 同类限制）；单行高于一页的极端段落会被裁剪；无章节标记的超大 TXT 整本单章，按段落度量仍可分页但首次打开偏慢。
- Android 本地库暂用应用私有目录，SAF 文件选择器待补。
- 真实 Venera 源尚未零修改验证；源运行时兼容层已冻结到 M2。
- 下载/追踪/同步、视频播放器与轨道选择在后续里程碑。

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
- [小说端计划](docs/10-novel-plan.md)
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
