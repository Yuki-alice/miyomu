# Phase 1 M1 任务重排：本地漫画与小说阅读器

- 状态：执行中 · 2026-10-28；U1/L1/L2/C1/C2 基础切片已落地（目录/ZIP、书架/详情/阅读、续读）
- 基线：`c065b3d`（仓库结构标准化完成；根目录为生产构建，`experiments/` 为独立构建）
- 决策：M1 先做**表现层 + 本地漫画/小说阅读器**；Aniyomi 作为行为与代码参考；JS 源运行时冻结，真实 Venera 源验证移到 M2。
- 关联：[Phase 1 计划](05-phase-1-plan.md) · [M1 清单](checklists/phase-1-m1.md) · [ADR-0010](adr/0010-aniyomi-local-reader-reuse.md)

## 1. 范围

| 保留在 M1 | 后移 |
|---|---|
| 共享 UI 壳、主题、导航、库/详情/设置 | JS 源兼容层补齐、真实 Venera 源验证（M2） |
| 本地文件导入与本地库扫描 | 源仓库索引/安装/更新（M2） |
| 本地漫画阅读器（目录、ZIP/CBZ、7Z/CB7、PDF） | 在线源阅读、下载/离线库（M2） |
| 本地小说阅读器（TXT、EPUB） | WebDAV 同步、备份恢复（M3） |
| 阅读模式、RTL/LTR、双页、缩放、预取、进度 | 视频播放器与轨道选择（M3） |
| Aniyomi 屏幕对照与许可边界 | iOS App 壳（M1 之后） |

M1 完成后，用户不依赖任何在线源，就能在 Android 和桌面导入并阅读本地漫画与小说。

## 2. 目标与退出标准

**目标**：Android 与桌面共享同一套 UI、本地库、漫画阅读器和小说阅读器，完成“导入 → 库 → 详情 → 阅读 → 续读”的本地闭环。

M1 完成必须同时满足：

- [ ] 本地漫画支持目录、ZIP/CBZ、7Z/CB7、PDF；章节/页面排序、封面、详情、阅读进度可用。
- [ ] 本地小说支持 TXT、EPUB；章节识别、分页/滚动、字体/字号/行距/主题、进度可用。
- [ ] 漫画阅读器支持 LTR/RTL、纵向分页、Webtoon/连续纵向、双页、缩放、预取、上一/下一章。
- [ ] 库/详情/阅读器/设置四个屏幕在 Android 与桌面可用；双端入口只做平台壳。
- [ ] 本地库与进度持久化；重启后可续读；文件被移动/删除时有错误状态，不崩溃。
- [ ] `:shared:local`、`:shared:reader-comic`、`:shared:reader-novel`、`:shared:library` JVM 测试通过；`:desktopApp:compileKotlin`、`:androidApp:assembleDebug` 通过。
- [ ] 双端截图/录屏证据写入 `evidence/`；Aniyomi 对照清单签字；`NOTICE` 与文件来源头完整。
- [ ] 不提交任何受版权保护的漫画/小说文件；测试素材自造或公共领域。

## 3. Aniyomi 复用边界

Aniyomi 为 Apache-2.0，代码可复用但必须保留版权/NOTICE；不得复制 Venera 的 GPL 代码。

