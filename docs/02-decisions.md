# 关键决策

> 这里记录已选路线，不是实现或可用性证明。当前状态见 [00-current-status.md](00-current-status.md)。

| 决策 | 内容 | 状态 |
|---|---|---|
| 许可证 | Apache-2.0；清洁实现，不复制 GPL 代码 | 已决定 |
| 技术栈 | KMP 共享核心 + Compose Multiplatform UI | 已决定 |
| 平台顺序 | Android + 桌面优先；iOS 后置（当前无 `iosMain`） | 已决定 |
| UI 策略 | 移植 Aniyomi（Apache-2.0）的 presentation 层，按模块替换 Android 绑定点 | 已决定 |
| 扩展运行时 | 单一 JS/QuickJS 源运行时；Venera 风格读路径 | 已决定 |
| anime 源 | 自有 `AnimeSource` 格式（`search` / `detail` / `resolve`） | 已决定 |
| 小说源 | legado 书源 JSON 的独立规则求值器，不复用 QuickJS | 已决定 |
| 路由 | Navigation 3；目的地是可序列化 `MiyomuKey`，映射集中在 `MiyomuNavHost` | 已决定（2026-10-30） |
| 数据层端口 | 仓储接口与记录放 `:shared:contract`；`MiyomuData` 实现 `DataPort`，UI 只见端口 | 已决定（2026-10-30） |
| 构建约定 | `build-logic/` 提供 `miyomu.kmp-library` / `kmp-compose` / `android-application` / `jvm-compose` 约定插件；模块脚本不重复 toolchain/sdk/test 配置，依赖用 typesafe accessors | 已决定（2026-10-30） |
| 组合根 | `:shared:application` 统一装配数据、偏好、源、下载与小说抓取；平台入口只提供 `MiyomuAppEnvironment` | 已决定（2026-10-30） |
| 目录布局 | 入口放 `app/android`、`app/desktop`（`:app:*`）；库放 `shared/*`；保留标准 KMP 源集，不扁平化；iOS 仍后置 | 已决定（2026-10-30） |
| 数据层 | Room KMP，schema v4；小说书签/偏好/书源放 JSON `PreferenceStore` | 已决定 |
| 播放器 | mediamp 抽象；Android ExoPlayer，桌面 macOS/Windows MPV | 已决定 |
| 桌面 MPV 许可 | macOS/Windows 使用 LGPL 运行时；Linux GPL 构建不随应用分发 | 已决定 |
| 源仓库 | 索引 JSON + JS 源包；不内置、不推荐任何源 | 已决定 |
| 发布 | 完成度不足，打包/tag 暂停 | 已决定（2026-10-30） |

这些决策描述的是“打算怎么做/已经选了哪条路”，不代表对应功能已经可用。
