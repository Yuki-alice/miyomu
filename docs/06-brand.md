# Miyomu 品牌备忘

- 名称：**Miyomu**
- 确认日期：2026-10-28
- 状态：名称已确认；域名/商标待正式清查

## 名称

- 读音：/miːˈjoʊmuː/（mee-YOH-moo）
- 构成：見る **miru**（看）+ 読む **yomu**（读）
- 含义：看与读；一个库承载视频、漫画、小说。
- 写法：正式文案用 **Miyomu**；代码、包名、域名、仓库名一律小写 `miyomu`。
- 语言策略：**全球统一名称，不设独立中文名**，也不做音译。
- 定位：安静、清晰、可靠的跨平台三合一内容阅读器；源可扩展，但不托管内容。
- 建议 Slogan：
  - See. Read. Keep.
  - One library. Every story.
  - Stories, in every form.

## 标识符

| 用途 | 暂定值 | 状态（2026-10-28 清查） |
|---|---|---|
| GitHub 仓库/组织 | `miyomu-app` | 可用；`miyomu` 已被个人用户占用（2020 年注册、0 仓库），避开 |
| GitHub 备选 | `miyomu-reader` / `getmiyomu` / `miyomuapp` | 均可用 |
| Android 包名 | `app.miyomu` | 采用（反向域名约定，不要求持有域名） |
| Maven group | `io.github.miyomu-app` | 建议；待 GitHub 组织创建后用于 Sonatype 验证 |
| 主域名 | `miyomu.app` | RDAP 404，可注册；与包名 `app.miyomu` 对应 |
| 备选域名 | `miyomu.dev` / `miyomu.io` / `miyomu.net` | RDAP 404，可注册 |
| 已占用域名 | `miyomu.com` | 2021-11-26 注册，当前 ACTIVE（到期 2026-11-26），不可用 |
| 标识符大小写 | 全小写 `miyomu` | 约定 |

查询方法：域名用 RDAP（`rdap.org/domain/<name>` 与 Verisign RDAP）交叉验证；GitHub 用 profile 页返回码（404 = 命名空间可用）。本地 DNS 返回 198.18.0.x 是网络劫持，不能作为依据。

商标：App Store 检索未发现同名应用；公开网络检索未发现 “Miyomu” 商标命中（小米的 “MiMo” 拼写不同，不构成同名）。**JP/US/EU/CN 正式商标检索仍待做**，公开使用前完成。

## 品牌角色（看板娘）方向

后续品牌化可围绕名称设计看板娘，但应用图标与核心 UI 建议保持非人类符号（眼 + 书页/播放键），避免商店审核与过度二次元化风险。

1. **Miyo（みよ）——纸页精灵**
   - 由书页折成的小生命；一只眼睛是播放键，发带是书签。
   - 可拟人化，适合社区表情包、商店截图、周边。
2. **纸鸮**
   - 猫头鹰：夜读与“看”的意象；羽毛像纸页。
   - 非人类、全球友好、易做成动画与图标；推荐作为主品牌符号。
3. **双形态**
   - Miru 形态：播放键/屏幕发饰，偏视频；Yomu 形态：书签/纸页，偏阅读。
   - 用于加载、空状态、更新提示，表达“看 + 读”的切换。

视觉基调：夜蓝（#1B2A4A）、纸白（#F6F1E7）、播放琥珀（#E8A33D）；辅助青绿。避免直接使用 Aniyomi/Venera 的图标、名称或角色。

## 迁移说明

- Phase 0 spike 代码仍使用 `org.projectv.spike.*` 包名，S3 样例窗口标题仍是 “Project V — Phase 0 / S3”，均为历史临时标识。
- Phase 1 M0 新建仓库时统一为 `miyomu`，并替换包名、窗口标题、应用名与 Maven group。
- 工作目录已更名为 `/Users/kurisu/miyomu`；生产构建位于仓库根目录，Phase 0/1 验证代码在 `experiments/`。

## 待办

- [x] 域名可用性查询（RDAP，2026-10-28）
- [x] GitHub org / 包名占用检查（2026-10-28）
- [ ] 注册 `miyomu.app`（或备选 `.dev`/`.io`）与 GitHub org `miyomu-app`
- [ ] 创建 GitHub org 后把 Maven group 定为 `io.github.miyomu-app`
- [ ] JP/US/EU/CN 商标关键词正式检索
- [ ] 若商标冲突，启用后备名 Tsuzuru 或 Yonaga
- [ ] 设计应用图标与非人类符号
- [ ] 看板娘设定稿与表情包（后续）
