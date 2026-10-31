# 00 · 参考项目分析：Kototoro

> **产品范围说明（2026-10-31）：** 本文是代码分析，不定义最终功能范围。Miyomu 当前策略是“核心功能照搬 + 架构优先 + MD3E 新 UI”，以 [07-架构优先方案](07-architecture-first.md) 为准。

> 分析对象：`reference/Kototoro`，`devel` 分支，v2.3.4（2026-10）。
> 方法：阅读 README、AGENTS、CLAUDE、`docs/` 下的架构文档，统计源码规模，抽查核心模块实现。
> 目的不是复刻 Kototoro，而是回答三个问题：**哪些机制值得学、哪些复杂度必须避开、Miyomu 的第一版应该做什么。**

> 局限：这是静态代码与文档分析，没有对 Kototoro 做完整运行/性能复现；行数与文件数为脚本统计，模块功能以官方文档与源码入口为依据。性能数字引用自其自身审计文档，不代表所有设备。

## 1. 一句话结论

Kototoro 的核心价值不在页面数量，而在于它把**多种内容类型 + 多种源生态 + 阅读进度 + 离线/同步**收敛到了同一套内容模型和 SourceRuntime 契约里；它最大的问题也来自同一处——能力持续累加，而 UI、数据读取和平台适配没有同步收敛，最终形成 64 万行、4,200 个 Kotlin 文件、86 个数据库版本、30 个左右 Gradle 模块的巨型单体。

Miyomu 的策略应当是：**保留契约，重做实现；保留语义，重做页面；先做纵切，再做生态。**

## 2. 规模画像

| 指标 | 数值 |
| --- | ---: |
| Kotlin 文件总数 | 4,203 |
| Kotlin 总行数 | 642,362 |
| `app` 主源码 | 1,939 文件 / 373,661 行 |
| `app` 测试源码 | 683 文件 |
| 共享 KMP 模块 | 1,366 文件 |
| Swift 文件 | 114 |
| Gradle 模块 | 约 30 个 |
| 文档 Markdown | 168 篇 |
| 数据库版本 | v86 |
| 数据库表 | 约 35 张 |
| 顶层导航目的地 | 10 个（默认展示 5 个） |
| 源生态 | Mihon、Aniyomi、Tsundoku、IReader、Legado、TVBox、CloudStream、Kotatsu parsers 等 |
| 支持平台 | Android（稳定）、Windows（预览）、iOS（预览）、Linux（开发中） |

模块规模（Kotlin/Java 文件数，粗略）：

| 模块 | 文件数 | 角色 |
| --- | ---: | --- |
| `app` | 2,697 | Android 主应用、绝大多数功能 |
| `desktop-app` | 322 | Compose Desktop 外壳 |
| `core-db` | 182 | Room KMP 数据库、实体、DAO、迁移 |
| `parser-api` | 171 | 解析器 ABI 与模型 |
| `core-ui` | 152 | 跨端共享 Compose UI |
| `core-domain` | 115 | 共享业务规则 |
| `reader-core` | 88 | 纯 Kotlin 阅读器几何 |
| `desktop-runtime` | 78 | 桌面存储/运行时 |
| `source-host` | 47 | JVM 扩展宿主 |
| `core-source` | 35 | SourceRuntime 契约与协议 |
| `core-backup` | 35 | 备份格式 |
| `core-net` | 25 | WebDAV / Drive / MAL 协议 |

这组数字说明：**Kototoro 不是一个可以“整体照搬再改 UI”的项目**。它的价值是大量经过实战验证的边界与语义，而不是可以直接裁剪的代码量。

## 3. 技术栈

来源：[`gradle/libs.versions.toml`](../reference/Kototoro/gradle/libs.versions.toml)、[`app/build.gradle`](../reference/Kototoro/app/build.gradle)。

- Kotlin 2.4.0、AGP 9.3.1、Gradle 9.7.0
- Android `compileSdk 37`、`minSdk 26`、`targetSdk 37`
- Jetpack Compose（BOM 2026.08.00）+ Material3 1.5 alpha
- Room 2.8.4（KMP）+ KSP
- Hilt / Dagger 2.60.1（Android）；共享模块构造注入
- OkHttp 5.4.0；Ktor 3.5.0（共享协议）
- kotlinx.serialization 1.11.0
- Coil 3.4.0
- Media3 1.9.3 / libmpv（桌面）
- ML Kit、ONNX Runtime、NCNN、PaddleOCR、DJL tokenizers（OCR/翻译/超分）
- QuickJS、Rhino（JS 源）
- JUnit5 + Kotest + MockK + MockWebServer
- Navigation3（Android）、自定义导航（桌面）、SwiftUI（iOS）

