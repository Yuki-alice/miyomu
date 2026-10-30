# 关键决策

> 这里是决策记录，不是实现或可用性证明。旧 ADR 正文已删除，可在 git 历史 `14c495e` 查看。

| 决策 | 内容 | 状态 |
|---|---|---|
| 许可证 | Apache-2.0；清洁实现，不复制 GPL 代码 | 已决定 |
| 技术栈 | KMP 共享核心 + Compose Multiplatform UI | 已决定 |
| 平台顺序 | Android + 桌面优先；iOS 后置（当前无 `iosMain`） | 已决定 |
| UI 策略 | 移植 Aniyomi（Apache-2.0）的 presentation 层，按模块替换 Android 绑定点 | 已决定 |
| 扩展运行时 | 单一 JS/QuickJS 源运行时；Venera 风格读路径 | 已决定 |
| anime 源 | 自有 `AnimeSource` 格式（`search` / `detail` / `resolve`） | 已决定 |
| 小说源 | legado 书源 JSON 的独立规则求值器，不复用 QuickJS | 已决定 |
| 数据层 | Room KMP，schema v4；小说书签/偏好/书源放 JSON `PreferenceStore` | 已决定 |
| 播放器 | mediamp 抽象；Android ExoPlayer，桌面 macOS/Windows MPV | 已决定 |
| 桌面 MPV 许可 | macOS/Windows 使用 LGPL 运行时；Linux GPL 构建不随应用分发 | 已决定 |
| 源仓库 | 索引 JSON + JS 源包；不内置、不推荐任何源 | 已决定 |
| 发布 | 完成度不足，打包/tag 暂停 | 已决定（2026-10-30） |

这些决策描述的是“打算怎么做/已经选了哪条路”，不代表对应功能已经可用。当前状态见 [00-current-status.md](00-current-status.md)。
