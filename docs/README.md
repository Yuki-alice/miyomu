# Miyomu 文档索引

## 当前权威方案

1. [架构优先修正方案](07-architecture-first.md) — 功能照搬、模块重构、迁移顺序、并行设计。
2. [MD3E 设计系统](08-md3e-design.md) — 色彩、字体、形状、动效、组件、页面方向。
3. [初始化方案](09-initialization.md) — 20 模块 + build-logic、Room、MD3E、迁移顺序、CI。
4. [P0 状态](10-p0-status.md) — 三端构建结果、截图、已知问题与 P1。
5. [P1 状态](11-p1-status.md) — reader-core 复制、193 测试、source/net 阻塞。
6. [build-logic](12-build-logic.md) — 约定插件、模块写法与验证。

## 分析材料（初稿）

7. [参考项目分析](00-reference-analysis.md) — Kototoro 的规模、机制与风险。
8. [技术栈初稿](01-tech-stack.md) — KMP + CMP 选型；DB/ID 部分已被 07 修正。
9. [架构初稿](02-architecture.md) — 模块/SourceRuntime/DB；部分结论已被 07 修正。
10. [功能矩阵](03-feature-matrix.md) — parity 矩阵与迁移映射。
11. [UI 初稿](04-ui-ux-design.md) — Sumi 方向已作废；保留布局/阅读器思路。
12. [初始化初稿](05-initialization-plan.md) — Gradle/CI 细节可参考；迁移顺序已被 07 修正。
13. [路线图初稿](06-roadmap.md) — 里程碑可参考；阶段已被 07 调整。

## 决策记录

- [ADR-0001：KMP + Compose Multiplatform](adr/0001-kmp-compose.md)
- [ADR-0002：SQLDelight（已被 ADR-0005 取代）](adr/0002-sqldelight.md)
- [ADR-0003：复合 ID（部分被 ADR-0005 修正）](adr/0003-source-runtime.md)
- [ADR-0004：自适应单一 UI](adr/0004-adaptive-ui.md)
- [ADR-0005：Room KMP 与 Long ID](adr/0005-room-and-ids.md)
- [ADR-0006：架构优先的 Strangler 迁移](adr/0006-architecture-first.md)

## 当前状态

- 阶段：P1 进行中（reader-core + build-logic 已完成）
- 代码：三端壳 + MD3E + reader-core 已落地
- 原型：并行设计（MD3E）
- 下一步：source/net 复制（等待网络）、Navigation