值得注意的工程事实：

- Android UI 已经基本全 Compose，但仍有 ViewBinding / Fragment / XML 过渡层。
- 共享模块只做构造注入，DI 框架只在平台装配层出现。
- `core-db` 用 Room KMP，但 Android 仍必须使用框架 SQLite 驱动，因为旧迁移实现的是 `migrate(SupportSQLiteDatabase)`。
- 桌面与 Android 共用 `core-ui`，但 iOS 使用原生 SwiftUI；三端页面并非同一套实现。
- 构建包含 CMake/NDK、4 个 ABI、外部兼容 JAR、libmpv、JVM 运行时等重资产，构建复杂度高。

## 4. 架构地图

```text
┌───────────────────────────── 平台外壳 ─────────────────────────────┐
│ Android: MainActivity + Hilt + WorkManager + WebView + Media3      │
│ Desktop: Compose Desktop + JVM 扩展宿主 + libmpv + WebView2        │
│ iOS: SwiftUI + 嵌入式 OpenJDK Zero + AVPlayer + WKWebView          │
└───────────────┬──────────────────────────────┬─────────────────────┘
                │                              │
        ┌───────▼────────┐             ┌───────▼────────┐
        │  app (Android) │             │ desktop-app    │
        │  2,697 files   │             │ 322 files      │
        └───────┬────────┘             └───────┬────────┘
                │                              │
        ┌───────▼──────────────────────────────▼────────┐
        │ core-ui (Compose commonMain)                  │
        │ core-domain / core-library / reader-core      │
        │ reader-novel / core-source / core-backup      │
        │ core-net / core-db (Room KMP)                 │
        └───────┬──────────────────────────────┬────────┘
                │                              │
        ┌───────▼────────┐             ┌───────▼────────┐
        │ Android 进程内 │             │ JVM source-host│
        │ Mihon/Aniyomi  │             │ dex2jar + JAR  │
        │ APK ClassLoader│             │ AndroidCompat  │
        └────────────────┘             └────────────────┘
```

### 4.1 模块边界

- `app/`：Android 功能实现，按 `core/`、`home/`、`details/`、`reader/`、`video/` 等包组织；每个包内部再分 `data/domain/ui`。
- `core-db/`：Room KMP 数据库，实体/DAO 分散在约 15 个功能包后统一搬入。
- `core-domain/`：身份键、更新生命周期、书库派生、换源、进度、统计等纯规则。
- `core-source/`：SourceRuntime、协议 DTO、`SourceEndpoint`、动态筛选、偏好树。
- `reader-core/`：纯 Kotlin 的阅读器几何、可见范围、缩放、翻页、进度锚点；零 Android/Compose 依赖。
- `reader-novel/`：小说文档块、段落切分、分页、HTML→文本、样式子集。
- `core-ui/`：Android 与桌面共享的 Compose 页面/组件/主题；iOS 尚未接入。
- `source-host/`、`parser-host/`：JVM 上加载 Mihon / Tsundoku / Aniyomi / Kotatsu / UMA / CloudStream 扩展。
- `desktop-runtime/`：桌面数据根、数据库、偏好、图片存储、备份。
- `desktop-player/`：libmpv JNA 封装。

### 4.2 架构风格

用作者自己的话说，是 **modular monolith with feature-oriented packaging**：不是严格的 Clean Architecture，也没有把每个功能拆成 Gradle 模块；而是在功能包内部做 data/domain/ui 分层，共享基础设施放 `core/*`。这是单人/小团队能维护大项目的现实选择，但也导致 `app` 过大、跨功能耦合、平台分叉。

## 5. 核心机制拆解

### 5.1 SourceRuntime：最值得照搬的契约

位置：[`core-source/.../SourceRuntime.kt`](../reference/Kototoro/core-source/src/commonMain/kotlin/org/skepsun/kototoro/core/source/SourceRuntime.kt)、[`SourceModels.kt`](../reference/Kototoro/core-source/src/commonMain/kotlin/org/skepsun/kototoro/core/source/SourceModels.kt)。

它把“一个源能做什么”压缩成一组 suspend 方法：

- `getSources()` / `describe()` / `getFilterOptions()` / `getDynamicFilters()`
- `getList()` / `getDetails()` / `getPages()` / `getPageUrl()`
- `getChapterContent()`（小说正文）
- `fetchImage()` / `fetchCover()`
- `getRelated()`
- `browserSession()` / `importCookies()`（Cloudflare）
- `getPreferences()` / `updatePreference()`

关键设计：

