# 03 · 功能 Parity 矩阵

> **方向修正（2026-10-31）：** 本文改为“功能照搬优先”的 parity 矩阵。当前权威迁移方案见 [07-架构优先方案](07-architecture-first.md)。
>
> 原则：**核心功能不主动裁剪。** 先复用 Kototoro 的实现，再谈优化；`later` 只表示排期，不表示放弃。

## 1. 动作定义

| 动作 | 含义 |
| --- | --- |
| **copy** | 直接复制 Kototoro 代码/模块，改包名/装配 |
| **adapt** | 复制核心逻辑，改接口/平台依赖 |
| **rewrite UI** | 复用数据/规则，按 MD3E 重写页面 |
| **runtime** | 通过依赖/宿主运行，不复制源码 |
| **later** | parity 之后再做，不删除能力 |

## 2. 核心阅读闭环

| 功能 | Kototoro 参考 | 目标模块 | 动作 | 阶段 | 验收 |
| --- | --- | --- | --- | --- | --- |
| SourceRuntime 契约 | `core-source/` | `core/source` | copy | P1 | 原契约单测通过 |
| 源注册/启停/排序 | `ContentSourcesRepository` | `core/source` + `feature/extensions` | adapt + rewrite UI | P1/P4 | 启停持久化、排序生效 |
| 浏览/分页 | `MangaRepository.getList` | `core/source` | copy/adapt | P1 | OFFSET/PAGE_INDEX 都可 |
| 搜索 | `search/` | `feature/discover` | adapt + rewrite UI | P3 | 单源/全局搜索 |
| 筛选/动态筛选 | `filter/` | `feature/discover` | adapt + rewrite UI | P3 | 静态+动态筛选树 |
| 详情/章节 | `details/` | `feature/discover` | adapt + rewrite UI | P3 | 详情/章节/相关 |
| 收藏/分类 | `favourites/` | `feature/library` | adapt + rewrite UI | P3 | 多对多分类/置顶 |
| 历史/进度 | `history/` | `core/data` + `feature/library` | copy/adapt | P2/P3 | 位置恢复误差 < 1 页 |
| 更新/订阅 | `tracker/` | `core/data` + `feature/library` | copy/adapt | P2/P3 | 发现/已查看/已读完语义 |
| 书签/笔记 | `bookmarks/`、`notes/` | `core/data` + `feature/library` | copy/adapt | P2/P3 | 精确位置/摘录 |
| 下载/离线 | `download/` | `feature/downloads` | adapt + rewrite UI | P3 | 暂停/继续/重启恢复 |
| CBZ 导出 | `CbzOutput` | `feature/downloads` | copy/adapt | P3 | 逐页校验 |
| 本地导入 | `local/` | `core/data` | copy/adapt | P2 | CBZ/ZIP/EPUB/TXT/视频 |
| 备份/恢复 | `core-backup` | `feature/sync` | copy/adapt | P3 | 导出→清库→恢复 |
| WebDAV | `sync/`、`core-net` | `feature/sync` | copy/adapt | P3 | 上传/下载/合并 |
| 阅读统计 | `stats/` | `core/data` + `feature/library` | copy/adapt | P4 | session/看板 |
| 迁移/换源 | `migration/` | `core/data` | copy/adapt | P4 | 进度/分类迁移 |

## 3. 阅读器

