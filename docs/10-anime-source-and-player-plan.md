# anime 源与播放器计划（站点重写 + 对齐 Animeko）

- 状态：**已实施完成（2026-10-30，D1–D9）**· 实施记录见 [ADR-0014](adr/0014-anime-source-site-rewrite-and-player-alignment.md) 实施记录节
- 实施要点：omofun 真站验证通过（canary）；AGE/nyafun/次元城/baimao 无头不可行已记录；苹果CMS 模板覆盖采集站家族；外挂字幕后移（mediamp 0.5.3 不应用 extraFiles）；弹幕为用户自配弹弹play 凭据
- 前置：C1（源仓库）/ C2（运行时接入 + 漫画在线阅读）已完成，见 [09 交接文档](09-handoff-2026-10-29.md)；C3（数据层 / 下载）与本计划 D1 合并执行
- 估算口径：单人全职

## 1. 目标

把 Miru 与 Kazumi 两个生态覆盖的视频站点，用 **Miyomu 自己的 `AnimeSource` JS 源格式逐站重写**，实现"搜索 → 详情 → 剧集 → 解析播放"闭环；播放器功能面追平 **Animeko**。不做 Miru 模块导入、不做 Kazumi 规则解释器——站点支持是目标，格式兼容不是。

**非目标**（明确排除，不要当成遗漏去修）：

- BT 站（Mikanani/Nyaa/ACG.RIP/EZTV/YTS 类）——需要 BT 栈，Alpha 范围外；
- YouTube / IPTV m3u / media.ccc.de 等特殊源；
- 重反爬站（9Anime 类）与需要 WebView 过验证的站点（桌面无 WebView，见 §8）；
- 成人内容站；
- m3u8 加密/key 混淆的站点——遇到即放弃，不与站方对抗。

## 2. 生态覆盖面盘点（2026-10-30 实测）

数据来源：Miru 官方仓库索引（`miru-project/repo`，`index.json`，145 个模块中 **97 个视频源**，另有 39 漫画 + 9 小说模块）与 KazumiRules 官方仓库（`Predidit/KazumiRules`，**86 个站点规则** + 索引）。完整名单见附录 A/B。

站点按技术形态聚成三个家族（按命名归类，**待逐站验证**）：

| 家族 | 规模 | 技术形态 | 重写策略 |
|---|---|---|---|
| **苹果CMS 采集站** | Miru 侧约 25 站（量子/非凡/暴风/卧龙/无尽/玉兔/U酷/樱花资源/速播/天空/优质/丫丫…） | 标准 `/api.php/provide/vod/` JSON API，返回带 m3u8 剧集的播放地址 | **一个模板源 + 每站一行 API 配置**，一次覆盖 20+ 站，顺带支持影视剧 |
| **中文动漫 HTML 站** | Kazumi 86 规则的主体（AGE/樱花/次元城/omofun/nyafun/风车/clicli/稀饭/mifun/baimao…） | 各站 HTML 结构 + 播放页嵌 m3u8/iframe | 每站一个源，共享 stdlib |
| **欧美动漫站** | Miru 侧约 10 站（9Anime/HiAnime/Animepahe/Animeworld…） | 反爬重，少数有干净 API | 只挑 API 友好的（如 Animepahe，待验证）；反爬重的排除 |

**两家重叠的热点站约 8–10 个**（AGE、樱花、次元城、风车、NyaFun、稀饭、omofun、libvio 等）——被两个社区共同验证过，是第一波首选。

## 3. `AnimeSource` JS API（草案，实施时定稿）

现状：`:shared:source-api` 只有漫画侧（Venera 风格 `ComicSource` + `Source`/`SManga`/`Page`），**anime 源接口不存在**，需要新建。JS 面大致：

```js
class extends AnimeSource {
  config = { id, name, version, baseUrl, lang }

  async search(keyword, page)            // → { list: [{ title, cover, url, extras? }], maxPage? }
  async detail(itemUrl)                  // → { title, cover, introduction, lines: [[episode]] }
  async episodes(line)                   // → [{ name, url }]（多线路 lines 备选）
  async resolve(episodeUrl)              // → { urls: [{ url, quality?, type: "hls"|"mp4"|"dash" }],
                                         //      headers: { Referer, User-Agent, ... } }
}
```

设计要点：

- `resolve` 返回的 **`headers` 是一等公民**（防盗链 Referer/UA 是这类站的刚需，Kazumi 的 `playHeaders` 字段即为此设）；headers 必须一路传到播放器（见 §7 mediamp spike）。
- 多线路（"线路1/线路2"）映射到移植的 Aniyomi `EpisodeOptionsDialog`（hoster/quality）UI 位置。
- 宿主 primitives 复用现有漫画源运行时能力：`Network`、Ksoup DOM、KV storage、超时/内存/响应配额（ADR-0008）。
- 解析结果需要**缓存层**（点播每次都要 resolve，缓存 + TTL 直接决定换集体验），落在 anime 数据表（D1）。

