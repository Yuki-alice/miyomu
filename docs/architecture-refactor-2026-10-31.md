# Miyomu 架构重构实施记录（2026-10-31）

> **依据**：`docs/architecture-review-2026-10-30.md` 的路线图"立刻 + 本迭代"两档。
> 用户在 2026-10-31 批准执行架构重构优化，覆盖此前"只出文档"的决定（见 `product-goals.md` Q4 的更新注记）。
> 本文记录已实施项、验证证据与剩余债务；条目编号（AR-xx）沿用审阅报告。

---

## 1. 已实施项

### AR-01 导航状态可保存恢复（P0）

- `shared/ui-core/.../navigation/MiyomuNavigator.kt`：back stack 由 `remember` 改为 `rememberSaveable`
  + `MiyomuBackStackSaver`（kotlinx.serialization JSON 往返）。旋转/进程死亡后恢复原栈，包括
  reader key 携带的位置；解码失败回退到 `HomeKey` 而非崩溃。
- 新增 `shared/ui-core/src/jvmTest/.../MiyomuBackStackSaverTest.kt`：混合 key 类型往返 + 非法输入回退两个测试。

### AR-02 移除主线程 `runBlocking`（P0）

- `shared/ui/.../home/HomeScreen.kt`：历史记录两条点击回调不再 `runBlocking` 查 Room；
  改为 `rememberCoroutineScope().launch` 中异步查询后导航，公共逻辑收敛为 `onOpenSourceItem`。

### AR-05 服务生命周期收口（P1，本轮止血部分）

- 新增 `shared/application/.../MiyomuServices.kt`：数据层服务容器（Room、偏好、仓库、源运行时、
  下载队列、HTTP client、app 级 scope），实现 `Closeable`。
- **Android**：新增 `app/android/.../MiyomuAndroidApp : Application`，清单注册 `android:name`；
  服务在 Application 层持有，Activity 旋转/重建不再关闭数据库或取消下载。
- **Desktop**：`app/desktop/.../Main.kt` 在 composition 之外创建服务，窗口关闭时 close。
- `MiyomuGraph` 实现 `Closeable`：自身拥有的 updater scope 与 danmaku HTTP client 在 close() 中释放；
  `MiyomuApp` 以 `DisposableEffect(graph)` 保证 graph 重建（SAF 切换）时旧 graph 被关闭——
  修复审阅指出的"graph 重建泄漏 scope/client"。
- `MiyomuApplication` 组合根只保留 UI 环境参数（`MiyomuAppEnvironment` 移除 `databaseFactory`）。

### AR-03 模块边界与 `api`/`implementation` 一致化（P1）

- `shared/ui-core`：`MiyomuGraph` 公开签名涉及的依赖全部升为 `api`（model、contract、domain、
  local、novel、readerNovel、sourceRepo、sourceManager、download、danmaku、sourceNovel）。
- 删除 `ui-core` 对 `i18n`/`i18n-aniyomi` 的再导出；`presentation` 保留为 `api`，但原因已注明：
  `MiyomuGraph.readerPreferences`（Compose 适配的偏好组，仍在 presentation）属于公开签名——
  这是签名驱动的 api，而非整体再导出。
- 7 个 `ui-*` feature 与 `ui` 模块改为**显式声明**各自实际 import 的 `presentation` /
  `presentation-core` / `i18n`，不再依赖 `ui-core` 的偶然传递暴露。

### AR-04 `domain` 去 Compose 化（P1）

- `eu.kanade.core.preference`（`PreferenceMutableState`、`asToggleableState`）迁至
  `shared/presentation-core`（包名不变，调用方 import 不变）。
- `Manga`/`Anime` 移除 `@Immutable` 与 Compose import（数据类 + strong skipping 已保证稳定性）。
- `Category` 从 `tachiyomi.domain.category.model` 迁至 `app.miyomu.model`（15 个引用文件同步更新）。
- `shared/domain` 构建由 `miyomu.kmp-compose` 改回 `miyomu.kmp-library`，新增 `api(model)`；
  `shared/contract` 断开 `api(domain)`，仅依赖 model。
- 结果：`grep androidx.compose shared/domain` 为 0；domain 恢复平台无关。

### AR-06 移除 Voyager 残留（P1）

- `MangaScreenModel` 去掉 `ScreenModel` 继承（保留其 State/Dialog 类型，仍被 4 个模块使用）。
- `MangaLibrarySettingsScreenModel` / `ReaderSettingsScreenModel` 改为构造注入 `CoroutineScope`，
  两个调用点传 composition scope；删除 `ScreenModel.ioCoroutineScope` 全局扩展。
