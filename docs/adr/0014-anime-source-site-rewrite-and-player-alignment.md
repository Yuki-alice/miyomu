# ADR-0014: anime 源站点重写策略与播放器对齐 Animeko

- 状态：**接受**（2026-10-30）
- 关联：[ADR-0011](0011-kmp-aniyomi-port.md)（KMP 移植主线）· [ADR-0008](0008-venera-source-runtime.md)（Venera 风格读路径）· [ADR-0013](0013-js-source-repository-format.md)（源仓库格式）· [10 anime 源与播放器计划](../10-anime-source-and-player-plan.md)

## 背景

Phase C 完成漫画在线阅读闭环（C1 源仓库安装 / C2 运行时接入）后，anime（视频）侧需要确定源生态路线。可选方案：

1. 仿 Aniyomi 定义完整 anime 扩展 API（表达力最强，但生态从零、与 Aniyomi 扩展模型相同重）；
2. 做导入兼容层：宿主化 Miru 的 JS 模块、解释 Kazumi 的 XPath 规则 JSON（直接继承现成生态，但长期背负两个外部格式的兼容责任）；
3. **站点重写**：把 Miru 与 Kazumi 覆盖的站点，用 Miyomu 自己的源格式逐站重写。目标是"支持这些站点"，不是"支持它们的格式"。

用户确认（2026-10-30）：**选方案 3**；播放器功能对齐 Animeko。

覆盖面实测（2026-10-30）：Miru 官方仓库 97 个视频源（bangumi 类型），KazumiRules 官方仓库 86 个站点规则；站点聚成三个技术家族（苹果CMS 采集站 / 中文动漫 HTML 站 / 欧美动漫站），两家重叠热点站约 8–10 个。清单与家族归类见[计划文档](../10-anime-source-and-player-plan.md)。

## 决策

### anime 源：站点重写，不做导入兼容

- 在 `:shared:source-api` 新增 **`AnimeSource` JS API**（`search` / `detail` / `episodes` / `resolve`）；`resolve` 结果一等公民支持防盗链请求头（Referer/UA）与流类型（hls/mp4/dash）。
- 逐站用自有格式重写；**不实现 Miru 宿主层，不实现 Kazumi XPath 规则解释器**。
- 用**模板源 + 站点配置**覆盖苹果CMS 采集站家族（一个模板脚本 + 每站一行 API 配置，覆盖 20+ 站）；中文动漫 HTML 站逐站编写，但共享源标准库（m3u8 提取、iframe/播放页解析等 helper）。模板配置可放进 ADR-0013 索引条目（该格式明确"未知字段忽略"，可平滑扩展）。
- 每站配 **canary 冒烟测试**（搜索 → 详情 → 解析出非空视频 URL）；源经 ADR-0013 仓库分发。
- 第一波：苹果CMS 模板 + 两生态重叠热站约 8 个。重反爬（9Anime 类）与需要 WebView 的站点不进第一波。

### 播放器：功能对齐 Animeko

- 目标功能面 = Animeko：手势（亮度/音量滑动、长按倍速、锁定）、画面滤镜与音量增强、外挂字幕、连播/下一集、倍速与轨道持久化、MediaSession/PiP、**弹幕**。实现全部自写。
- **合规红线：Animeko 为 AGPL-3.0，只做行为对齐，不复制任何代码**（比 Venera 的 GPL 规则更严，AGPL 覆盖网络服务场景）。mediamp 为 Apache-2.0（open-ani 出品），可放心深用。
- 首个技术风险：**mediamp 是否透传自定义请求头**（防盗链必需）。ExoPlayer/MPV 底层都支持，但 mediamp API 层未确认——先做半天 spike 再定实现方式（API 扩展或后端直连）。

## 后果

正面：

- 源代码完全自有可控，不依赖 Miru/Kazumi 的格式演进与宿主 API 长尾兼容；"支持哪些站点"由我们自主决定。
- 模板源机制使采集站家族一次覆盖 20+ 站，后续每站新增成本降到 0.5 天以内。
- 播放器与 mediamp 同源（同属 open-ani 技术栈），对齐目标明确、逐项可验证。

负面 / 风险：

