# ADR-0005：保留 Room KMP 与 Long ID，先保 parity

- 状态：Accepted
- 日期：2026-10-31
- 取代：ADR-0002（SQLDelight）、ADR-0003 的复合字符串 ID 部分

## 背景

Miyomu 的新策略是“先改架构、功能照搬”。Kototoro 已经有一套运行中的 Room KMP 数据库（约 35 张表、86 个版本）、Long ID + 源前缀、备份格式与解析器 ABI。

此前我们选择了 SQLDelight 与 `SourceId + remoteId` 复合字符串 ID，理由是“更干净”。但这会：

- 重写 35 张表、DAO、迁移；
- 破坏 Kototoro/Mihon 备份兼容；
- 要求所有 parser 适配器改 ID；
- 把“架构重构”变成“数据层重写”，直接拖慢 parity。

## 决策

1. **数据库使用 Room KMP 2.8.x**，直接复用 `core-db` 的 schema、DAO、迁移。
2. **ID 先保留 Long + 源前缀**（`MIHON_`、`ANIYOMI_`、`LOCAL_` 等）。
3. 在新代码中用 `@JvmInline value class` 包装：`ContentId(Long)`、`ChapterId(Long)`、`SourceId(String)`，但不改变底层协议与数据库。
4. 复合 `SourceId + remoteId` 作为后续演进，不在 parity 阶段做。

## 理由

- 复用是 parity 的前提；数据库/ID 是最高风险的兼容面。
- Room KMP 已被 Kototoro 验证可在 Android/JVM/iOS 工作。
- Long ID 已被 parser ABI、备份、扩展生态依赖。
- 值类可以在不破坏底层的情况下提高类型安全。

## 后果

### 正面

- 数据库/迁移/备份直接复用，节省数月。
- 旧扩展、旧备份、旧源无需适配。
- 迁移风险集中在模块边界，而不是数据格式。

### 负面

- 保留 Room/KSP 与 schema JSON 的构建复杂度。
- Long ID 的跨源碰撞风险仍在，需要在 Repository 层用源前缀约束。
- 后续若改复合 ID，需要一次独立迁移。

## 约束

- 不在 parity 阶段修改 `MangaDatabase` 的表结构/版本。
- 新模块不得直接依赖 Room 实体；通过 `core:data` 的 Repository/投影。
- 列表查询继续遵守“只读投影、不装载完整章节”的性能规则。
- 任何 schema 变更必须新增迁移 + 回放测试。

## 备选方案

- **SQLDelight 重写**：更干净，但违背“直接复用”，放弃。
- **复合字符串 ID**：更稳，但破坏 ABI/备份，延后。