| 功能 | Kototoro 参考 | 目标模块 | 动作 | 阶段 | 验收 |
| --- | --- | --- | --- | --- | --- |
| 场景几何 | `reader-core` | `core/reader` | copy | P1 | 原测试通过 |
| 单页/双页/RTL | `PagedReaderScene` | `core/reader` | copy | P1 | 翻页/跨章正确 |
| 纵向/条漫/横向 | `VerticalReaderScene`/`HorizontalReaderScene` | `core/reader` | copy | P1 | 长条漫不 OOM |
| 自动模式检测 | `ReaderModeDetection` | `core/reader` | copy | P1 | 30% 规则 |
| 缩放/拖动/双击 | `PagedCameraTransform` | `core/reader` + `feature/reader` | copy/adapt | P2 | 边界/回弹 |
| 裁边/宽图独占 | `WidePagePolicy` | `core/reader` | copy | P2 | 页面不拉伸 |
| 预取/分块 | `PagedSceneResourceWindowStrategy`/`TileGrid` | `core/reader` | copy/adapt | P2 | 预取命中/内存预算 |
| 阅读器 chrome | `ReaderChrome` | `feature/reader` | rewrite UI | P5 | MD3E 浮动工具栏 |
| 章节/选项面板 | `ComposeReaderOptionsSheet` | `feature/reader` | rewrite UI | P5 | 模式/字号/方向 |
| 点击网格/键盘/鼠标 | `TapGrid`/`DesktopReader` | `feature/reader` | adapt + rewrite UI | P5 | 三端输入 |
| 自动滚动/电子墨水 | `ReaderAutoScroll`/`eink` | `feature/reader` | adapt | P6 | 可选 |
| 小说阅读 | `reader/novel` | `core/reader` + `feature/reader` | copy/adapt + rewrite UI | P3/P5 | 分页/连续/书签 |
| EPUB/TXT | `local/epub` | `core/data` | copy/adapt | P2 | 解析/排版 |
| TTS | `reader/novel/tts` | `feature/reader` | adapt | P6 | 平台 TTS |

## 4. 视频

| 功能 | Kototoro 参考 | 目标模块 | 动作 | 阶段 | 验收 |
| --- | --- | --- | --- | --- | --- |
| 播放器抽象 | `video/` | `feature/video` | adapt + rewrite UI | P4/P5 | 平台引擎可换 |
| HLS/DASH/字幕/音轨 | Media3/libmpv/AVPlayer | `feature/video` | adapt | P4 | 轨道选择 |
| 手势/进度/PiP | `VideoPlayerControls` | `feature/video` | adapt + rewrite UI | P5 | 三端一致 |
| 视频下载 | `VideoDownload` | `feature/downloads` | adapt | P5 | 队列/离线 |
| 外挂字幕 | `externalSubtitleTracks` | `feature/video` | adapt | P5 | 文件选择 |
| 超分 Anime4K/NCNN | `ncnn` | `feature/video` | later | P6 | GPU 降级 |
| DLNA/弹幕 | `dlna`/`danmaku` | `feature/video` | later | P6 | 可选 |

## 5. 源生态

| 生态 | Kototoro 参考 | 目标模块 | 动作 | 阶段 | 验收 |
| --- | --- | --- | --- | --- | --- |
| 原生 JSON/CSS/JS | `core/parser` | `core/source` | copy/adapt | P1 | 浏览/详情/页面 |
| Mihon APK | `mihon/` | `core/source` + `feature/extensions` | runtime | P4 | Android 进程内 |
| Mihon JAR | `source-host/` | `source/host` | copy | P4 | Desktop 独立进程 |
| Aniyomi | `aniyomi/` | `feature/extensions` + `source/host` | runtime | P4 | 视频源 |
| Tsundoku/IReader | `tsundoku/`/`ireader/` | `source/host` | runtime | P4 | 小说源 |
| Legado JSON | `core/parser` | `core/source` | adapt | P4 | 规则解析 |
| TVBox/苹果 CMS | `source-tvbox/` | `core/source` | copy/adapt | P4 | type 0/1 |
| CloudStream | `cloudstream-*` | `source/host` | runtime | P5 | 插件运行 |
| 扩展仓库/安装 | `extensions/` | `feature/extensions` | adapt + rewrite UI | P4/P5 | 安装/更新/启用 |
| 源测试/诊断 | `SourceTester` | `feature/extensions` | adapt + rewrite UI | P4 | PASS/FAIL/SKIP |
| Cloudflare 浏览器 | `browser/`、`core-cloudflare` | `core/network` + platform | adapt | P4 | 挑战/ Cookie |

