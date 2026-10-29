# 小说端开发计划(2026-10-29)

- 状态:**执行中**(`feature/novel` 分支;N1 进行中)
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
| N0 | 决策记录(本文件)、参考仓库、数据层盘点 | 本文件入库;`reference/` 就位 | ✅ |
| N1 | `shared/novel`:TXT(UTF-8/BOM/GB18030 回退、中文章节正则、单章回退、128 MB 上限)+ EPUB(container→OPF spine→XHTML 文本、封面提取)+ `NovelLibraryScanner` | jvmTest 全绿:编码/章节数/偏移/EPUB 结构(自造 fixture,不提交版权文本) | 🔄 |
| N2 | `shared/reader-novel`:分页(`TextMeasurer`)/滚动(`LazyColumn`)、章节导航、字符偏移进度、排版偏好(字号/行距/边距/主题,挂 `JsonPreferenceStore`) | 双端阅读器可翻页/续读;排版即时生效 | |
| N3 | 库/详情接线:`NovelLibraryScanner` 接入、合成 Manga 复用网格与详情、i18n(en/zh-rCN)、证据截图、NOTICE/README/M1 清单勾选 | 导入 → 库 → 详情 → 阅读 → 续读闭环(Android + 桌面) | |

估算(设备 A,非全职):N1 约 2 周,N2 约 2.5–3 周,N3 约 1.5 周。

### 已知取舍(刻意,非缺陷)

- TXT 用整文件解码 + 大小上限,不做流式;>上限文件报错并提示,接口形态预留流式升级。
- EPUB v1 只取纯文本与封面;图片、脚注、CSS 排版后置(Alpha 范围一致)。
- 章节身份 = 书 location + 章节序号;文件内容变化(非重命名)导致的章节漂移不在 v1 处理。

## 6. 验证命令(N1 起)

```bash
./gradlew :shared:novel:jvmTest
# 全量(含漫画侧,确认没碰坏)
./gradlew :shared:ui:jvmTest :shared:reader-comic:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug
```
