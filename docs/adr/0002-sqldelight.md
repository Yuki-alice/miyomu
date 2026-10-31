# ADR-0002：使用 SQLDelight 作为 KMP 数据库

> **已被 [ADR-0005](0005-room-and-ids.md) 取代。** 当前方案保留 Room KMP，以复用 Kototoro 的 schema/DAO/迁移。

- 状态：Accepted
- 日期：2026-10-31

## 背景

Miyomu 从干净 schema 开始，需要 Android、JVM、iOS 三端一致的 SQLite 访问、迁移与查询可控性。候选：

1. SQLDelight 2.4.1
2. Room 2.8.5 KMP
3. Exposed / 手写 SQLite

## 决策

采用 **SQLDelight**。

## 理由

- 原生 KMP，Android/JVM/Native driver 完整。
- SQL-first：列表投影、索引、`EXPLAIN QUERY PLAN` 可直接审查。
- 无 KSP，构建更简单。
- `.sqm` 迁移 + `verifyMigrations` 适合从 v1 建立可靠迁移链。
- 生成的类型安全 API 足够，不需要 ORM 的隐式行为。

## 后果

### 正面

- 查询性能可控，避免 `SELECT *` + 懒加载导致的 N+1。
- 迁移文件是普通 SQL，易 review。
- 与 `core:model` 解耦，schema 不依赖运行时。

### 负面

- 需要写 SQL 与映射代码。
- 复杂联表/动态筛选需要手写。
- 团队需要熟悉 SQLDelight 的生成规则。

## 约束

- 列表查询只返回投影；禁止在列表查询中 Join 章节。
- 每个列表查询必须有索引与查询计划快照。
- 已发布 schema 只能追加 `.sqm`，禁止修改 `.sq`。
- 迁移测试从 v1 开始，每个版本都跑回放。

## 备选方案

- **Room KMP**：成熟，但 KSP、schema JSON、Android 驱动历史约束更重；参考项目已经证明其复杂度。放弃。
- **Exposed**：JVM-only，不适合 iOS。放弃。
- **手写 SQLite**：可控但重复劳动多，易错。放弃。