- `presentation` 移除 `api(voyager.screenmodel)`；版本目录删除全部 voyager 条目。仓库内已无 voyager 引用。

### AR-08 主题系统去重（P2）

- 运行时主题（`MiyomuTheme` + `AppThemeColors` + 20 套 palette + `MonetColorScheme` expect/actual）
  从 `ui-core` 迁至 `shared/presentation-core`（包名不变），成为唯一设计系统主题。
- 删除 `shared/presentation/.../theme/`（`TachiyomiTheme` + 20 套重复 colorScheme，约 1.6k 行）；
  新增 `MiyomuPreviewTheme` 薄包装，10 个 preview 文件机械替换。

### AR-14 依赖清理（P3）

- `ui-core` 移除未使用的 `readerComic`/`player`/`data`（`data` 降为 `jvmTest`，仅 `LibraryUpdaterTest` 使用）。
- 版本目录移除 voyager 四个条目与版本号。
- 注：审阅报告 AR-14 提到的 `source-novel` "重复 quickjs" 实为 `jvmMain`/`androidMain` 两个源集各需一份，
  属 AR-11 的 hierarchy template 议题，未改动。

### AR-12 CI 质量门禁（P3）

- `.github/workflows/ci.yml`：Android job 增加 `:app:android:lintDebug`（本地实测通过）；
  新增周计划 `canary` job（`-Pcanary :shared:source-runtime:jvmTest`）+ workflow_dispatch。
- 顺带修复遗留损坏测试：`OmofunSourceTest` 依赖的 `demo-resources/` 夹具已在 edaf960 删除，
  该测试自那时起必然失败；按项目既有 canary 约定打 `@Tag("canary")`（还原夹具后可用 `-Pcanary` 运行）。

---

## 1b. P1 推进记录（2026-10-31 第二轮）

在 P0 达成后，同日继续推进 `product-goals.md` §5.2 的 P1 项（不依赖产品决策的先行）：

### P1-1 本地格式增强（完成）

- 审计发现 FB2/HTML/MARKDOWN 解析器此前已入库（扫描/分发/测试齐全，文档未更新）。
- 新增 **MOBI** 解析（`MobiNovelParser`）：PDB/MOBI 头解析、PalmDOC LZ77 解压（字面量/转义/
  空格对/回引用）、尾随 extra-data 裁剪（KindleUnpack 语义）、UTF-8 与 cp1252 编码、复用
  `HtmlNovelParser` 抽章；DRM/HUFF-CDIC/KF8(AZW3) 拒绝并给出可操作错误。6 个新测试
  （fixture 构造器在测试内组装合法 MOBI 文件）。AZW3 进书架但解析给出转换引导。

### P1-2 小说离线下载（核心完成）

- `NovelChapterCache` 端口（shared/novel）；`FileDownloadStore` 实现之（chapter.txt + .complete，
  沿用哈希目录布局）。
- `LegadoContentSource` 离线优先：目录与正文读写穿透缓存；缓存命中不触网——"缓存全本"与
  断网续读同一路径。
- 在线详情页"缓存全本"：顺序预取全章节（逐章跳过已缓存），进度显示 + 二次点击取消。
- 测试：store 契约 3 个 + 缓存行为 2 个（含"清空网络 fixture 后新实例离线可读"）。

### P1-3 批量元数据（基础完成）

- 书架多选新增"编辑信息"：`BulkMetadataEditDialog` 批量覆盖作者/简介（留空清除），经
  `LibraryRepository.upsert` 逐项落库。

### P1-4 书库健康与批量修复（基础完成）

- 库体检区新增"重新扫描并清理缺失条目"（确认对话框 + 状态反馈），双书架从磁盘重扫后
  失效条目自动移除；重复项仍走逐书删除。

### P1-8 阅读统计与回顾（完成）

- `ReadingStatsRecorder`（ui-core）：日粒度 JSON 偏好存储（零 Room 迁移）、90 天保留、
  连续阅读天数；6 个测试。
- 双阅读器会话记录（`DisposableEffect` 退出时累计，单会话 2 小时上限）。
- "阅读统计"页（更多页入口 + 新 `ReadingStatsKey` 路由）：近 7/30 天合计与日均、连续天数、逐日列表。

### P1 第三轮（系统集成 / 追踪 / 阅读增强 / 统一下载中心）

- **P1-6 系统集成（最小切片）**：Android `ACTION_VIEW` 打开本地书——manifest 增加 VIEW
  intent-filter（zip/cbz/7z/pdf/epub/txt/html 等 MIME），MainActivity 在首帧前把外部文件
  复制入库根（按扩展名分书架、重名避让），`MiyomuAppEnvironment` 新增 `startBookLocation/
  startBookMediaType`，应用壳据此以详情页为起始目的地；桌面 `main(args)` 支持
  `miyomu <file>`（库内直达、库外复制导入）。