- **协议 DTO 与解析器 ABI 分离**：`SourceContent`、`SourceChapter`、`SourcePage` 是序列化 DTO，不直接暴露 Mihon/Kotatsu 的类。
- **64 位 ID 用十进制字符串跨 JSON**，避免 JS/JSON 精度丢失。
- **能力显式声明**：`SourceDescriptor` 里用 `isImageFetchingSupported`、`isChapterContentSupported` 等布尔值表达“可选能力”，而不是让调用方猜。
- **挑战显式建模**：`SourceChallengeException` 只把 origin 跨边界，浏览器验证由平台负责。
- **分页模式显式建模**：`OFFSET` / `PAGE_INDEX`，不假设所有源都支持页码。
- **过滤状态与动态筛选显式传输**，而不是把扩展的 Filter 对象塞进共享层。

这是 Miyomu 可以直接继承的**语义**。实现可以更小：先把方法收敛到 6 个核心操作，把偏好/浏览器/相关作品作为可选扩展接口。

### 5.2 内容模型与数据库

位置：[`core-db/.../MangaDatabase.kt`](../reference/Kototoro/core-db/src/commonMain/kotlin/org/skepsun/kototoro/core/db/MangaDatabase.kt)、[`MangaEntity.kt`](../reference/Kototoro/core-db/src/commonMain/kotlin/org/skepsun/kototoro/core/db/entity/MangaEntity.kt)、[`ChapterEntity.kt`](../reference/Kototoro/core-db/src/commonMain/kotlin/org/skepsun/kototoro/core/db/entity/ChapterEntity.kt)。

- 主内容表仍叫 `manga`，但通过 `content_type` 支持漫画/小说/视频。
- 用户状态（收藏、历史、统计、追踪）直接挂在内容行上，避免额外的“作品/投影”中间层；曾经的 Entity Graph 已在 v84 移除。
- 章节用 `(manga_id, chapter_id)` 复合主键，带上 `branch`、`source_data`、`upload_date`。
- 历史保存 `chapter_id`、`page`、`scroll`、`percent`、`chaptersCount`，并支持 EPUB 父章节。
- 收藏与分类是多对多；收藏行有 `pinned`、`sort_key`、软删除时间。
- 数据库有 86 个版本、约 35 张表，覆盖统计、书签、阅读会话、空间、追踪、词典、替换规则、同步快照等。

**可学**：内容/章节/收藏/历史/更新事件的实体关系；软删除与 tombstone；把用户状态直接挂内容行，避免过度建模。
**不可学**：把 35 张表都塞进一个 `MangaDatabase`、在列表查询里装载完整章节。Miyomu 复用 Room schema/迁移，但新代码只通过 Repository 投影访问，并用查询计数守住 N+1。

### 5.3 阅读器：项目里最强的部分

位置：[`reader-core`](../reference/Kototoro/reader-core/src/commonMain/kotlin/org/skepsun/kototoro/reader/core)、[`reader-novel`](../reference/Kototoro/reader-novel/src/commonMain/kotlin/org/skepsun/kototoro/reader)。

- `ReaderScene` 是纯几何契约：总长度、页面几何、可见节点、进度锚点、按页定位。
- `PagedReaderScene` 支持单页/双页/RTL/纵向，按固定视口步长做 O(1) 范围查询，支持宽图独占、跨章、零 CLS 锚点补偿。
- `VerticalReaderScene` / `HorizontalReaderScene` 支持连续滚动与横向条带。
- `ReaderModeDetection` 用 30% 处的页面宽高比判断条漫。
- 图片层有 `DecodePlan`、`TileGrid`、`VisibleImageTiles`、`ReaderLodPolicy`、`PagedSceneResourceWindowStrategy` 等，处理大图分块、解码尺寸、预取窗口、内存预算。
- 阅读器 UI 支持单页、双页、RTL、Webtoon、纵向、横向、连续、自动检测、裁边、缩放、背景、滤镜、自动滚动、点击网格、键盘/鼠标/触控、跨章。
- 小说有独立的文档块、分页、样式、书签、TTS、翻译层。

**可学**：纯 Kotlin 场景层、进度锚点、预取窗口、分块解码、阅读模式检测、跨章语义、输入方式无关的共享状态。
**不可学**：把 203 个文件的阅读器原样塞进 UI 模块；需要拆成 `core:reader`（几何/进度）+ `feature:reader`（呈现/平台）。

### 5.4 更新生命周期：一个被低估的领域设计

位置：[`UpdateLifecycle.kt`](../reference/Kototoro/core-domain/src/commonMain/kotlin/org/skepsun/kototoro/tracker/domain/UpdateLifecycle.kt)、[`CONTEXT.md`](../reference/Kototoro/CONTEXT.md)。

