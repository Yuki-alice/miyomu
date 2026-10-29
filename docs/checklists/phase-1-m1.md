# Phase 1 M1 清单：本地漫画与小说阅读器

- 状态：**进行中**（2026-10-28）；仓库结构已标准化，M1 重排为表现层 + 本地阅读器
- 关联：[08 M1 任务重排](../08-m1-task-plan.md) · [ADR-0010](../adr/0010-aniyomi-local-reader-reuse.md) · [Phase 1 计划](../05-phase-1-plan.md)

> 源运行时“读路径垂直切片”已完成并冻结：JS 源、Ktor/Cookie/KV、双端调试壳和 Room 数据层保留，真实 Venera 源与源仓库移到 M2。

## 0. 本轮进度（2026-10-28）

- [x] `:shared:local`：`LocalFileRef`、目录/ZIP 扫描、自然排序、封面、目录/CBZ 页加载
- [x] `:shared:reader-comic`：阅读控制器、LTR/RTL/纵向模式、双页配对函数、进度映射
- [x] `:shared:ui`：主题、`MiyomuApp`、书架/详情/阅读器、翻页、续读
- [x] Aniyomi 视觉层：主题 token、Home 分栏、书架紧凑网格、详情头图、阅读器浮层
- [x] 三屏截图证据：`evidence/m1-library-anilook.png`、`m1-detail-anilook.png`、`m1-reader-anilook.png`
- [x] 根 `NOTICE` 记录 Aniyomi Apache-2.0 来源
- [x] Compose UI 流程测试：书架 → 详情 → 阅读；保存页续读
- [x] 桌面端实际启动并打开本地样例库；Android `assembleDebug` 通过
- [ ] 7Z/PDF/EPUB/TXT、Webtoon/连续、双页 UI、设置面板、SAF、小说阅读器

## 1. 已完成基线

- [x] 仓库根目录生产构建：KGP 2.3.21 + AGP 9.4.1 + Gradle 9.6 + compileSdk 37
- [x] `:shared:model`：MediaType、SourceDescriptor、ComicSummary/Details、Chapter、PageRef、Paged
- [x] `:shared:network`：Ktor 3.6 + OkHttp/CIO + 持久 Cookie（8/8 测试）
- [x] `:shared:source-api` / `:shared:source-runtime`：QuickJS 读路径、Ksoup DOM、源 KV、配额（5/5 测试）
- [x] `:shared:data`：Room 2.8.5 + KSP 2.3.12 + bundled SQLite；库/进度 repository（3/3 测试）
- [x] `:desktopApp` / `:androidApp`：双端调试壳跑通 fixture 搜索→详情→章节→图片
- [x] 证据：`evidence/m1-desktop-flow.png`、`m1-android-flow.png`、`m1-desktop-room.png`、`m1-android-room.png`

## 2. 共享 UI 与本地库

- [ ] **U1** `:shared:ui`：主题、通用组件、`MiyomuApp()`、`AppRoute` 导航；双端入口只做平台壳
- [ ] **U1** Library/Detail/Reader/Settings 四个路由与空/错/加载状态
- [ ] **L1** `LocalFileRef`：桌面 `Path`、Android SAF `Uri`；打开目录/文件与持久化权限
- [ ] **L1** 桌面文件选择器、Android `OpenDocumentTree`/`OpenDocument`
- [ ] **L2** 本地库扫描：目录/压缩包/PDF/EPUB 识别，自然排序，封面，`ComicInfo.xml`
- [ ] **L2** 本地书 ID（`sourceId = "local"`）与文件缺失/权限丢失错误状态

## 3. 本地漫画阅读器

