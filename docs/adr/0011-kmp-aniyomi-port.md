# ADR-0011: KMP 版 Aniyomi 大面积移植策略

- 状态：已接受 · 2026-10-29
- 取代：`docs/08-m1-task-plan.md` 中"本地阅读器优先、Aniyomi 仅作视觉参考"的路线（该文档的工程细节仍可引用，但里程碑排序被本 ADR 取代）。

## 背景

用户方向明确：以 Aniyomi（Apache-2.0）为底本做大面积代码移植，把 Miyomu 做成 KMP 版 Aniyomi——前端视觉对齐、视频播放引擎用 mediamp、扩展插件引擎用 JS；小说功能暂缓。

探索结论（2026-10-29，对 `reference/aniyomi` 快照）：

- Aniyomi UI 层天然是 `presentation/`（纯 Composable，86% 无 Android 依赖）+ `ui/`（Voyager Screen/ScreenModel，presenter 纯 Kotlin）分层。
- moko-resources、coil3、kotlinx-collections-immutable 本身是 KMP 库；Aniyomi 发布的 Voyager fork（`cafe.adriel.voyager:*:2.2.21-1.10.3`）带 desktop/jvm 目标。
- 必换点集中在：Injekt DI、`LocalContext` 助手（stringResource/toast/Intent）、Android View 阅读器核心、SQLDelight 数据层、`androidx.activity`/WorkManager 胶水。

## 决策

1. **保留原包名移植**：复制的文件保持 `eu.kanade.presentation.*` / `tachiyomi.presentation.core.*` / `tachiyomi.core.common.*` 包名，逐文件直接复制、diff 最小、后续可跟上游同步；Miyomu 自有适配层用 `app.miyomu.*`。
2. **依赖替换表**：

| Aniyomi | Miyomu |
|---|---|
| Injekt DI | 构造注入（`MiyomuGraph` + 顶层装配） |
| Voyager Android fork | 同一 Voyager fork 的 KMP artifact（2.2.21-1.10.3） |
| moko-resources（Android 目标） | moko-resources 0.27.1（android+jvm 双目标） |
| `Context.stringResource`（core/common i18n） | `dev.icerock.moko.resources.compose`（presentation-core Localize.kt 一个咽喉点） |
| `androidx.activity.compose.BackHandler` | `org.jetbrains.compose.ui:ui-backhandler` |
| `WindowInsets.isImeVisible` / `systemGestureExclusion` | expect/actual（android 真实现，jvm 空实现） |
| Android View 阅读器核心 | 保留 Miyomu 的 CMP viewer（Pager/Webtoon/缩放/双页） |
| SQLDelight 数据层 | 保留 Miyomu Room（library/progress）+ 本地库 + ComicSource JS 运行时作适配层 |
| WorkManager 更新任务 | 暂不移植（后续按需） |
| paging-compose（Browse） | 先用简单列表 + `Paged<T>`，需要时再评估 |

3. **新模块**：`shared/i18n`、`shared/i18n-aniyomi`（moko 双包 MR/AYMR）；`shared/presentation-core`（移植 presentation-core）；`shared/domain`（preference API + JsonPreferenceStore；A2 再加 Manga 域模型）；`shared/presentation`（A2 起移植 eu.kanade.presentation）。`shared/ui` 改造为 Voyager 屏幕壳层（home tabs、entries、reader）。
4. **里程碑排序**：Phase A 漫画侧 UI 对齐（A1 底座 → A2 库/详情/阅读器/Browse/设置移植）→ Phase B anime + mediamp → Phase C 扩展仓库安装/下载/同步。小说阅读器无限期后移。
5. **许可**：Apache-2.0 不变；NOTICE 登记移植来源；不复制 GPL（Venera）代码。

## 后果

- 正面：屏幕代码可逐文件复制，视觉/行为对齐成本大幅下降；上游 Aniyomi 修复可低成本吸收。
- 负面：与 Aniyomi 的数据模型（Manga/Chapter 等）需要适配层；Aniyomi 基于 Kotlin 2.4，个别语法在 2.3.21 下需小补丁；桌面端缺少部分 Android 专属能力（系统手势排除、IME）为空实现。
- 现有 Miyomu 本地阅读能力（目录/ZIP、进度持久化）保留为数据后端，不被废弃。

## 实施记录（A2-2 ~ A2-5，2026-10-29）

原表之外，实施过程中确认需要补充的替换点：

| 场景 | 实际做法 |
|---|---|
| Kotlin context parameters（`Modifier.animateItemFastScroll`） | 源码原样保留，模块加 `-Xcontext-parameters` 编译参数，避免与上游产生 diff |
| 每 manga 的 `readingMode` / `readerOrientation` 列 | Room schema 没有这些列，阅读器设置退化为全局 `defaultReadingMode()` / `defaultOrientationType()` |
| `androidx.core.graphics.{red,green,blue,alpha}` | 自写 ARGB 位运算扩展 |
| `LocalConfiguration` / `Configuration` | `LocalWindowInfo.current.containerSize`（平板阈值 600dp，横屏按宽 > 高） |
| Android 对话框窗口（`LocalView` + `DialogWindowProvider` 调 dim） | CMP 无窗口句柄，去掉 dim 调整，只保留菜单显隐 |
| 封面加载（**两处坑**） | 网格传 `MangaCover`、详情头部传 `Manga`；coil 侧需同时提供 `Fetcher.Factory<MangaCover>` 与 `Mapper<Manga, MangaCover>`（对应 Aniyomi app 模块的 `MangaMapper`，未移植） |
| 方向锁定 | expect/actual：Android 用 `Activity.requestedOrientation`（`ReaderOrientation.flag` 即 ActivityInfo 常量），桌面空实现 |
| 阅读器状态 | `ComicReaderUiState` → `ReaderViewModel.State` 适配器，而非重写 chrome |
| 页码指示 | 移植的 `ChapterNavigator` 用纯数字文本而非 `"1 / 2"`；`PageIndicatorText` 在 `currentPage <= 0` 时不渲染，测试改用页面 tag |

刻意保留的能力缺口（等对应数据层到位再补）：章节已读/书签无存储、分类系统、下载、追踪、`readerTheme`。