| Aniyomi 区域 | 复用方式 | 必须重写/替换 |
|---|---|---|
| `source-local/` 本地扫描、格式识别、ComicInfo 元数据、自然排序 | 移植算法到 `shared:local`；保留来源头 | `UniFile`、Android `Context`、`StorageManager` |
| `core/archive/` ZIP/EPUB 读取 | 复用 ZIP/EPUB spine 解析思路；EPUB 用 Ksoup | `libarchive`、`UniFile`、Android `InputStream` 假设 |
| `ui/reader/loader/` Directory/Archive/Epub 页加载器 | 移植算法；新增 PDF | Android 文件 API；改为 `LocalPageSource` |
| `ui/reader/model/` ReaderPage/ReaderChapter 状态 | 复用概念与状态机 | 不直接复制 Android ViewModel 依赖 |
| `ui/reader/setting/` ReadingMode、PagerConfig、ViewerConfig | 移植模式和设置语义 | Injekt/Preference 存储；改为项目设置层 |
| `presentation/reader/` 顶栏/底栏、章节导航、设置浮层 | 适配 Compose Multiplatform；替换资源/字符串 | Moko Resources、Aniyomi 主题、Android 资源 |
| `ui/reader/viewer/pager`、`webtoon` | 仅行为参考：翻页、双页、缩放、连续滚动 | Android View/RecyclerView/ViewPager；用 Compose Pager/LazyColumn 重写 |
| `ui/reader/ReaderActivity`、`ReaderViewModel` | 行为参考 | Android Activity/ViewModel/下载/追踪依赖；重写共享状态层 |
| `presentation/library`、详情屏幕 | 复刻布局、信息层级、交互 | Voyager/Injekt/Moko；用共享 UI 重写 |
| 小说阅读器 | **Aniyomi 没有文本小说阅读器**；EPUB 只按图片页处理 | 从零实现 TXT/EPUB 文本解析与排版 |

直接复制仅限平台无关或已明确移植的文件；复制文件加来源头，仓库根目录新增 `NOTICE`，并在 [ADR-0010](adr/0010-aniyomi-local-reader-reuse.md) 记录边界。

## 4. 模块划分

| 模块 | 职责 |
|---|---|
| `:shared:ui` | 主题、通用组件、App 壳、导航、Reader 浮层基础 |
| `:shared:local` | `LocalFileRef`、导入、目录/压缩包/PDF/EPUB 扫描、元数据、封面 |
| `:shared:library` | 库/历史/详情/设置屏幕与状态 |
| `:shared:reader-comic` | 漫画页加载器、阅读状态、漫画阅读 UI |
| `:shared:reader-novel` | TXT/EPUB 解析、小说状态、排版阅读 UI |
| `:shared:model` | 扩展本地书/章节/小说内容模型 |
| `:shared:data` | 继续使用现有 Room 库/进度 repository |
| `:androidApp` / `:desktopApp` | 文件选择器、窗口/Activity、平台入口 |

不新增 DI 框架；构造注入 + 状态持有器。M1 不引入第三方导航库，`AppRoute` + 状态保存。

## 5. 任务图

| ID | 优先级 | 任务 | 依赖 | 估算 | 验收 |
|---|---|---|---|---|---|
| U1 | P0 | 共享 UI 基础与 App 壳 | none | 4 人日 | 双端入口只调用 `MiyomuApp()`；库/详情/阅读/设置路由可用 |
| L1 | P0 | 本地文件抽象与导入 | none | 3 人日 | 桌面选择目录/文件；Android SAF 授权持久化 |
| L2 | P0 | 本地库扫描与元数据 | L1 | 3 人日 | 目录/压缩包识别、自然排序、封面、ComicInfo |
| C1 | P0 | 漫画页加载器 | L2 | 5 人日 | 目录/ZIP/CBZ/7Z/CB7/PDF 页列表与字节流；测试通过 |
| C2 | P0 | 漫画阅读状态 | C1 | 3 人日 | 页序、章节切换、RTL、双页配对、进度映射有测试 |
| C3 | P0 | 漫画阅读 UI | C2, U1 | 7 人日 | LTR/RTL/纵向分页/Webtoon/连续、双页、缩放、预取、浮层 |
| C4 | P0 | 漫画库/详情/进度集成 | C3, L2 | 2 人日 | 导入→库→详情→阅读→续读双端可用 |
| N1 | P0 | 小说解析 TXT/EPUB | L2 | 3 人日 | 章节识别、文本提取、编码回退、解析测试 |
| N2 | P0 | 小说阅读 UI/排版 | N1, U1 | 5 人日 | 分页/滚动、字号/行距/主题、章节导航 |
| N3 | P0 | 小说库/详情/进度集成 | N2, L2 | 2 人日 | 导入→库→详情→阅读→续读双端可用 |
| Q1 | P0 | 双端验收/性能/证据 | C4, N3 | 3 人日 | 大图/长文/1,000 项库、截图、NOTICE 检查 |

