# Phase 0：技术验证计划

> 状态：执行中 · 2026-10-28 → 2026-11-24（4 周，单人全职假设；兼职按 6–8 周排）
> 目标：用最小代码回答三个决定性问题，避免在错误的地基上投入 12 个月。

## 1. 要回答的三个问题

1. **S1**：mediamp 能否在 Android、macOS、iOS 三端覆盖播放器的必备能力（HLS、字幕、续播、画中画）？
2. **S2**：QuickJS 能否在 KMP 三端安全、稳定地运行 Venera 兼容的漫画源（读路径）？
3. **S3**：Aniyomi 的 Compose 屏幕能否以可接受的成本迁到 CMP，并使用 MD3 Expressive？

## 2. 工作规则

- Spike 代码放在 `experiments/spikes/`，**允许丢弃**，不追求架构优雅。
- 每个 Spike 有固定时间盒和通过标准；到期即出报告，不无限打磨。
- 结论必须附证据（日志、矩阵、截图、耗时），不写"感觉可用"。
- 测试素材只用自生成或公共领域/CC 授权内容；不下载受版权保护的影视资源。
- 不复制 GPL 代码；Venera 兼容层按文档重写，见 [ADR-0004](adr/0004-licensing-and-clean-room.md)。
- 不修改 Aniyomi 主干代码；S3 只做独立样例，避免 Phase 0 污染后续迁移。

## 3. Spike 概览

| ID | 名称 | 时间盒 | 关键产出 | 通过标准 |
|---|---|---|---|---|
| S1 | [播放器](spikes/s1-player.md) | 6 个工作日 | 三端播放器样例 + 功能矩阵 | 必备项全过，矩阵 ≥80% |
| S2 | [JS 源运行时](spikes/s2-js-runtime.md) | 8 个工作日 | 三端运行器 + 兼容源 | 读路径全通，超时/中断可靠 |
| S3 | [CMP/MD3E UI](spikes/s3-cmp-ui.md) | 6 个工作日 | 三屏桌面样例 + 依赖审计 | 三屏可运行，无死路依赖 |

缓冲 4 个工作日，用于补齐证据与决策会议。

## 4. 日程

| 周 | 日期 | 安排 |
|---|---|---|
| W1 | 10-28 → 11-01 | 仓库/工具链；S3 三屏骨架；S1 桌面 + Android 基础播放 |
| W2 | 11-02 → 11-08 | S1 字幕/HLS/PiP/iOS；S2 运行时嵌入（桌面 + Android） |
| W3 | 11-09 → 11-15 | S2 Venera 兼容读路径 + iOS；S3 MD3E 与依赖审计；S1 报告 |
| W4 | 11-16 → 11-22 | 补测、三份报告、决策会议、ADR 更新 |
| 缓冲 | 11-23 → 11-24 | 风险缓冲 |

每周五更新风险登记与任务状态；任何 Spike 在时间盒过半仍无核心结论时，立即上报并考虑缩小范围。

## 5. 决策门

Phase 0 结束时的结论只能是以下之一：

| 结论 | 条件 | 下一步 |
|---|---|---|
| **Go** | 三项全过 | 进入 Phase 1：抽 KMP core + JS 源 + Android feature flag |
| **Go（缩范围）** | S1/S2/S3 中一项为条件性通过 | 调整 v1 范围（如视频后置、iOS 后置、放弃 fork）后进入 Phase 1 |
| **Pivot** | S3 失败（CMP/屏幕迁移不可接受） | 新建 CMP 壳或重新评估 Flutter；不 fork Aniyomi |
| **Stop** | S1 + S2 同时失败 | 停止三媒跨平台计划，退回单平台方案 |

任何 Pivot/Stop 决定必须更新 ADR 与产品简报，并记录触发证据。

## 6. 环境基线

| 项目 | 版本/位置 |
|---|---|
| JDK | 21（Corretto 21.0.12） |
| Gradle | 9.6.0（根 wrapper） |
| Android Spike | AGP 8.13.2 + Kotlin 2.4.21 + Gradle 8.13（独立工程） |
| iOS Spike | Kotlin/Native 2.4.10 + Gradle 8.13 + iosSimulatorArm64 |
| Kotlin | 2.4.21 |
| Compose Multiplatform | 1.12.2 |
| mediamp | 0.5.1（S1 锁定；验证与 CMP 1.12 的兼容） |
| AGP | 8.13.2（Android Spike；AGP 9.4 内置 Kotlin 2.2 与 Kotlin 2.4 依赖不兼容） |
| quickjs-kt | 1.0.15（S2 锁定；如遇 bug 记录并评估 vendor QuickJS） |
| Android SDK | platforms 37 已装；Android Spike 使用 compileSdk 36（AGP 8.13） |
| Xcode | 26.6；iOS 17 系列模拟器 |
| 设备 | Android AVD `han1me-16kb`；iPhone 17 模拟器 |

所有版本在 Spike 报告中记录实际锁定值；升级必须单独评估。

## 7. 交付物

- 三份 Spike 报告：`docs/spikes/results/s1-player.md`、`s2-js-runtime.md`、`s3-cmp-ui.md`（目录在 Phase 0 结束时创建）。
- 可复现的样例代码（`experiments/spikes/s1-player`、`experiments/spikes/s2-js-runtime`、`experiments/spikes/s3-ui`）。
- 依赖迁移审计表（S3）。
- 播放器功能矩阵（S1）。
- JS API 覆盖表与性能数据（S2）。
- 更新后的 ADR、风险登记与 Phase 1 任务清单。

## 8. 明确不做

- 不接真实第三方内容源；只用自建 fixture。
- 不实现下载、同步、小说排版、完整播放器 UI。
- 不处理 iOS 分发、签名、上架。
- 不重构 Aniyomi；不复用任何 GPL 代码。
- 不追求视觉终稿；S3 只验证"迁移成本与能力边界"。

## 9. 退出条件

Phase 0 完成 = 三份报告可复核 + 决策门结论明确 + Phase 1 范围与任务列表更新。没有报告就不算完成，代码只是证据。