### 模板源与标准库

- **模板源**：苹果CMS 家族写成"通用脚本 + 站点配置"。配置随源条目放进 ADR-0013 索引（该格式明确"未知字段忽略"，可加 `config` 字段，无需破坏性变更）。
- **stdlib**（JS 侧 helper，随运行时注入）：m3u8 提取器（页面/iframe/`player_data` 常见模式）、iframe 直链解析、苹果CMS API client、常见编码/解密 helper。目标：第一波之后每新增一站 ≤ 0.5 天。

## 4. 第一波站点

选择标准：两家生态重叠优先；无头 HTTP + DOM 可走通（无 WebView 依赖）；无重反爬；m3u8 明文可解析。

| 站点 | 家族 | 出处 | 备注 |
|---|---|---|---|
| 苹果CMS 模板源 | 采集站 | Miru | 一个动作覆盖约 25 站，最高性价比，先做 |
| AGE动漫 | HTML | 两家重叠 | Kazumi 规则标记 `useWebview`，先验证无头可达性 |
| 樱花动漫 | HTML | 两家重叠 | 播放页结构待验证 |
| 次元城动漫 | HTML | 两家重叠 | |
| 风车动漫 | HTML | 两家重叠 | |
| NyaFun | HTML | 两家重叠 | |
| 稀饭动漫 | HTML | 两家重叠 | |
| omofun | HTML | Kazumi | iframe/referer 场景典型 |
| clicli | HTML | Kazumi | |
| Animepahe（备选） | 欧美 | Miru | API 相对友好，待验证 |

第二波起按 canary 存活率与用户反馈扩站；Miru 的漫画源覆盖站点可作为漫画侧（`ComicSource`）的后续重写清单，复用同一套方法论。

## 5. 逐站 canary 冒烟测试

- 每个源一条 JVM 冒烟测试：`search(关键词) → detail → episode → resolve`，断言解析出非空 http(s) 视频 URL。
- 标记独立 tag（如 `canary`），**默认不进 CI**（第三方站点可达性不稳定，不能让 CI 红绿被站方行为绑架）；发版前与改站修复时本地手动跑。
- 测试报告按源列出存活状态，作为"哪个站的源坏了"的一眼定位。
- 与源仓库版本化配合：站点改版 → 修源 → 发新版本号 → 用户端提示更新。

## 6. 播放器对齐 Animeko 功能清单

**合规红线：Animeko 是 AGPL-3.0，只做行为对齐，一行代码不复制**（比 Venera GPL 规则更严）；mediamp 是 Apache-2.0，可放心深用。以下全部自写实现：

| 组 | 内容 | 依赖 |
|---|---|---|
| 手势 | 上下滑亮度/音量、长按倍速、双击区自定义（双击 ±10s 已有）、锁定手势 | 无，纯 UI |
| 画面/音频 | 滤镜（亮度/对比度/饱和度/伽马）、音量增强（>100% 增益）、缩放模式、截图 | mediamp 能力逐项验证 |
| 字幕 | 外挂 .srt/.ass 加载（`MediaExtraFiles`）、样式、轨道切换（已有基础） | mediamp 验证 |
| 连播 | 下一集按钮、自动连播、跳过 OP/ED 标记 | anime 数据层（D1） |
| 弹幕 | dandanplay 剧集匹配 + Compose 自绘渲染层 + 密度/透明度/偏移/屏蔽词 | 独立大块；自研渲染器 |
| 集成 | 倍速/轨道偏好持久化、MediaSession 通知控制、桌面键盘快捷键、Android PiP | 平台层 |

### mediamp 技术验证点（按优先级）

1. **请求头透传**（第一 spike，半天）：防盗链 headers 能否经 mediamp 到达 ExoPlayer/MPV。底层都支持，API 层未确认；不通则后端直连或给 mediamp 提 PR。
2. HLS/m3u8 播放（两后端原生支持，验证 mediamp 配置路径）。
3. ExoPlayer 的 ASS 渲染或降级策略（MPV 原生支持 ASS）。
4. 画面滤镜/音量增强在两后端的 API 可达性。

## 7. 任务分解与估算（单人全职）

| ID | 任务 | 依赖 | 估算 |
|---|---|---|---|
| D1 | anime 数据层：Room anime/episode/解析缓存表（与 C3 数据层合并设计，避免二次迁移） | C3 | 1.5 周 |
| D2 | `AnimeSource` JS API + 运行时接线（Kotlin 接口、JS 桥、注册/加载复用 source-manager） | D1 | 1 周 |
| D3 | stdlib + 苹果CMS 模板源 + 首站验证 | D2 | 1 周 |
| D4 | 第一波 HTML 站 8 个（每站 0.5–2 天） | D3 | 2 周 |
| D5 | canary 测试设施 | D3 | 2–3 天（与 D3/D4 并行） |
| D6 | mediamp headers 透传 spike | D2 之前 | 0.5 天 |
| D7 | 播放器第一批：手势 / 偏好持久化 / 外挂字幕 / 连播 | D6 | 2 周（可与 D3–D4 并行） |
| D8 | 弹幕：dandanplay 匹配 + Compose 渲染器 + 设置 | D7 | 2–3 周 |
| D9 | 源仓库对接（模板源 config 扩展、仓库分发）+ 第二波扩站 | D5 | 持续 |

