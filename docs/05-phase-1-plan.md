# Phase 1 计划（提案）

- 状态：已确认（Apache-2.0；Miyomu；单人全职；Android + 桌面并行）· 2026-10-28
- M0：核心完成（2026-10-28），见 [M0 清单](checklists/phase-1-m0.md) · [ADR-0006](adr/0006-unified-android-toolchain.md) · [ADR-0007](adr/0007-network-and-cookie-storage.md)
- M1：进行中（2026-10-28），见 [M1 清单](checklists/phase-1-m1.md) · [ADR-0008](adr/0008-venera-source-runtime.md)
- 依据：[S1 报告](spikes/results/s1-player.md) · [S2 报告](spikes/results/s2-js-runtime.md) · [S3 报告](spikes/results/s3-cmp-ui.md) · [Phase 0 清单](checklists/phase-0.md)

## 1. Phase 0 决策

**Go（缩范围）**：

- 首期做 **Android + 桌面**（Windows/macOS/Linux），iOS 延后；
- 首发媒介：**漫画 + 视频**；小说放 M3，本地 TXT/EPUB 优先；
- 源：JS + QuickJS，HTML DOM/Cookie 读路径已验证；Ktor 正式网络层、Cookie 持久化、源仓库为 Phase 1 必做；
- 播放器：mediamp 锁 0.5.1，但必须补音轨/内嵌字幕轨道选择；
- iOS：先做真实 App 壳 AVKit spike，失败则改用平台 AVPlayer 自定义播放器或降低优先级；
- 许可证：Apache-2.0；名称：Miyomu（2026-10-28 确认，域名/商标待清查）；团队：单人全职；平台：Android + 桌面并行。

## 2. Alpha 范围

| 能力 | Alpha 包含 | 不包含 |
|---|---|---|
| 平台 | Android 8+；Windows/macOS/Linux 桌面 | iOS |
| 漫画 | 本地 CBZ/ZIP/7Z/PDF；在线源、下载、阅读进度 | RAR |
| 视频 | mediamp 播放、外挂字幕、seek/倍速、音轨选择 | 投屏、BT、直播 |
| 小说 | 本地 TXT/EPUB、排版、进度 | 在线书源 |
| 源 | JS QuickJS、HTML DOM、Cookie、用户源仓库 | 源市场/推荐 |
| 数据 | 收藏/历史/分类/下载/备份/WebDAV 同步 | 自建云服务 |
| 追踪 | 无 | MAL/AniList/Bangumi |

## 3. 里程碑（单人全职；5–7 个月到 Alpha）

### M0 工具链与网络层（1–2 周）

- ✅ 锁定单一工具链：AGP 9.4.1 + KGP/编译器 2.3.21 + Compose 1.12 + compileSdk 37（[ADR-0006](adr/0006-unified-android-toolchain.md)）。
- ✅ 解决 quickjs-kt 元数据冲突：Kotlin 2.3.21 直接读取 2.4 元数据，放弃双工具链/AAR 包装。
- ✅ Miyomu 标识符清查：`miyomu.app` 等可注册；GitHub `miyomu` 被占用，推荐 org `miyomu-app`；包名 `app.miyomu`，Maven group 建议 `io.github.miyomu-app`。注册与正式商标检索待执行（[品牌备忘](06-brand.md)）。
- ✅ Ktor 网络层（Android OkHttp / 桌面 CIO）+ 持久 Cookie（[ADR-0007](adr/0007-network-and-cookie-storage.md)）；Darwin 引擎随 iOS 接入。
- ✅ CI 工作流：桌面根构建 + 两个 Android 独立工程 + iOS 测试 + M0 探针；命令已本地验证，GitHub 首跑待仓库初始化（[ci.yml](../.github/workflows/ci.yml)）。

### M1 核心与单媒介（4–6 周，进行中）