- **P1-2 统一下载中心**：`FileDownloadStore` 新增书目命名（`.name`）与盘点
  （`novelCachedBooks()`，章节数/标题），预取时登记可读标题；下载中心页新增"小说离线缓存"
  分区（清单 + 逐书清除），与漫画任务同一页面呈现。
- **P1-9 追踪（客户端 + 默认关闭）**：新模块 `shared/track`——`AniListClient`（GraphQL：
  Viewer/搜索/进度更新）与 `BangumiClient`（API v0：me/书目搜索/收藏进度），统一
  `TrackException` 错误域；8 个 mock 引擎测试覆盖请求形态、认证头、错误路径。"追踪"设置页
  （更多页入口 + `TrackSettingsKey` 路由）：默认关闭，开启后才携带令牌请求，令牌验证显示
  账号名。逐书绑定 UI（`TrackBinding` 模型已定义）为后续切片。
- **P1-7 阅读增强（最小切片）**：小说朗读——`NovelTts` expect/actual（Android
  `TextToSpeech`，初始化异步就绪；桌面返回 null 时隐藏入口），阅读器 chrome 朗读/停止切换，
  章节文本变化续读、退出即停。跨章自动连读、批注/高亮、词典/翻译为后续。

### P1 第四轮（逐书绑定 / 重复项合并）

- **P1-9 逐书绑定与进度推送**：`TrackBindingStore`（JSON 偏好持久化，读写注入便于测试，
  5 个测试）+ `TrackBindingDialog`（ui-core）：站点切换、按书名搜索结果列表、一键绑定、
  解除绑定、"推送进度 N"（漫画详情页已读章节数 → AniList progress / Bangumi ep_status）；
  接入移植版详情页的既有 `onTrackingClicked` 钩子（追踪图标常显）。
- **P1-4 重复项合并**：库体检重复组新增"合并"动作——对话框列出组内副本（路径/类型）、
  单选保留项，确认后其余副本经仓库删除（漫画 `LocalComicRepository.delete`、小说新增
  `LocalNovelRepository.delete`）并重扫双书架。
- **P1-9 小说详情页绑定入口（第五轮补齐）**：`NovelDetailScreen` 的 `onTrackingClicked`
  钩子接入同一 `TrackBindingDialog`，推送进度取"当前章之前的章数"（与该页 `read` 标记语义
  一致）。至此漫画与小说两类详情页均可逐书绑定与推送。
- **P1-7 跨章自动连读（第六轮补齐）**：`NovelTts.speak` 增加 `onDone` 回调（Android
  `UtteranceProgressListener`，回调跳主线程）；朗读自然完成后自动进入下一章，末章自动停止。
  手动停止/退出保持既有语义。
- **视频按集下载**：评估后暂缓——在线视频流以 HLS/DASH 为主，真实现需分段拉取与混流
  （独立工程域）；MP4 直链限定的最小切片价值有限，按文档纪律单独立项。

### P1 第七轮（损坏文件扫描 / 绑定管理列表）

- **P1-4 损坏文件扫描**：`LocalLibraryMaintenance.audit` 新增格式探针——ZIP/CBZ/EPUB 开 ZIP、
  7z/CB7 开 SevenZFile、PDF 开平台渲染源、纯文本仅可读性检查；损坏条目进入
  `LibraryHealth.corruptBooks` 并在库体检区列出。新增 1 个测试（坏 zip/坏 7z 命中、好 zip
  与文本书通过）。
- **P1-9 绑定管理列表**："追踪"设置页列出全部手工绑定（条目名/站点/进度），逐条解绑。

### P1 第八轮（分享动作）

- **P1-6 Android 分享动作**：`rememberBookSharer` expect/actual（ui-core 共享层，遵守
  feature 不互依红线）——Android 经 FileProvider + ACTION_SEND 分享书目文件（仅
  filesDir/library 内可分享，SAF 根书目静默跳过），桌面无分享面板返回 null 隐藏入口；
  漫画与小说详情页接入移植版 `MangaScreen` 的 `onShareClicked` 钩子，MIME 按格式映射。
  Manifest 注册 FileProvider（authorities `${applicationId}.files`）与 file_paths 资源。

### P1 第九轮（标签字段）

