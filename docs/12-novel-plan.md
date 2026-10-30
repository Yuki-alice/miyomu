# 小说端开发计划(2026-10-29)

- 状态:**N1–N3 完成 + 插画/格式/legado 在线阅读闭环**(2026-10-30,`feature/novel` 分支);后续增强见第 7 节
- 决策人:用户(2026-10-29 会话确认)
- 关联:[00 产品简报](00-product-brief.md) · [M1 清单](checklists/phase-1-m1.md)(N1–N3 条目)· [09 交接文档](09-handoff-2026-10-29.md)

## 1. 决策记录:推翻「小说暂缓」

`docs/09-handoff-2026-10-29.md` 与 ADR-0011 记录的「小说(novel)全部暂缓」是**单设备串行**工作方式下的排序决定。2026-10-29 起改为**双设备并行**:

| 设备 | 范围 | 分支 |
|---|---|---|
| 设备 A(本机) | 小说端:解析层、小说阅读器、库/详情接线 | `feature/novel` |
| 设备 B | KMP 版 Aniyomi 继续:Phase B(anime + mediamp)、后续 C | 主线 + 各自 feature 分支 |

分支合并规则:

- 冲突热点只有 `settings.gradle.kts`、`MiyomuGraph.kt` / `MiyomuApp.kt` / `home/*`(各自加 Tab/路由)、`.github/workflows/ci.yml`、`NOTICE`、`docs/README.md`;约定**全部追加式修改**,冲突手工合并。
- 小说端不修改 `shared/presentation`、`shared/reader-comic`、`ComicReaderScreen`、`shared/local` 的既有文件;只新增 `shared/novel*` 模块与 `app.miyomu.ui.novel` 包,以及复用(不修改)已有类型。
- 推送前 `git pull --rebase`;合并顺序:manga 设备先入 main,novel rebase。
- 设备 B 的 agent 见到本文件后,不再把「小说」当作全项目禁区,但仍**不实现**小说功能(归设备 A)。

## 2. 设计参考与许可边界(2026-10-29 调研)

结论先行:**GPL 项目一律只参考设计与交互,不进代码**;代码复用主体是已移植的 Aniyomi(Apache-2.0)组件。