- ✅ 生产 KMP 工程（仓库根目录）：model / network / source-api / source-runtime；Android + 桌面壳。
- ✅ JS 源运行时读路径：每源 QuickJS、Ktor、持久 Cookie、KV、Ksoup DOM、超时/内存/响应配额；JVM 测试 5/5。
- ✅ 双端调试壳：同一 HTML fixture 源跑通搜索→详情→章节→图片（[证据](../evidence/)）。
- ✅ KMP 数据层：Room 2.8.5 + KSP 2.3.12 + bundled SQLite；库/进度已接入双端（[ADR-0009](adr/0009-room-kmp-data-layer.md)）。
- ⏳ 真实 HTML Venera 兼容源零修改对照（需要合法本地测试源）。
- [ ] `:shared:library` / `:shared:download` / `:shared:sync` 产品化。
- [ ] 源运行时产品化：源仓库索引/校验、JS Cookie API、explore/categories、每源日志。
- [ ] 漫画阅读器 MVP：Android + 桌面（复用 Aniyomi loader/VM，重写视图）。
- [ ] 播放器轨道选择 spike：ExoPlayer/MPV 后端直连，定义 `TrackSelector`。
- 验收：一个真实 HTML Venera 兼容源在 Android + 桌面跑通搜索→详情→章节→图片。

### M2 迁移与桌面壳（6–8 周）

- Fork Aniyomi，Strangler 迁移 Library/Detail/Settings/Source 等屏幕到 CMP。
- 桌面 CMP 壳（窗口、路由、图片、下载目录）。
- Android feature flag 接入共享 core；旧 APK 扩展仅作限期过渡。
- 验收：Android 与桌面共享核心；屏幕迁移单屏 ≤1.5 人日；持续发版。

### M3 视频 / 小说 / 同步（6–8 周）

- 视频播放器 UI：mediamp + 轨道选择 + 外挂字幕 + 续播。
- 小说：TXT/EPUB 导入、分页排版、字体/主题/进度。
- 下载队列、WebDAV 同步、备份/恢复。
- 验收：三种媒介各自可用；离线库完整；同步冲突可恢复。

### M4 Alpha 硬化（3–4 周）

- 性能：1,000 项库、长图/大文件、启动时间。
- 源兼容性与错误处理；崩溃与日志；打包/签名/更新。
- 验收：Android + 桌面 alpha 可日常使用；无 P0 数据丢失/崩溃缺陷。

总估算：单人全职 5–7 个月到 Alpha；2–3 人可压缩约 30–40%。以上不含 iOS 正式支持。

## 4. 工作流与验收

| 工作流 | 关键任务 | 验收 |
|---|---|---|
| Toolchain | 单轨工具链、CI | 根构建 + Android 两工程 + iOS 测试全绿 |
| Source runtime | Ktor、Cookie 持久化、DOM 覆盖、源仓库 | 真实 Venera 源零修改读路径 |
| Player | 轨道选择、字幕、PiP、长播 | 多音轨/内嵌字幕可选；30min 无崩溃 |
| Fork/UI | CMP 屏幕迁移、桌面壳 | 单屏 ≤1.5 人日；双端一致 |
| Data/sync | Room、下载、WebDAV | 断网/冲突/恢复测试通过 |
| Legal/release | 许可证、品牌、源合规 | Q1/Q2 决策完成，无 GPL 污染 |

## 5. 关键技术与风险

- ~~**双 Android 工具链**~~：M0 已解决。AGP 9.4.1 + KGP 2.3.21 单轨即可读取 quickjs-kt 的 Kotlin 2.4 元数据（[ADR-0006](adr/0006-unified-android-toolchain.md)）。
- **音轨/内嵌字幕**：mediamp 统一 API 缺失；优先后端直连，其次扩展 mediamp。
- **iOS**：裸 K/N 测试宿主无法驱动 AVKit；必须用 App 壳验证；AVKit 不支持 MKV/SRT/ASS，需自定义字幕层或降低范围。
- **源生态**：Venera 兼容可读路径已验证；源仓库、版本兼容与真实源维护是长期成本。
- **MD3E alpha**：S3 需显式 `material3:1.12.0-alpha03`；Phase 1 锁版本并保留 M3 回退。
- **合规**：不内置源、不托管内容；许可证 Apache-2.0（待 Q1）。

## 6. 已确认决策

| ID | 决策 | 结果 |
|---|---|---|
| Q1 | 许可证 | **Apache-2.0**（保留闭源/上架可能） |
| Q2 | 产品名/品牌 | **Miyomu**（2026-10-28）；不设独立中文名；域名/商标 Phase 1 M0 清查 |
| Q3 | 团队与投入 | **单人全职**；Alpha 5–7 个月 |
| Q4 | 首发平台 | **Android + 桌面并行**；iOS 后置 |