## 6. 增强能力

| 功能 | Kototoro 参考 | 目标 | 动作 | 阶段 |
| --- | --- | --- | --- | --- |
| OCR | `reader/translate` | `feature/reader` | later | P6 |
| 漫画翻译 | `reader/translate` | `feature/reader` | later | P6 |
| 小说翻译 | `reader/novel` | `feature/reader` | later | P6 |
| 词库/替换规则 | `dictionary`/`replace` | `core/data` | adapt | P6 |
| 视频超分 | NCNN/Anime4K | `feature/video` | later | P6 |
| 追踪站点 | `tracker/`/`tracking` | `feature/library` | adapt | P6 |
| 自定义空间 | `space/` | `feature/library` | adapt | P6 |
| 统计看板 | `stats/` | `feature/library` | adapt | P5 |
| 桌面小组件 | `widget/` | platform | later | P6 |
| 自建 OTA | `incremental-updates` | platform | later | P6 |

## 7. UI / 平台

| 功能 | Kototoro 参考 | 目标 | 动作 | 阶段 |
| --- | --- | --- | --- | --- |
| 主题/视觉 | `core-ui` | `core/design` | rewrite MD3E | P0/P5 |
| 自适应导航 | `adaptive/` | `core/ui` | rewrite MD3E | P0/P5 |
| 桌面壳/菜单 | `desktop-app` | `app/desktop` | adapt + rewrite UI | P3/P5 |
| 桌面播放器 | `desktop-player` | `app/desktop` | copy/adapt | P4 |
| iOS 壳 | `iosApp` | `app/ios` | adapt + rewrite UI | P4/P5 |
| 设置 | `settings/` | `feature/settings` | rewrite UI | P5 |
| 下载页 | `download/ui` | `feature/downloads` | rewrite UI | P5 |
| 备份/同步页 | `backups/`/`sync/` | `feature/sync` | rewrite UI | P5 |
| 扩展管理 | `extensions/ui` | `feature/extensions` | rewrite UI | P5 |
| 通知/分享/深链 | `widget/`/`main` | platform | adapt | P5 |
| 无障碍/本地化 | `values-*` | all | adapt | P5/P6 |

## 8. 版本与阶段

| 阶段 | 目标 | 功能范围 |
| --- | --- | --- |
| P0 | 基线/骨架 | 三端壳、MD3E token、CI、许可证 |
| P1 | 纯核心 | reader-core、core-source、core-net、core-backup、core-domain |
| P2 | 数据库/仓库 | Room、收藏/历史/进度/本地/备份数据 |
| P3 | Feature 逻辑 | library/discover/reader/downloads/sync 的数据与状态 |
| P4 | 源/扩展 | Mihon/Aniyomi/Tsundoku/TVBox/CloudStream、源测试 |
| P5 | MD3E UI | 全量页面替换、桌面/平板/iOS 适配 |
| P6 | Parity/增强 | OCR/翻译/超分/追踪/空间/统计/小组件 |

## 9. Parity 验收

一个功能算“完成”，必须同时满足：

- [ ] 用户可见行为与 Kototoro 基线一致（差异有记录）。
- [ ] 数据可读写，重启/离线/同步后不丢。
- [ ] 关键路径有测试（单测/集成/查询计数）。
- [ ] 新 UI 使用 MD3E 组件，不是旧页面换色。
- [ ] 三端中承诺的平台都可用；不可用有明确空态。
- [ ] 不新增 `app` 级耦合；代码在目标模块内。

## 10. 结论

功能策略一句话：

> **先把 Kototoro 的核心功能搬进新架构，再用 MD3E 把 UI 全部重写；不靠删功能来换“精简”。**

“精简”指的是目录、边界、重复实现和平台分叉，不是用户能力。