| 项目 | 许可 | 用途 |
|---|---|---|
| [NovelDokusha](https://github.com/nanihadesuka/NovelDokusha) | GPL-3.0 | 信息架构蓝图:Tachiyomi 式小说阅读器(书架/章节列表/下载标记/源模型) |
| [Book's Story](https://github.com/Acclorite/book-story) | GPL-3.0 | 排版与阅读器设置面板设计(字号/行距/边距/主题的分组呈现) |
| [legado-with-MD3](https://github.com/HapeLee/legado-with-MD3) | GPL-3.0 | 中文网文排版惯例(段首缩进、章节间距)、章节标题正则的**行为**参考 |
| [Myne](https://github.com/Pool-Of-Tears/Myne) | Apache-2.0 | EPUB 阅读实现,可复用代码(记 NOTICE) |
| [material-3-expressive-catalog](https://github.com/meticha/material-3-expressive-catalog) | Apache-2.0 | MD3E 组件参照,可复用代码(记 NOTICE) |

仓库已浅克隆到 `reference/`(不入库);许可表见 [07](07-skills-and-references.md) 与 `AGENTS.md`。

## 3. 数据层盘点结论(N0,已核实)

- `MediaType.NOVEL` 已存在(`shared/model/MediaType.kt`),`LibraryItem.mediaType` 可直接存小说。
- 进度模型无需改表:`ReadingProgress.entryId` = 章节稳定 id(书 location + 章节序号 hash,与漫画 A2-4 的手法同源),`position: Double` = 章节内**字符偏移**;`kind` 用 `PAGE`(分页模式)/`SCROLL`(滚动模式)。
- `LocalBookFormat` 已含 `EPUB`、`TXT`,但 `LocalLibraryScanner.bookOf()` 用 `isComicFormat()` 把它们过滤掉了——小说扫描器是**新增** `NovelLibraryScanner`(放 `shared/novel`),不改 `shared/local` 既有文件。
- `LocalFileRef.readBytes(): ByteArray` 已够 N1 用;TXT 大文件防护用**大小上限**(默认 128 MB,可配),真正的流式解析留待后续切片(接口形态已预留)。
- Ksoup 0.2.6 已在版本目录(`ksoup` / `ksoup-android`),EPUB 的 XHTML→文本用它,与 `source-runtime` 同款接线。

## 4. 架构:复用 A2 的「合成 Manga」手法

Aniyomi 没有小说 UI,不存在可移植的小说界面。但 A2-3/A2-4 已验证:**把自有数据合成 Aniyomi 的 `Manga`/`Chapter`,整套移植的库网格、详情头、阅读器 chrome 就能零成本复用**(参见 `MangaLibraryMapping.kt`)。小说端沿用同一手法:

```
shared/novel         解析层:TXT/EPUB → ParsedNovel(标题/作者/章节/文本)
shared/reader-novel  阅读状态:分页(TextMeasurer)/滚动/字符偏移进度(N2)
shared/ui/novel      适配层:Novel → 合成 Manga/Chapter → 复用库/详情 chrome;阅读器渲染层新写(N2/N3)
```

阅读器的**内容渲染**(分页、排版)是新代码,chrome(顶栏/底栏/设置对话框)尽量复用已移植组件;排版设置页设计参照 Book's Story。

## 5. 任务切片

| 切片 | 内容 | 验收 | 状态 |
|---|---|---|---|
| N0 | 决策记录(本文件)、参考仓库、数据层盘点 | 本文件入库;`reference/` 就位 | ✅ 2026-10-29 |
| N1 | `shared/novel`:TXT(UTF-8/BOM/GB18030 回退、中文章节正则、单章回退、128 MB 上限)+ EPUB(container→OPF spine→XHTML 文本、EPUB2/3 封面)+ `NovelLibraryScanner` | 19 jvmTest 全绿(fixture 代码内生成) | ✅ 2026-10-29 |
| N2 | `shared/reader-novel`(偏好/分页数学/控制器,9 测试)+ `ui/novel` 阅读器:分页(段落级 TextMeasurer 度量、超长段按行切片)/滚动双模式、章节导航、排版设置(字号/行距/边距/段距/四档主题)、字符偏移进度 | UI 测试:库→详情→阅读→翻页→进度保存→续读落页 | ✅ 2026-10-30 |
| N3 | `NovelLibraryRepository`、小说 Tab(Aniyomi anime/manga 双 Tab 结构)、库/详情接线(合成 Manga 复用移植网格/详情)、i18n(en/zh-rCN)、封面 fetcher 分流、证据截图、双端构建 | 全模块测试绿;桌面/Android 构建绿;`evidence/novel-*.png` | ✅ 2026-10-30 |

证据:`evidence/novel-library.png` · `novel-detail.png` · `novel-reader.png`(headless 桌面 UI 测试实拍)。

## 5.5 实施记录(2026-10-30 计划外决策)

- 分页实现从"整章一个 TextLayoutResult"改为**段落级度量**:每段单独 layout 后按高度堆叠成页,超长段落按自身行切片;数学在 `shared/reader-novel`(纯函数可测),绘制用 `drawText(topLeft)` 平移 + 裁剪。
- 阅读器 chrome 为小说专用(`NovelReaderChrome`),不复用漫画的 `ReaderAppBars`(页码滑条语义不同);视觉语言保持 M3 一致。
- 首页 Tab 结构对齐 Aniyomi 上游的 anime/manga 双 Tab:**小说 Tab 放首位**(对应 AnimeLibraryTab 位置),漫画测试相应加了切 Tab 步骤。
- 进度键沿用漫画 A2-4 手法:`chapterId(book, index) = "<location>#<index>".hashCode()`,详情页按序号反查;续读恢复走"页包含偏移"二分(`NovelChapterPages.pageIndexForOffset`)。
- 修复:章节 0 续读时 `remember(chapterIndex)` 不会重建导致偏移丢失,改为 `LaunchedEffect(state.text, chapterIndex)` 显式同步。

估算(设备 A,非全职):N1 约 2 周,N2 约 2.5–3 周,N3 约 1.5 周。

### 已知取舍(刻意,非缺陷)

- TXT 用整文件解码 + 大小上限,不做流式;>上限文件报错并提示,接口形态预留流式升级。
- EPUB v1 只取纯文本与封面;图片、脚注、CSS 排版后置(Alpha 范围一致)。
- 章节身份 = 书 location + 章节序号;文件内容变化(非重命名)导致的章节漂移不在 v1 处理。

## 6. 验证命令

```bash
./gradlew :shared:novel:jvmTest :shared:reader-novel:jvmTest :shared:ui:jvmTest
# 全量(含漫画侧,确认没碰坏)
./gradlew :shared:ui:jvmTest :shared:reader-comic:jvmTest :shared:presentation:compileKotlinJvm :desktopApp:compileKotlin :androidApp:assembleDebug
```

## 7. 后续增强(按对标 Google Play Books 排序;2026-10-30 对标调研见 [13](13-novel-benchmark.md))

1. ~~**目录/书签/书内搜索**~~ ✅ 2026-10-30(目录面板 + JSON 偏好书签 + 全书搜索)
2. ~~**护眼暖色滤镜 / 音量键翻页**~~ ✅ 2026-10-30
3. ~~**EPUB 插画渲染**~~ ✅ 2026-10-30(内容块架构 NovelBlock.Text/Image,分页/滚动双端渲染,尺寸嗅探)
4. ~~**更多文件格式**~~ ✅ 2026-10-30(FB2 含 base64 插画与封面、HTML、Markdown;TXT/EPUB 已有)
5. ~~**legado 书源引擎(读路径 + 应用内闭环)**~~ ✅ 2026-10-30(`shared:source-novel`:书源 JSON 导入、规则子集求值器、搜索/详情/目录/正文;`NovelContentSource` 抽象让阅读器同时服务本地与在线;浏览 Tab 书源管理/搜索/书架/在线阅读;20+ 测试)——**剩余**:发现/分类页、正文分页抓取(nextContentUrl)、在线书源的书内搜索
6. **书内长按与注释**:选中文本、高亮/笔记(需要 Room 新表,建议等分支合并后统一动 schema)。
7. **翻页动画**:覆盖/仿真 page delegate(平移已有)。
8. **自动翻页与亮度控制**(Android window attributes,expect/actual)。
9. **TTS 朗读**(M3 范围,依赖平台 TTS 抽象)。
10. **流式 TXT 解析**:替换整文件解码(>128 MB 文件当前拒读)。