- [ ] **C1** 页加载器：目录、ZIP/CBZ、7Z/CB7、PDF；页序/图片检测/损坏分支测试
- [ ] **C2** 阅读状态：章节切换、当前页、预取窗口、RTL、双页配对、进度映射
- [ ] **C3** 阅读 UI：LTR/RTL、纵向分页、Webtoon、连续纵向、双页、缩放/平移、点击区/键盘
- [ ] **C3** 顶栏/底栏、页码、章节导航、阅读设置（模式/方向/裁边/主题）
- [ ] **C4** 库 → 详情 → 阅读 → 续读闭环；Android + 桌面
- [ ] Aniyomi 对照：Library/Detail/Reader/Settings 信息层级与交互一致（允许像素差异）
- [ ] 许可：复制/移植文件保留来源头，根 `NOTICE` 记录 Aniyomi

## 4. 本地小说阅读器

> 2026-10-30：小说端改在 `feature/novel` 分支并行开发（[docs/10](../10-novel-plan.md)），N1–N3 已完成。

- [x] **N1** TXT 解析：UTF-8/BOM、GB18030 回退、章节正则/单章回退、文件大小上限（`shared/novel`，19 测试）
- [x] **N1** EPUB 解析：container.xml → OPF spine → XHTML 文本；章节/封面提取（Ksoup）
- [x] **N2** 阅读 UI：分页（段落级 TextMeasurer 度量 + 行切片）与滚动（`LazyColumn`）、章节导航（`shared/reader-novel` + `ui/novel`）
- [x] **N2** 排版：字号、行距、页边距、段落间距、四档主题；进度按字符偏移/章节保存（`JsonPreferenceStore` + Room 进度表）
- [x] **N3** 库 → 详情 → 阅读 → 续读闭环；小说 Tab + 合成 Manga 复用移植网格/详情；桌面验证（UI 测试 + 截图），Android 构建通过
- [x] 测试：自造 TXT/EPUB fixture 的章节数/文本/编码/偏移；不提交版权文本（`NovelLayoutTest`/`NovelFlowTest`/`shared:novel` 19 测试/`shared:reader-novel` 9 测试）

## 5. M1 验收

- [ ] 本地漫画（目录/ZIP/CBZ/7Z/CB7/PDF）在双端可导入、打开、阅读、续读
- [ ] 本地小说（TXT/EPUB）在双端可导入、打开、阅读、续读
- [ ] 双端构建与测试通过（见第 7 节）
- [ ] 1,000 项库、20+ 页长图、长篇小说无明显卡顿/OOM
- [ ] 截图/录屏证据写入 `evidence/`；Aniyomi 对照清单签字；`NOTICE` 完整
- [ ] 不用 fixture 或自造文件冒充真实用户内容；测试素材自造/公共领域

## 6. 移出 M1

- 真实 Venera 源验证、源兼容 harness（M2）
- 源仓库索引/安装/更新（M2）
- 在线源搜索/详情/阅读（M2）
- 下载队列、离线库（M2）
- WebDAV 同步、备份恢复（M3）
- 视频播放器、音轨/内嵌字幕轨道选择（M3）
- iOS App 壳与 AVKit（M1 之后）

## 7. 验证命令

```bash
./gradlew :shared:local:jvmTest
./gradlew :shared:reader-comic:jvmTest :shared:reader-novel:jvmTest
./gradlew :shared:library:jvmTest :shared:data:jvmTest
./gradlew :desktopApp:compileKotlin :androidApp:assembleDebug
```

本地素材放 `fixtures/local/`（gitignored）：

- 漫画：目录、CBZ、ZIP、7Z、PDF；页名含 `1.jpg`、`2.jpg`、`10.jpg`。
- 小说：UTF-8/GB18030 TXT、多章节 EPUB（含图片）。
- 手动流程：导入 → 库 → 详情 → 阅读 → 退出 → 续读。

## 8. 参考与许可

- Aniyomi（Apache-2.0）：`reference/aniyomi/source-local`、`core/archive`、`ui/reader`、`presentation/reader`、`presentation/library`
- 复用边界：[ADR-0010](../adr/0010-aniyomi-local-reader-reuse.md)
- 不复制 Venera-Next（GPL-3.0）代码；不复用 Aniyomi 的下载/追踪/扩展安装模块
- 复制/移植文件加来源头；根 `NOTICE` 列出 Aniyomi 与复用范围