- **P1-3 标签字段**：`LibraryItem.tags`（JSON 数组序列化）+ `LibraryItemEntity.tags` 列 +
  Room **v4→v5 迁移**（ALTER ADD COLUMN DEFAULT '[]'）+ 备份模型兼容（默认空数组，旧备份
  可读）+ 批量编辑对话框标签输入（逗号分隔）+ 仓库往返测试。P1-3 全部子项完成。

### P1 第十轮（OPDS 导出切片）

- **P1-6 OPDS 导出**：`OpdsCatalog`（shared/local）——OPDS 1.2 acquisition feed 生成
  （XML 转义、ISO 时间戳、按格式映射 MIME、文件名净化）+ `export()` 复制书目到
  `<libraryRoot>/.opds-export/`（点前缀目录被扫描器忽略，导出不回流入库）；库体检区
  "导出 OPDS 目录"入口（确认对话框 + IO 调度 + 状态反馈）。4 个测试（转义、空库、
  复制布局、文件名净化）。剩余：OPDS 导入（目录作为源）、网络服务化（Ktor server）。

### P1 第十一轮（OPDS 导入）

- **P1-6 OPDS 导入**：`OpdsCatalog.import` 解析本模块生成的 catalog（href 提取），把
  `.opds-export/books/` 的书目复制回书库根（冲突加 " (n)" 后缀、不覆盖既有文件）并触发
  重扫；库体检区"从 .opds-export 导入"入口。2 个测试（恢复 + 冲突避免 + 缺失目录零导入）。
  导出 → 转移 → 导入的离线转移闭环成立。

### P1 第十二轮（OPDS 网络服务化）

- **P1-6 OPDS 服务化**：`startOpdsServer`（shared/local，Ktor CIO 嵌入式）——`/catalog.xml`
  按请求实时重建 feed（新导出无需重启）、`/books/<file>` 下载（canonical 路径守卫阻断
  穿越）、`/` 重定向；`lanAddress()` 供 UI 显示局域网地址；设置页启停（离开页面即停，
  DisposableEffect 收口），端口占用给出可见错误。真实 socket 测试覆盖 feed、下载与穿越
  防护（port=0 随机端口）。**P1-6 的会话内可行子项至此全部完成**（余外部 feed 源接入，
  属源体系扩展）。

### P1 第十三轮（外部 OPDS feed 接入）

- **P1-6 外部 feed 源**：`fetchOpdsFeed`（条目解析：title/acquisition link/相对 href 经
  URI resolve、XML 实体反转义）+ `downloadOpdsBook`（ByteReadChannel 流式写盘、冲突避免、
  扩展名截断防护）；数据与存储页"从 OPDS feed 导入"对话框（URL 输入 → 获取条目 →
  下载全部 → 重扫）。3 个 mock 引擎测试（解析/相对解析/流式下载与冲突/错误路径）。
  **P1-6 的会话内可行子项至此全部完成**。

### P1 第十四轮（高亮/批注最小切片）

- **P1-7 高亮/批注**：`NovelHighlightStore`（reader-novel，镜像书签的偏好存储，5 个测试）+
  `NovelHighlight`（章节内字符区间 + 批注）；滚动模式段落按保存区间渲染琥珀背景 span，
  长按段落打开标注对话框（添加/编辑批注/删除，同段落起点覆盖保存）；漫画分页画布模式与
  拖拽手柄精调为后续。

### P1 其余项状态

### P1 第十五轮（外部 feed 分页与认证）

- **P1-6 外部 feed 分页/认证**：`fetchOpdsFeed` 跟踪 `rel="next"` 分页链接（页数预算防失控）
  并支持可选 HTTP Basic 凭据（feed 与下载同凭据，`OpdsCredentials` 显式类型）；2 个新测试
  （跨页累积与请求序列、Basic 头正确性）。**外部 feed 切片至此完整**（抓取/分页/认证/下载）。

### P1 第十六轮（分页模式高亮）

- **P1-7 分页模式高亮**：分页测量管线（`measureChapter`）接受高亮列表，文本块测量改用
  带琥珀背景 span 的 AnnotatedString（既有 `drawText(textLayoutResult)` 自动渲染背景，
  无需改绘制层）；标注助手收敛为单函数 `annotateWithHighlights`（滚动/分页共用），
  `produceState` 键入高亮版本号使保存后立即重排版。长按标注仍在滚动模式。

### P1 第十七轮（封面候选）

- **P1-1 封面候选**：`localCoverCandidates`（目录漫画顶层图片枚举）+ `saveCoverOverride`/
  `localCoverOverrideBytes`（偏好存储的书相对路径覆盖，缺失文件降级 null）——MiyomuApp 的
  封面管线优先使用用户选择；漫画详情页点击封面打开候选网格（Coil AsyncImage）点选即存。
  5 个测试（枚举、单文件无候选、覆盖解析、缺失降级、未设置）。**P1-1 的会话内可行子项
  （封面候选）完成**（源端多封面随源体系立项）。

