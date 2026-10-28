# ADR-0010：Aniyomi 本地阅读器复用边界

- 状态：接受（M1 本地漫画/小说阅读器）
- 日期：2026-10-28
- 关联：[ADR-0002](0002-fork-and-strangler.md) · [ADR-0004](0004-licensing-and-clean-room.md) · [M1 任务重排](../08-m1-task-plan.md)

## 背景

M1 优先做表现层与本地漫画/小说阅读器。参考仓库 `reference/aniyomi` 是 Apache-2.0，包含成熟的本地漫画源、压缩包/EPUB 读取、阅读器模式与 Compose 浮层；但它的阅读器主体是 Android View（ViewPager/RecyclerView/自定义 View），并依赖 Aniyomi 的 domain、UniFile、libarchive、Injekt、Voyager、Moko Resources。

Miyomu 的目标是 Compose Multiplatform（Android + 桌面）和自有 Room/数据层，不能整体 fork 阅读器模块。需要明确哪些可以复用、哪些必须重写。

## 决策

按以下边界复用 Aniyomi：

**已经落地（本轮视觉层）**

- Material 3 主题色：`shared/ui/MiyomuTheme.kt` 直接移植 Aniyomi `TachiyomiColorScheme` 的 dark/light token（primary `#B0C6FF`、secondary 未读角标、tertiary 已下载角标、surfaceContainer 系列），保留来源注释与根 `NOTICE`。
- Home 壳：底部 `NavigationBar` / 宽屏 `NavigationRail`（书架/更新/历史/浏览/更多），对齐 Aniyomi `HomeScreen` 的分栏策略。
- 书架：`TopAppBar` + 计数 Pill + 搜索/刷新/打开目录动作、`PrimaryScrollableTabRow` 分类标签、紧凑封面网格（`GridCells.Adaptive`、标题压在封面底部渐变上、格式角标），对齐 `MangaLibraryContent` 与 `EntryCompactGridItem`。
- 详情：`TopAppBar` + 封面头图 + 作者/状态行 + 主操作按钮 + 章节计数 + 未读圆点章节行，对齐 `MangaInfoBox` / `MangaChapterListItem` 的信息层级。
- 阅读器：圆角 `Surface` 顶/底浮层、页码 Slider、阅读模式下拉菜单、上一/下一章按钮，对齐 `ReaderAppBars` / `ChapterNavigator` 的结构。

**可以移植/复用（保留版权头与来源记录）**

- `source-local/` 的本地扫描、格式识别、ComicInfo 元数据、自然排序、封面查找算法。
- `core/archive/` 的 ZIP/EPUB spine 解析思路；EPUB 文本/图片提取。
- `ui/reader/loader/` 的 Directory/Archive/Epub 页加载器算法。
- `ui/reader/model/` 的 ReaderPage/ReaderChapter 状态机概念。
- `ui/reader/setting/` 的 ReadingMode、PagerConfig、ViewerConfig 语义（模式、RTL、双页、裁边、点击区）。
- `presentation/reader/` 的 Compose 浮层结构与信息层级（顶栏、底栏、章节导航、设置）。
- `presentation/library/` 的网格/列表密度与信息层级。

**必须重写**

- `ReaderActivity`、`ReaderViewModel`：Android Activity/ViewModel、下载、追踪、Injekt 依赖；改为共享状态持有器。
- `PagerViewer`、`WebtoonViewer`、`ReaderPageImageView`：Android View/RecyclerView/自定义 View；改为 Compose Pager/LazyColumn + 手势/缩放。
- 文件访问：`UniFile`、Android `Context`、`ContentResolver` 依赖；改为 `LocalFileRef` expect/actual。
- 压缩包：`libarchive` JNI；改为 JVM/Android 可用的 ZIP/7Z/PDF 实现。
- 主题/字符串/资源：替换为 Miyomu 的 Material 3 主题与自有字符串；不复用 Moko/Aniyomi 资源。
- 小说阅读器：Aniyomi 没有文本小说实现，EPUB 只按图片页处理；TXT/EPUB 文本解析、分页、排版从零实现。

**明确不复用**

- Venera-Next 的 GPL 代码。
- Aniyomi 的下载、追踪、扩展安装、备份/同步模块。
- 任何包含第三方版权内容的测试素材。

## 后果

正面：

- 复用成熟的本地扫描、页排序、阅读模式语义，减少 C1/C2 的探索成本。
- 保持 Miyomu 的 KMP/Room 架构，不把 Android View 技术债带进桌面。
- 许可边界清晰，可审计。

负面/成本：

- Compose 阅读器仍需重写，C3 约 7 人日；不能按“复制 Aniyomi 即完成”估算。
- 小说阅读器没有参考，N1/N2 是 M1 最大不确定性。
- `libarchive`/`UniFile` 替换需要逐格式验证（ZIP/7Z/PDF/EPUB）。
- 复制/移植文件需要来源头与根 `NOTICE`；代码评审增加许可检查。

## 验证

- 复制/移植的文件顶部保留 Aniyomi 版权与 Apache-2.0 声明，并注明修改。
- 根目录 `NOTICE` 列出 Aniyomi 项目与复用范围。
- M1 Q1 检查：`NOTICE`、来源头、无 GPL 代码、无版权测试素材。
- 阅读器测试覆盖页序、RTL、双页、进度；本地源测试覆盖目录/ZIP/7Z/PDF/EPUB。