Kototoro 把“更新”拆成了几个严格区分的事实：

- **更新事件**：首次发现一批新增章节的事实；阅读、下载、查看提醒不会删除它。
- **发现时间**：客户端首次检测到新增的时间，是时间轴排序依据。
- **来源更新时间**：来源声明的发布时间，可以早于发现时间，不能替代发现时间。
- **未查看提醒**：用户尚未打开的提醒，对应红点；查看提醒不会把章节标为已读。
- **待读更新章节**：已发现但尚未读完的不同章节；按章节身份去重；下载完成不代表读完。
- **每日更新记录**：同一作品/来源/分支在同一本地日期发现的事件分组；跨日分别保留。

更新计数用章节身份（source + branch + url/id）而不是通知标志或历史总数；`mergeUpdateChapters` 会保留最早发现时间、取最大已读时间，避免同步后“复活”提醒。

**这是 Miyomu 应该直接继承的语义**。它比“有新章节就 +1、点开就清零”复杂，但能避免大量用户可见的错乱。

### 5.5 扩展生态：最大的差异化，也是最大的维护黑洞

支持的生态：

- Mihon / Tachiyomi APK 扩展（Android 进程内 ClassLoader）
- Aniyomi 视频扩展
- Tsundoku 小说扩展
- IReader 小说扩展
- Legado JSON 源
- TVBox / 苹果 CMS 源
- CloudStream3 插件
- Kotatsu / kototoro / UMA parser JAR

实现要点：

- 子优先 ClassLoader 隔离扩展依赖。
- Android 侧检测已安装 APK；桌面侧把 APK 转成 JVM JAR（dex2jar + AndroidCompat）。
- 扩展仓库、安装/更新/启用/禁用、来源测试、偏好桥接、Cloudflare WebView 桥。
- iOS 侧通过嵌入式 OpenJDK Zero 运行同一套 JVM 扩展，代价是启动慢、体积大、只能侧载。

**可学**：SourceRuntime 适配器、扩展仓库模型、安装策略、来源测试、偏好树。
**不可学**：把所有生态继续堆进 `app`。Miyomu 复用 `source-host`/`parser-host` 运行时，但按 parity 矩阵逐生态接入，运行时与 UI 分离。

### 5.6 本地内容、下载与同步

- 本地导入支持 CBZ/ZIP、文件夹、EPUB/TXT、MP4/MKV/TS/WebM/AVI/M3U8；可自动识别或强制类型；支持 `index.json` 元数据。
- 下载：WorkManager（Android）/ 桌面队列；按章清单、分页文件、SHA 校验、暂停/继续、批量操作、CBZ 导出、存储预算与 LRU 回收。
- 备份：自有 ZIP 格式 + Mihon/Aniyomi/Venera 导入；WebDAV 上传/下载/保留策略/自动备份/启动恢复。
- 同步：Android AccountManager/ContentProvider 与 WebDAV 备份是两套概念，文档明确要求区分“状态同步”和“备份恢复”。

**可学**：本地内容作为 `SourceRuntime` 的一种；按章清单与原子写；下载与阅读进度解耦；备份预览/合并而不是直接覆盖。
**不可学**：把全部云盘/自动恢复策略塞进核心。Miyomu 先复用备份格式与 WebDAV，其他云盘按 parity 后置。

### 5.7 UI：CMP 收敛是正确方向，但历史包袱太重

- Android 与桌面共享 `core-ui`，有 `HomeScreenLayout`、`TabletDetailsPanes`、`SharedSettingsRows`、`ReaderChrome` 等共享组件。
- 但截至 2026-10，仍有大量页面是两套实现；文档 `cmp-ui-convergence-plan-2026-10.md` 列了 20 个区域的收敛清单。
- iOS 使用 SwiftUI，未接入 `core-ui`，因此三端 UI 无法真正一致。
- 视觉上大量使用玻璃/模糊/渐变/大圆角；截图显示同一屏内有多种卡片语言、信息密度不一致。
- 性能审计发现：列表状态不稳定导致整页重组、在 composition 中做 map 分配、导航 lambda 不稳定、列表查询装载完整章节、更新检查逐条查询、来源缺失检查重复全库读取。6,000 部收藏、每部 20 章时，收藏读取从 7.5 秒优化到 0.66 秒——说明数据层设计比 UI 微优化更关键。

**可学**：自适应双栏/三栏、共享阅读器 chrome、设置行、空态/错误态、选择栏。
**不可学**：玻璃拟态作为默认视觉、Android 页面直接拉伸成桌面、iOS 另写一套 SwiftUI。