- **站点维护责任转移到我们**：站方改版需我们更新源，这是重写模式与导入模式的本质差异。缓解：逐站 canary + 源仓库版本化，让用户能快速拿到修复。
- 桌面无 WebView：需要 WebView 过验证/反爬的站点不可支持，选站时先验证无头 HTTP + DOM 能走通。
- 弹幕渲染需自研：Animeko 弹幕引擎不可抄（AGPL），Compose 侧无成熟现成方案。
- 个别站 m3u8 加密/key 混淆：放弃该站，不与站方对抗。

## 实施记录（2026-10-30，D1–D9 完成）

| 项 | 结果 |
|---|---|
| D6 mediamp headers spike | **通过**：0.5.3 的 `UriMediaData.headers` 两个后端都应用（ExoPlayer `setDefaultRequestProperties`；MPV `user-agent`/`referrer`/`http-header-fields`，经产物字节码验证）。`VideoPlayerScreen` 已接入 `headers` 参数。无需上游改动 |
| D1 数据层 | `entry_episode` / `entry_chapter` / `video_resolve_cache` 三表 + 索引，迁移 1→2（createSql 与导出 schema 逐字比对一致）；`AnimeRepository`/`ChapterRepository`/`VideoResolveRepository`（replace 保留已读/书签标记）；备份三表全量导出/导入 |
| D2 AnimeSource | `AnimeSource`/`ManagedAnimeSource`（search/detail/resolve），JS API `extends AnimeSource`（`search`/`detail`/`resolve` 直方法），preamble 共享宿主（QuickJS/Ktor/KV/DOM）；manager 按脚本嗅探双类型加载；UI 链路：浏览→搜索→`SourceAnimeDetailScreen`（AnimeScreen）→resolve→`OnlineVideoPlayerRoute`（headers 进播放器），书架/已看/书签/解析缓存全部落库 |
| D3 stdlib + 苹果CMS | preamble 注入 `Std`（resolveUrl/extractM3u8/parseAppleCmsPlay/parseEpisodeNumber）；`applecms_template.js` 模板源 + fixture 测试 |
| D4 第一波站点 | **omofun 真站验证通过**（canary 7s，搜索→详情→`_dyn_plays` m3u8）。AGE PC 网页播放已死（服务端直接渲染"请下载APP"）；nyafun/次元城有 JS 反爬跳转；baimao/风车剧集 JS 渲染——均无头不可行，已记录。苹果CMS 模板覆盖 Miru 侧约 25 个采集站 |
| D5 canary | `CanarySourceTest`（`-Pcanary` 开关，默认排除出 CI），逐源 搜索→详情→解析 冒烟 |
| D7 播放器 | 倍速持久化（`player_speed`）、长按 2x（点击恢复）、上下滑亮度（左）/音量（右，`AudioLevelController`）、下一集按钮 + `player_auto_next` 自动连播（`MediaStatus.Ended`）。**外挂字幕后移**：mediamp 0.5.3 两个后端都不应用 `MediaExtraFiles`，需上游贡献 |
| C3 | 漫画侧：`SourceDetailScreen` 接章节表（加入书架/已读/书签/续读/离线阅读分支）；`shared:download` 下载队列（顺序 worker、断点续传、取消、原子写、`.complete` 标记）+ `DownloadQueueRoute` + 章节下载指示器；`BackupManager`（JSON 全量导出/恢复）+ `WebDavClient`（Basic 认证 PUT/GET）+ 备份与同步设置屏 |
| D8 弹幕 | `shared:danmaku`：dandanplay 开放 API 客户端（AppId/AppSecret HMAC-SHA256 签名，用户自配，v2 comments 为 `{m, p:"秒,模式,颜色,来源"}` 对象数组——从官方 swagger/生态实现核实）；标题+集号自动匹配；Compose 无状态帧驱动渲染器（TextMeasurer，lane 轮转，top/bottom 停留）；播放设置内开关；弹幕设置屏 |
| D9 仓库 config | index/manifest `config` 字段（JsonObject），loader 注入 `globalThis.__MIYOMU_CONFIG`；苹果CMS 模板读它覆盖站点参数 |

验证：7 个模块 JVM 测试 + `:desktopApp:compileKotlin` + `:androidApp:assembleDebug` 全绿；测试新增 26 个（data 9、download 4、danmaku 4、source-runtime 5+3、source-manager 3、network 2）。