P0 合计约 **40 人日**；单人全职加缓冲约 **8 周**。如果必须压到 6 周，砍 PDF/7Z、双页和小说高级排版，保留目录 + ZIP/CBZ + TXT/EPUB 主路径。

## 6. 里程碑

### M1a：共享壳与本地漫画（第 1–5 周）

| 周 | 任务 |
|---|---|
| 第 1 周 | U1 主体；L1 起步 |
| 第 2 周 | L1 收尾；L2；U1 收尾 |
| 第 3 周 | C1；C2 起步 |
| 第 4 周 | C2 收尾；C3 主体 |
| 第 5 周 | C3 收尾；C4；漫画双端验收 |

### M1b：本地小说与总验收（第 6–8 周）

| 周 | 任务 |
|---|---|
| 第 6 周 | N1；N2 起步 |
| 第 7 周 | N2 收尾；N3 |
| 第 8 周 | Q1；性能/证据/修复/缓冲 |

## 7. 关键设计

### 7.1 本地文件与导入（L1/L2）

- `LocalFileRef`：`name`、`isDirectory`、`list()`、`openStream()`、`size`、`lastModified`、`uri`；桌面用 `java.nio.file.Path`，Android 用 SAF `Uri` + `ContentResolver`。
- 导入：桌面 `JFileChooser`/`FileDialog`；Android `ActivityResultContracts.OpenDocumentTree`/`OpenDocument`，持久化 URI 权限。
- 本地书 ID：`sourceId = "local"`，`sourceKey = 稳定文件 URI/路径`；文件缺失时详情/阅读显示可恢复错误。
- 扫描：目录下每个子目录为一本书；压缩包/PDF/EPUB 单文件为一本书；自然排序（`compareToCaseInsensitiveNaturalOrder` 的等价实现）。
- 元数据：`ComicInfo.xml`（复用 Aniyomi 字段映射）、封面取第一张图；无元数据时用文件名。
- 测试：临时目录/内存 ZIP 的 JVM 测试；Android SAF 手动验证。

### 7.2 漫画页加载器（C1）

- 支持：目录、ZIP/CBZ、7Z/CB7、PDF；RAR 按 Alpha 范围排除。
- ZIP：`java.util.zip`（Android/JVM 均可用）；7Z：Apache Commons Compress；PDF：Android `PdfRenderer`，桌面 PDFBox。
- EPUB 图片页：复用 ZIP + OPF/spine 解析；若 EPUB 含文本，归小说解析器处理。
- 页排序：自然排序；图片检测用扩展名 + 魔数；封面取第一页。
- 大图：按目标尺寸采样解码；保留原图字节用于缩放/导出。
- 测试：目录、ZIP、7Z、PDF 的页数/顺序/首图；损坏文件错误分支。

### 7.3 漫画阅读器（C2/C3）

- 模式：LTR、RTL、纵向分页、Webtoon、连续纵向；双页拆分/反转/旋转适配按 Aniyomi 语义子集实现。
- 状态：`ReaderChapter`（Wait/Loading/Loaded/Error）、`ReaderPage`、当前页、预取窗口、上一/下一章；页索引映射到 `ProgressRepository`。
- UI：Compose `HorizontalPager`/`VerticalPager` + `LazyColumn`；双指缩放/平移；点击区/键盘翻页；顶部/底部浮层、页码、章节导航、阅读设置。
- 预取：当前页前后各 2–3 页；章节末尾预取下一章；取消策略避免泄漏。
- 错误：缺页/损坏/文件移动显示占位与重试，不崩溃。
- 测试：页序/双页/RTL/进度的 JVM 测试；桌面 Compose UI 测试；双端手动截图。

### 7.4 小说解析与阅读（N1/N2/N3）