关键路径 D1 → D2 → D3 → D4 约 5.5 周；播放器线 D6/D7/D8 约 4.5 周，可部分并行。**总体约 2.5–3 个月**。

## 8. 风险与取舍

- **站点维护是永久成本**：重写模式把源维护责任放在我们身上（与导入模式的本质差异）。canary + 版本化是缓解，不是消除；接受"站点改版 → 修源"的常态节奏。
- **桌面无 WebView 是硬约束**：Kazumi 生态里部分规则明确 `useWebview: true`；此类站与重反爬站不支持，选站时先做无头可达性验证。
- **mediamp headers 若不通**：退路是后端直连（ExoPlayer DataSource / MPV `http-header-fields`），代价是绕开 mediamp 抽象的一角；若要做干净，给 mediamp 上游提 PR。
- **弹幕是最大新增块**：AGPL 不可抄、Compose 无现成方案，自研渲染器是 D8 的 2–3 周主体；若进度吃紧，弹幕可整体后移而不阻塞源生态。
- **Animepahe 等欧美站**：语言/时区/合规复杂度高于收益，仅作备选，不进第一波主线。

## 附录 A：Miru 官方仓库视频源名单（97 个，2026-10-30 抓取）

345movie · 360资源 · 9Anime · AGE动漫 · AniGoGo · AniLiberty · AnimeFlv · girigiri爱动漫 · Animeazu · Animepahe · Animeworld · 奥斯卡资源站 · 音悦台MTV · Arabsama · 暴风资源[高清无水印] · MyIPTV · 欧乐影院 · 咕咕影视 · 次元城动漫 · media.ccc.de · 7喜影院 · Draftsex · DramaCool · EroticMV · EZTV · 非凡资源 · Filmpalast · GakiArchives · gimy · Hanime · 黑木耳资源 · HiAnime · HindiMovies · 华为吧资源 · IDLIX · iKun资源 · IPTV-ORG · Iwara · JAVHD.icu · 极速资源 · KimCartoon · Kisskh · 老鸭资源 · Letv影院 · libvio · 量子资源 · Mikanani · 魔都资源网 · Enime · MonosChinos · Movieku · YTS.mx · NETFAPX · Nimegami · Nyaa · NyaFun动漫 · OneJav · Otakudesu · 在线影院 · PornXP · ACG.RIP · Rule34Video · 樱花动漫 · Samehadaku · 色猫资源 · 森林资源 · Senpai Tambayan · sFlix · 速播资源 · SuperCartoons · TamilYogi · MoviesArc · 天空资源网 · 好看的1号 · TopCartoons · Turkish123 · UAA视频 · U酷资源网 · Unimay · 影视集合 · 18影视集合 · 八戒影视 · 风车动漫 · 卧龙资源 · 无尽资源网 · 稀饭动漫 · xHamster · XNXX · XVIDEOS · 好看的2号 · 丫丫资源[1080无水印] · 樱花资源 · 樱花动漫 CC · YoMovies · YouTube · 玉兔资源 · 优质资源库

（另有 39 个漫画源与 9 个小说源，漫画侧重写清单另行整理。）

## 附录 B：KazumiRules 官方仓库站点规则名单（86 个，2026-10-30 抓取）

1ANI · 233dm · 295yhw · 7sefun · 80tv · 9ciyuan · AGE · AnFuns · BF · DM84 · FQDM · Fantuan · HZDM · IF · LMM · MT · MXdm · NT · TvTFun · WYDM · XY · aafun · akianime · anime7 · ant · aowu · baimao · bobodm · brovod · ciyuancheng · clicli · cyfz · dalvdm · dlma · dmand · dmghg · dms · eacg · enlie · ezdmw · fcdm · ffdm · giriGiriLove · gpjda · gugu3 · hfkzm · jzsdm · k8dm · kimani · libvio · mandao · mcy · mengfan · mgnacg · mifun · milimili · mitang · mitaodm · moefan · moonci · mutefun · mwcy · nekodm · nyafun · omofun · omofun03 · omofunz · pekolove · qdm · qifun · qimi · qkan9 · skr · sorani · tt776b · xfdm · xfdmneo · xfdmnext · xiaobao · xiapidm · xigua · yinghua · yishijie · ylsp · ziyedm · zkk79
