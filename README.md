# Miyomu

**跨平台三合一内容阅读器** · 名称来自 見る（看）+ 読む（读）

漫画、小说、视频，一套库、一套源、一套进度。以 Aniyomi 的 Android 体验为设计基准，用 Kotlin Multiplatform 共享核心、Compose Multiplatform 构建跨平台 UI，用 JavaScript 扩展源替代 Android-only 的 APK 扩展模型。

> 状态：**Phase 1 — M1 表现层与本地阅读器（进行中）**
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
| `:shared:reader-comic` | 漫画阅读状态、模式、页配对 |
| `:shared:ui` | 主题、App 壳、书架/详情/阅读器 |
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
./gradlew :shared:local:jvmTest :shared:reader-comic:jvmTest :shared:ui:jvmTest
./gradlew :shared:source-runtime:jvmTest :shared:data:jvmTest
```

验证代码是独立构建，从仓库根目录执行：

```bash
./gradlew -p experiments :spikes:s3-ui:run
```

## 界面

视觉层按 Aniyomi 的结构重做（Apache-2.0，见 [NOTICE](NOTICE) 与 [ADR-0010](docs/adr/0010-aniyomi-local-reader-reuse.md)）：

- 主题色沿用 Aniyomi 默认 `TachiyomiColorScheme` 的 M3 token。
- 书架：顶栏计数 Pill、分类 Tab、紧凑封面网格、格式角标、搜索/刷新/打开目录。
- 详情：封面头图、作者与格式行、主操作按钮、章节计数与未读圆点。
- 阅读器：圆角上下浮层、页码 Slider、阅读模式菜单、上一/下一章。
- 窄屏用底部导航，宽屏（≥720dp）用侧边 NavigationRail。

截图：[书架](evidence/m1-library-anilook.png) · [详情](evidence/m1-detail-anilook.png) · [阅读器](evidence/m1-reader-anilook.png)

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

## 源运行时（M2）

源运行时读路径已实现，但当前 UI 不加载 JS 源；M2 继续真实源与源仓库。

`demo-resources/sources/demo_source.js` 是自写的 Venera 风格源，使用：

- `class ... extends ComicSource`（宿主构造并注册，不需要手动注册）
- `search.load` / `comic.loadInfo` / `comic.loadEp`
- `Network.get`、`HtmlDocument`、`this.loadData/saveData`

真实第三方源只用于本地验证，不提交；兼容范围见 [ADR-0008](docs/adr/0008-venera-source-runtime.md)。

## 已知限制

- 本地阅读器当前支持目录与 ZIP/CBZ；7Z/PDF/EPUB/TXT 尚未接入。
- 阅读器已有 LTR/RTL/纵向、翻页、进度续读；缩放、Webtoon/连续、双页、设置面板待补。
- 本地小说（TXT/EPUB）尚未开始。
- Android 本地库暂用应用私有目录，SAF 文件选择器待补。
- 真实 Venera 源尚未零修改验证；源运行时兼容层已冻结到 M2。
- 下载/同步、视频播放器与轨道选择在后续里程碑。

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