- TXT：UTF-8/BOM，JVM/Android 回退 GB18030；章节正则（`第…章`、`Chapter …`、分隔线），失败则单章；M1 限制文件大小（如 20 MB）。
- EPUB：ZIP → `META-INF/container.xml` → OPF manifest/spine → XHTML；Ksoup 提取正文；一个 spine 项一章；图片可选显示。
- 排版：分页模式用 Compose `TextMeasurer` 按可视区域分页；滚动模式用 `LazyColumn`；字号、行距、主题、页边距。
- 进度：字符偏移 + 章节 id；重启恢复；章节切换。
- 测试：自造 TXT/EPUB fixture 的章节数/文本/编码；分页偏移映射；不使用版权文本。

### 7.5 表现层与 Aniyomi 对照（U1/L2/C4/N3）

- Library：网格/列表、Library/History 标签、搜索、排序/筛选、空状态；复刻 Aniyomi 的 comfortably/compact 两种密度。
- Detail：封面、标题、作者、简介、标签、章节列表、继续阅读、章节操作。
- Reader：顶栏（返回、标题、书签）、底栏（阅读模式、方向、裁边、设置）、页码、章节导航、设置浮层。
- Settings：阅读模式、RTL/LTR、双页、裁边、主题、字号/行距（小说）、点击区。
- 桌面：鼠标滚轮/键盘方向键/全屏；Android：触摸、返回键、沉浸式。
- 对照清单作为 Q1 验收的一部分；不追求 100% 像素一致，但信息层级/交互一致。

### 7.6 许可与来源

- Aniyomi 代码 Apache-2.0；复制/移植文件保留版权头，新增 `NOTICE`，记录文件来源与修改。
- 不复制 Venera GPL 代码；本地阅读器不需要 Venera。
- `source-local`/`core/archive` 的第三方依赖（libarchive、UniFile）不直接带入；用 JVM/Android 替代实现。
- ADR-0010 记录复用边界；代码评审检查来源头与 NOTICE。

## 8. 验证

```bash
./gradlew :shared:local:jvmTest
./gradlew :shared:reader-comic:jvmTest :shared:reader-novel:jvmTest
./gradlew :shared:library:jvmTest :shared:data:jvmTest
./gradlew :desktopApp:compileKotlin :androidApp:assembleDebug
```

本地验收素材放 `fixtures/local/`（gitignored），只使用自造/公共领域文件：

- 漫画：目录、CBZ、ZIP、7Z、PDF 各一份；含非自然命名页（`1.jpg`、`2.jpg`、`10.jpg`）。
- 小说：TXT（UTF-8/GB18030）、EPUB（多章节、含图片）。
- 双端手动流程：导入 → 库 → 详情 → 阅读 → 退出 → 续读。

## 9. 风险与取舍

- **Aniyomi 阅读器是 Android View**：直接复制不可行；C3 的 7 人日已包含 Compose 重写成本。若低估，优先保分页/Webtoon/缩放，砍双页旋转/裁边。
- **小说阅读器无参考**：N1/N2 是最大不确定性；先 TXT 后 EPUB，先滚动后分页；N1 失败则 M1 只交付 TXT。
- **PDF/7Z 依赖**：Android/JVM 行为差异；C1 先用小样本证明，失败则移到 M1.5。
- **SAF/URI 持久化**：Android 文件移动/权限丢失；L1 必须处理并可恢复。
- **大图/长文性能**：C3/Q1 采样解码、预取窗口、1,000 项库；不达标不加功能。
- **许可**：复制 Aniyomi 文件必须加 NOTICE/来源头；这是 Q1 的检查项。
- **范围**：8 周是现实估计；6 周版本需要砍 PDF/7Z/双页/高级排版。

## 10. 与上一版差异

- 上一版以 JS 源兼容 + 在线阅读为主线；本版改为本地库 + 本地漫画/小说阅读器，JS 源运行时冻结到 M2。
- 真实 Venera 源、源仓库、下载/同步、播放器轨道全部后移；M1 不依赖任何第三方内容源。
- Aniyomi 复用从“UI 参考”细化为“算法/加载器可移植，Android View 阅读器必须重写；小说从零实现”。