## 6. 值得照搬的 10 个机制

| # | 机制 | 复用方式 |
| --- | --- | --- |
| 1 | SourceRuntime 契约 | 直接复用契约与 DTO，补 Registry 门面 |
| 2 | 内容/章节/收藏/历史/更新事件模型 | 复用 Room schema 与 DAO，Repository 边界做投影 |
| 3 | 更新生命周期 | 直接采用发现时间 / 来源时间 / 已查看 / 已读完的区分 |
| 4 | 纯 Kotlin 阅读器几何 | 直接复制 `reader-core`/`reader-novel`，只改包名 |
| 5 | 阅读模式自动检测 | 直接复用 30% 处页面宽高比规则 |
| 6 | 预取窗口与分块解码 | 复用策略与测试，UI 层重写 |
| 7 | 本地内容即源 | 复用 `LocalMangaRepository`/EPUB/视频解析 |
| 8 | 下载与进度解耦 | 复用队列/清单/原子写，UI 重写 |
| 9 | 备份预览/合并 | 复用格式/合并规则，UI 重写 |
| 10 | 来源诊断/来源测试 | 复用诊断模型与测试流程，UI 重写 |

> 原则：**核心逻辑直接复制/适配；UI、导航、DI、平台装配新写。**

## 7. 架构上必须避开的 10 个坑

| # | 坑 | Miyomu 的对策 |
| --- | --- | --- |
| 1 | 64 万行、30 模块、`app` 无边界增长 | 21 个模块，core/feature/platform 分层；依赖规则 CI 可执行 |
| 2 | 86 个数据库版本、schema 耦合 | 保留 Room 与迁移，但新代码只通过 Repository/投影访问 |
| 3 | 列表查询装载完整章节 | 列表只读投影；N+1 查询用查询计数测试拦截 |
| 4 | 三端三套 UI | Android/Desktop/iOS 共用 MD3E Compose；平台只保留能力适配 |
| 5 | Android 页面拉伸成桌面 | 断点驱动自适应；桌面 rail + list-detail + supporting pane |
| 6 | 旧玻璃/模糊/多套卡片 | 全新 MD3E token/组件；旧 UI 不复用 |
| 7 | Hilt + ViewBinding + Fragment 污染新架构 | 新模块构造注入 + Koin；Hilt 只允许在 legacy 适配层 |
| 8 | 扩展生态一次性全上导致工期失控 | 按 parity 矩阵分阶段：先复用运行时，再逐个 UI/生态接入 |
| 9 | OCR/超分/弹幕等增强拖慢主链路 | 先保核心阅读/播放 parity；增强能力独立模块、后置 |
| 10 | 为了“跨平台”重写 parser/DB/ID | 保留 Long ID、Room、备份格式；先改边界，不改协议 |

## 8. 对 Miyomu 的直接启示

1. **先改架构，再换 UI。** 用 Strangler 方式把 core/feature 抽出来；旧 UI 只做行为参考。
2. **核心功能照搬。** 数据、源协议、阅读器几何、更新生命周期、备份、扩展运行时优先复用。
3. **UI 全新 MD3E。** 不复用旧页面/旧卡片/旧玻璃；设计系统从零建立。
4. **并行设计。** MD3E 原型与架构迁移两条线同步推进，但原型不阻塞迁移。
5. **数据层性能优先于 UI 性能。** 列表投影、批量查询、索引、分页，先于动画和 blur。
6. **平台差异用能力声明，不用 `if (android)`。** `PlatformCapabilities` 明确告诉 UI：有没有 WebView、有没有后台下载、有没有扩展宿主。
7. **许可证先行。** 只复制 Apache-2.0 可兼容的 Kototoro 自有代码；Kotatsu 等 GPL 代码只通过依赖/运行时集成，不直接搬。

## 9. 分析结论

Kototoro 证明了“多内容类型 + 多源生态 + 跨平台阅读器”是可行的，也暴露了这类产品的真正难点：

- 不是“怎么解析一个网站”，而是**怎么让几十种源在同一套 UI/数据/进度语义下工作**；
- 不是“怎么画一个阅读器”，而是**怎么在缩放、预取、分块、跨章、离线之间保持进度与内存稳定**；
- 不是“怎么跨平台”，而是**怎么让平台差异不污染共享层**。

Miyomu 的机会不是做一个“功能更少的 Kototoro”，而是做一个**架构更清晰、UI 全新 MD3E、功能保持 parity 的 Kototoro**。具体方案见 [07-架构优先方案](07-architecture-first.md)。
