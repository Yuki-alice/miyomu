# ADR-0003：SourceRuntime 契约与复合 ID

> **部分被 [ADR-0005](0005-room-and-ids.md) 修正。** SourceRuntime 契约保留；复合字符串 ID 延后，parity 阶段保留 Long + 源前缀。

- 状态：Accepted
- 日期：2026-10-31

## 背景

Kototoro 用一个 `SourceRuntime` 契约统一 Mihon、Aniyomi、TVBox、Local 等源，但使用 `Long` ID + 前缀（如 `MIHON_123`）区分来源。跨源、跨设备同步、扩展升级时，ID 可能碰撞或漂移。

## 决策

1. 使用 **`SourceId` + `remoteId` 复合键**，而不是裸 `Long`。
2. `SourceRuntime` 采用“一个实例对应一个源”的模型。
3. 核心操作固定为 6 个：`browse`、`search`、`details`、`chapters`、`pages`、`image`；偏好/正文/相关/浏览器作为可选能力。
4. 所有 DTO 可序列化，64 位数值在 JSON 中用字符串。
5. 源身份不可变；manifest 升级不得改 `SourceId`。

## SourceId 格式

```text
native:<manifest-id>
mihon:<packageName>:<sourceId>
aniyomi:<packageName>:<sourceId>
tsundoku:<packageName>:<sourceId>
jar:<pluginId>:<sourceName>
local:<uuid>            # 不同步
```

## 理由

- 复合键消除跨源碰撞。
- 字符串 ID 在 JSON、JS、SQLite、Swift 之间无损。
- 源身份稳定后才能做跨设备同步与换源。
- 一个 runtime 实例对应一个源，生命周期/限流/缓存更清晰。

## 后果

### 正面

- 同步可精确匹配内容/章节。
- 扩展升级不会因数字 ID 变化而丢失收藏。
- 适配器只需要把生态 ID 映射为字符串。

### 负面

- 索引体积略大于整数。
- 需要为每个生态定义稳定的 `SourceId` 规则。
- 旧数据（Mihon 备份）需要映射。

## 约束

- `remoteId` 缺失时用规范化 URL 的 SHA-256 前 16 字节。
- 本地源 `syncable = false`，不参与跨设备同步。
- 章节身份 = `sourceId + contentRemoteId + chapterRemoteId`；更新去重使用同一身份。
- 禁止在 UI/路由中手拼 ID；使用 `ContentKey.encode()` / `decode()`。

## 备选方案

- **裸 Long + 前缀**：简单，但跨源/同步风险高；放弃。
- **UUID 主键**：稳定，但每次解析都要维护映射表，源数据无法自然 upsert；放弃。
