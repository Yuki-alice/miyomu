# 文档索引

> 最后整理：2026-10-30。当前交接见 [11-handoff-2026-10-30.md](11-handoff-2026-10-30.md)；05/07/08/09 等为历史文档，保留实施记录。

| 文档 | 内容 | 状态 |
|---|---|---|
| [00 产品简报](00-product-brief.md) | 产品定位、v1 范围、原则、非目标 | 草案 |
| [01 目标架构](01-architecture.md) | 模块划分、数据模型、源协议、播放器、迁移策略 | 草案 |
| [02 Phase 0 计划](02-phase-0.md) | 时间盒、三个 Spike、决策门、交付物 | 已完成（历史） |
| [03 风险登记](03-risk-register.md) | 风险、触发条件、缓解措施、退出标准 | 维护中 |
| [04 开放问题](04-open-questions.md) | 待决策事项与建议选项 | 待输入 |
| [05 Phase 1 计划](05-phase-1-plan.md) | 缩范围后的里程碑、任务与验收 | 历史（被 ADR-0011/0014 取代） |
| [06 品牌备忘](06-brand.md) | Miyomu 名称、标识符、看板娘方向 | 名称已确认；域名/商标待清查 |
| [07 Skills 与参考仓库](07-skills-and-references.md) | 已安装技能、许可、参考仓库与 agent 策略 | 历史（技能库不在本机，策略停用） |
| [08 M1 任务重排](08-m1-task-plan.md) | M1 本地漫画/小说阅读器、共享 UI、Aniyomi 复用边界 | 里程碑排序被 ADR-0011 取代，工程细节仍可引用 |
| [09 交接文档](09-handoff-2026-10-29.md) | KMP 版 Aniyomi 移植进度、环境、逐阶段替换记录 | 历史（截至 C2，保留 B1–C2 记录） |
| [10 anime 源与播放器计划](10-anime-source-and-player-plan.md) | anime 站点重写策略、覆盖面盘点、AnimeSource API、canary、播放器对齐 Animeko | 已实施完成（2026-10-30，D1–D9） |
| [11 当前交接](11-handoff-2026-10-30.md) | Phase D 后的能力、模块/数据层、CI 缺口、并行开发规则与下一步队列 | **最新（2026-10-30）** |
| [12 小说端计划](12-novel-plan.md) | 双设备分工决策、小说参考仓库与许可边界、N1–N3 切片与实施记录 | N1–N3 完成并已合入 main（2026-10-30） |
| [13 小说端对标调研](13-novel-benchmark.md) | 对标 Google Play Books / legado / NovelDokusha / Book's Story 的功能矩阵与优先级 | 本批功能已实现（2026-10-30） |
| [Phase 0 清单](checklists/phase-0.md) | 可勾选的任务列表与命令 | 已完成（历史） |
| [Phase 1 M0 清单](checklists/phase-1-m0.md) | 工具链、Ktor/Cookie、标识符、CI | 核心完成 |
| [Phase 1 M1 清单](checklists/phase-1-m1.md) | 共享 UI、本地库、漫画/小说阅读器 | 历史（被 ADR-0011/0014 取代） |
| [Spike S1 播放器](spikes/s1-player.md) | mediamp 三端验证规格 | 桌面/Android 通过；iOS 壳未开始 |
| [Spike S2 源运行时](spikes/s2-js-runtime.md) | QuickJS + Venera 兼容源验证规格 | 已完成（C2/D 已接入产品链路） |
| [Spike S3 UI](spikes/s3-cmp-ui.md) | CMP + MD3E 屏幕迁移验证规格 | 已完成（A/B 移植已落地） |

## Spike 结果

- [S1 播放器报告](spikes/results/s1-player.md) — 桌面 MP4/MKV/HLS/HEVC、SRT/ASS、seek/倍速/暂停通过
- [S2 源运行时报告](spikes/results/s2-js-runtime.md) — 三端 JSON + HTML/Cookie、并发、超时/中断通过；持久化/正式网络层待补
- [S3 UI 报告](spikes/results/s3-cmp-ui.md) — headless 三屏截图 + 1,000 项滚动 158ms；MD3E 需锁定 alpha 依赖
- [报告模板](spikes/results/TEMPLATE.md)

## ADR

| 编号 | 决策 | 状态 |
|---|---|---|
| [0001](adr/0001-kmp-cmp-baseline.md) | 采用 KMP + CMP 作为目标技术栈 | 提议（Phase 0 验证） |
| [0002](adr/0002-fork-and-strangler.md) | fork Aniyomi，按模块翻修 | 提议 |
| [0003](adr/0003-js-extension-runtime.md) | JS + QuickJS 作为唯一扩展运行时 | 提议 |
| [0004](adr/0004-licensing-and-clean-room.md) | Apache-2.0 与 GPL 清洁边界 | 提议 |
| [0005](adr/0005-mediamp-player.md) | mediamp 作为播放器抽象 | 提议（Phase 0 验证） |
| [0006](adr/0006-unified-android-toolchain.md) | 单一 Android/KMP 工具链 | 接受（M0 验证） |
| [0007](adr/0007-network-and-cookie-storage.md) | Ktor 网络层与持久 Cookie | 接受（M0 验证） |
| [0008](adr/0008-venera-source-runtime.md) | Venera 风格 JS 源运行时读路径 | 接受（M1 切片；真实源待验证） |
| [0009](adr/0009-room-kmp-data-layer.md) | Room KMP 作为数据层 | 接受（M1 验证） |
| [0010](adr/0010-aniyomi-local-reader-reuse.md) | Aniyomi 本地阅读器复用边界 | 接受（M1） |
| [0011](adr/0011-kmp-aniyomi-port.md) | KMP 版 Aniyomi 大面积移植策略（取代 08 的里程碑排序） | 接受（A2–D 已实施） |
| [0012](adr/0012-desktop-video-backend-licensing.md) | 桌面视频后端的许可证边界（MPV 原生库 GPL-3.0） | 接受（方案 A：macOS/Windows LGPL 运行时） |
| [0013](adr/0013-js-source-repository-format.md) | JS 源包与仓库索引格式 | 接受（C1） |
| [0014](adr/0014-anime-source-site-rewrite-and-player-alignment.md) | anime 源站点重写策略与播放器对齐 Animeko（AGPL 行为对齐红线） | 接受（D1–D9 完成，含实施记录） |

## 约定

- 日期一律写绝对日期（`2026-10-28`）。
- 估算统一标注「单人全职」或「小团队」。
- 未验证的结论标记 **待验证**，不写成既定事实。
- 文档随决策更新；过期的 ADR 不删除，标记 `Superseded by ADR-XXXX`。