### P1 其余项状态

- **P1-5 同步增强**：按产品决策等待 Q7（同步范围）确认——已十六轮向用户提出结构化问题，
  均未获回答，维持待决。

### P1 轮验证证据

| 命令 | 结果 |
|---|---|
| `./gradlew :shared:novel:jvmTest` | exit 0（FB2/HTML/MD/MOBI 全套） |
| `./gradlew :shared:download:jvmTest :shared:source-novel:jvmTest` | exit 0 |
| `./gradlew :shared:ui-core:jvmTest` | exit 0（含 ReadingStatsRecorder 6 测试） |
| `./gradlew jvmTest :app:desktop:compileKotlin :app:android:assembleDebug` | exit 0（最终门禁） |
| `./gradlew :shared:track:jvmTest` | exit 0（AniList/Bangumi 客户端 8 用例 + TrackBindingStore 5 用例） |

## 2. 验证证据（gradle-run wrapper，2026-10-31）

| 命令 | 结果 |
|---|---|
| `./gradlew jvmTest` | exit 0（修复 3 处编译错误与 1 个遗留损坏测试后全绿） |
| `./gradlew :app:desktop:compileKotlin :app:android:assembleDebug` | exit 0 |
| `./gradlew :app:android:lintDebug` | exit 0（40s，可安全作为 CI 门禁） |
| `./gradlew :shared:ui-core:jvmTest` | exit 0（含新增 back-stack 测试） |

过程中发现并修正的编译错误：`ReaderPreferences` 实际位于 presentation（审阅报告误记为 domain，
因依赖 Compose `BlendMode` 不能迁入已去 Compose 的 domain）；`monetColorScheme` expect/actual
需随主题一同迁移；Compose Multiplatform 1.12 的 `rememberSaveable(stateSaver=)` 重载要求
`MutableState` 形式。

---

## 3. 剩余债务（对应审阅报告"中期"档，未在本轮实施）

- **AR-07/AR-10**：包根分裂与 split package（`ported/` 边界、namespace 统一）。
- **AR-09**：`presentation` 巨石拆分；`ui-reader`→`ui-entries`/`ui-library` 的 feature 间依赖
  （mapper 下沉 `ui-core`）。
- **AR-11**：Convention Plugins API 面收窄（compose 图标按需、hierarchy template 表达 `jvmShared`）。
- **AR-13**：测试布局倒挂（`commonTest` 迁移、Android 冒烟测试、presentation 零测试）。
- **AR-05 后续**：`MiyomuGraph` 拆窄端口（`LibraryPort` 等）、`MiyomuApp` 18 参数签名收敛；
  `ReaderPreferences` 重新归属以解除 `ui-core → presentation` 的最后一根签名依赖。
- 架构规则测试（Konsist/ArchUnit）尚未引入，`domain → Compose` 目前靠约定约束。

---

## 4. 涉及文件总览

- 构建：`shared/{ui-core,ui,domain,contract,presentation,model,application}/build.gradle.kts`、
  `shared/ui-{entries,library,novel,reader,sources,settings}/build.gradle.kts`、`gradle/libs.versions.toml`
- 导航：`shared/ui-core/.../navigation/MiyomuNavigator.kt`（+ 新测试）
- 组合根：`shared/application/.../MiyomuServices.kt`（新）、`MiyomuApplication.kt`
- App 壳：`shared/ui/.../MiyomuApp.kt`、`home/HomeScreen.kt`
- 平台入口：`app/android/.../{MainActivity,MiyomoAndroidApp}.kt`（MiyomuAndroidApp 为新）、
  `AndroidManifest.xml`、`app/desktop/.../Main.kt`
- 图与 ScreenModel：`shared/ui-core/.../MiyomuGraph.kt`、`shared/presentation/.../{MangaScreenModel,
  MangaLibrarySettingsScreenModel,ReaderSettingsScreenModel,util/Navigator}.kt` 及 3 个调用点
- 主题：`shared/presentation-core/.../app/miyomu/ui/`（迁入）、`shared/presentation/.../theme/`（删除）、10 个 preview 文件
- 领域：`shared/model/.../Category.kt`（新）、`shared/domain`（删 2 类文件 + 去 @Immutable）、15 个 Category 引用文件
- CI：`.github/workflows/ci.yml`；测试标签：`shared/source-runtime/.../OmofunSourceTest.kt`
