# Miyomu 文档索引

## 当前权威方案

1. [架构优先修正方案](07-architecture-first.md) — 功能照搬、模块重构、迁移顺序、并行设计。
2. [MD3E 设计系统](08-md3e-design.md) — 色彩、字体、形状、动效、组件、页面方向。
3. [初始化方案](09-initialization.md) — 20 模块 + build-logic、Room、MD3E、迁移顺序、CI。
4. [P0 状态](10-p0-status.md) — 三端构建结果、截图、已知问题与 P1。
5. [P1 状态](11-p1-status.md) — reader-core/source/net 复制、372 测试。
6. [build-logic](12-build-logic.md) — 约定插件、模块写法与验证。
7. [Navigation](13-navigation.md) — 路由、返回栈、返回处理与迁移计划。
8. [数据流](14-data-flow.md) — Fixture → Repository → UI 的第一条真实链路。
9. [Reader 路由](15-reader-route.md) — Details → Reader 与分页演示。
10. [Room 持久化](16-persistence.md) — 收藏/阅读进度落库与平台数据库位置。
11. [真实 JSON 源](17-real-source.md) — MangaDex + Coil 封面/页面。
12. [ViewModel + Koin](18-viewmodel-koin.md) — 四个主 ViewModel 与依赖装配。

## 分析材料（初稿）

13. [参考项目分析](00-reference-analysis.md) — Kototoro 的规模、机制与风险。
14. [技术栈初稿](01-tech-stack.md) — KMP + CMP 选型；DB/ID 部分已被 07 修正。
15. [架构初稿](02-architecture.md) — 模块/SourceRuntime/DB；部分结论已被 07 修正。
16. [功能矩阵](03-feature-matrix.md) — parity 矩阵与迁移映射。
17. [UI 初稿](04-ui-ux-design.md) — Sumi 方向已作废；保留布局/阅读器思路。
18. [初始化初稿](05-initialization-plan.md) — Gradle/CI 细节可参考；迁移顺序已被 07 修正。
19. [路线图初稿](06-roadmap.md) — 里程碑可参考；阶段已被 07 调整。

## 决策记录

- [ADR-0001：KMP + Compose Multiplatform](adr/0001-kmp-compose.md)
- [ADR-0002：SQLDelight（已被 ADR-0005 取代）](adr/0002-sqldelight.md)
- [ADR-0003：复合 ID（部分被 ADR-0005 修正）](adr/0003-source-runtime.md)
- [ADR-0004：自适应单一 UI](adr/0004-adaptive-ui.md)
- [ADR-0005：Room KMP 与 Long ID](adr/0005-room-and-ids.md)
- [ADR-0006：架构优先的 Strangler 迁移](adr/0006-architecture-first.md)

## 当前状态

- 阶段：P1 进行中（核心模块 + Navigation + Room + MangaDex + Coil + Koin）
- 代码：三端壳 + MD3E + reader-core 已落地
- 原型：并行设计（MD3E）
- 下一步：源管理/筛选、迁移测试、ViewModel 单测
