# Miyomu 目标模块与架构设计（Draft v0.4）

> **状态**：设计参考；2026-10-31 第一轮架构重构（非模块级迁移）已实施并验证；Phase 0–4 目标模块迁移仍未批准。
> **2026-10-31 更新**：用户已批准第一轮架构重构优化（审阅报告的“立刻 + 本迭代”档：导航恢复、
> 服务容器、domain 去 Compose、Voyager 移除、主题去重、api 边界、CI 门禁），已实施并验证，
> 见 `docs/architecture-refactor-2026-10-31.md`。本文的 Phase 0–4 目标模块迁移**仍未批准执行**，
> 其中的 AR-01/02/05 止血项与 CI lint 已提前完成。
> **日期**：2026-10-30（2026-10-31 状态同步）
> **已确认约束**：
> - 定位：Mihon-like 漫画库 + 强本地能力 + 视频/小说一等公民。
> - 平台：Android/Desktop 同等优先。
> - 来源：本地 + 可安装源；源为可选插件。
> - 改造力度：2026-10-31 批准“立刻 + 本迭代”档重构；Phase 0–4 模块级迁移仍未批准。
> **输入**：`docs/product-goals.md`、`docs/architecture-review-2026-10-30.md`、当前 34 个模块（新增 `shared:track`）的实际依赖图。

---

## 1. 设计原则

1. **Mihon 基线**：漫画库、源/扩展、下载、备份、阅读设置、历史/更新，以 Mihon 的成熟体验为参照。
2. **本地做深**：本地格式、导入、元数据、封面、分类、离线、备份/同步比 Mihon 更强；在线源失败不阻塞本地功能。
3. **双端同等**：Android 与 Desktop 是同一功能矩阵的一等公民；差异必须显式记录，不能把 Desktop 当调试壳。
4. **按能力拆分，不按技术层拆分**：目标是 `feature:*` 能独立演进，而不是再增加一层 `ui-*` / `presentation-*`。
5. **依赖单向**：`app → feature → core`，feature 之间不互相依赖，core 之间不反向依赖 feature。
6. **领域层不依赖 UI**：`core:model`、`core:data`、`core:source-api` 不出现 Compose。
7. **状态可预览、可测试**：每个 screen 有 plain state holder + immutable UiState；不把整个服务图传给 UI。
8. **长生命周期服务有明确 owner**：数据库、HTTP、下载、源运行时由 `app:application` 持有并关闭。
9. **渐进迁移、不过度抽象**：每个 Phase 都可发布；iOS/TV 是 P2，不因此增加当前复杂度。

---

## 2. 目标模块图

```
app:android          app:desktop
      \                  /
       \                /
        app:application            // AppContainer、服务生命周期、平台环境
               |
          app:shell                // MiyomuApp、NavHost、Home、路由注册
               |
   ┌───────────┼────────────┬──────────────┬─────────────┐
 feature:library  feature:details  feature:sources  feature:downloads
   │                │                │                │
 feature:reader-comic  feature:reader-novel  feature:player  feature:settings
   └───────────┴────────────┴──────────────┴─────────────┘
               |
            core:*
```

### 2.1 core 模块

| 模块 | 职责 | 允许依赖 |
|---|---|---|
| `core:model` | 纯 Kotlin 领域模型：MediaType、LibraryItem、Chapter、Episode、Progress、Paged | 无（仅 kotlinx） |
| `core:common` | Result、时间、日志、Dispatcher、错误类型 | `core:model` |
| `core:preferences` | PreferenceStore 抽象与实现；无 Compose | `core:model` |
| `core:database` | Room 实体、DAO、迁移、事务 | `core:model` |
| `core:network` | Ktor client、Cookie、WebDAV、限流/重试 | `core:common` |
| `core:data` | 数据端口（DataPort、LibraryRepository、ProgressRepository 等）与实现 | `core:model`、`core:database` |
| `core:library` | 本地库扫描、格式解析（目录/ZIP/CBZ/7z/CB7/PDF/TXT/EPUB/视频）、封面、元数据、章节发现 | `core:model`、`core:common` |
| `core:reader` | 阅读引擎：分页、进度、书签、字符偏移、阅读会话 | `core:model`、`core:preferences` |
| `core:source-api` | `ComicSource`、`AnimeSource`、`NovelSource` 接口与 DTO | `core:model` |
| `core:source-runtime` | QuickJS/HTML 桥、源加载、超时/内存/响应限制 | `core:source-api`、`core:network` |
| `core:source-manager` | 已安装源、源仓库、安装/更新/卸载、健康状态 | `core:source-api`、`core:source-runtime` |
| `core:download` | 下载队列、断点续传、存储布局 | `core:source-manager`、`core:network` |
| `core:player` | mediamp 抽象、播放后端选择、音轨/字幕/倍速 | `core:model` |
| `core:danmaku` | 弹弹play 客户端、弹幕模型 | `core:network` |
| `core:sync` | 备份、恢复、WebDAV 双向同步、冲突策略 | `core:data`、`core:network` |
| `core:track` | 追踪客户端（AniList/Bangumi）与逐书绑定存储 | `core:model`、`core:network` |
| `core:i18n` | Moko resources/字符串目录 | 无 |
| `core:ui` | Design system、主题、通用组件、图标、导航原语 | `core:model`、`core:i18n` |

> `core:source-manager` 可以并入 `core:source-runtime` 或 `feature:sources`，取决于是否希望“源安装”独立于 UI。建议保留，便于测试和后台任务。

### 2.2 feature 模块

| 模块 | 职责 | 主要依赖 |
|---|---|---|
| `feature:library` | Mihon 式书架（漫画优先）+ 小说/视频书架、分类、筛选、排序、更新、历史、本地库整理 | `core:library`、`core:data`、`core:ui` |
| `feature:details` | 漫画/动画/小说详情、章节/剧集列表、元数据编辑 | `core:data`、`core:source-*`、`core:ui` |
| `feature:reader-comic` | 漫画阅读器 UI、翻页/Webtoon、阅读设置 | `core:reader`、`core:library`、`core:ui` |
| `feature:reader-novel` | 小说阅读器 UI、分页/书签/主题 | `core:reader`、`core:library`、`core:ui` |
| `feature:player` | 视频播放页、控制栏、弹幕开关、下一集 | `core:player`、`core:danmaku`、`core:ui` |
| `feature:sources` | Mihon 式扩展管理、源仓库、浏览、全局搜索、源错误诊断 | `core:source-manager`、`core:source-runtime`、`core:ui` |
| `feature:downloads` | 下载队列、存储管理、批量操作 | `core:download`、`core:ui` |
| `feature:settings` | 外观、阅读、下载、数据、备份、弹幕、追踪、关于 | `core:preferences`、`core:sync`、`core:ui` |

### 2.3 app 模块

| 模块 | 职责 |
|---|---|
| `app:application` | AppContainer（服务图）、生命周期、平台环境接口、长生命周期 scope/client |
| `app:shell` | MiyomuApp、NavHost、Home 六 Tab、主题、路由注册 |
| `app:android` | Activity、SAF、Android 数据库工厂、ExoPlayer 后端、分享/deep link |
| `app:desktop` | main、窗口、目录选择、MPV 后端、桌面快捷键 |

### 2.4 Mihon 基线与“视频/小说一等公民”

| 能力 | Mihon 基线 | Miyomu 目标 | 当前状态 |
|---|---|---|---|
| 书架与分类 | 有 | 保留并扩展到小说/视频 | 基本可用 |
| 源/扩展管理 | 有 | 保留，增加源健康与错误诊断 | 基本可用 |
| 下载 | 漫画章节 | 漫画 + 小说 + 视频 | 漫画/小说/在线视频可用 |
| 备份/恢复 | 有 | 保留，增加增量/冲突/偏好 | 可用（WebDAV 双向） |
| 阅读设置 | 有 | 保留并统一漫画/小说 | 基本可用 |
| 历史/更新 | 有 | 保留，覆盖三类媒体 | 基本可用 |
| 追踪 | 有（可选） | AniList + Bangumi，默认关闭；不做 MAL | 已实现（AniList + Bangumi） |
| 本地库深度 | 一般 | 格式、元数据、封面、健康检查 | 7z/PDF/MOBI 已实现；元数据/封面可扩展 |
| 视频 | 无 | 本地/在线播放、下载、弹幕、进度 | 播放与在线按集下载可用 |
| 小说 | 无 | 本地/在线阅读、下载、书签、进度 | 阅读与章节缓存可用 |

结论：架构必须让 **漫画是基线、视频/小说是一等公民、本地能力是差异化**，而不是把视频/小说塞进漫画的 `ComicSummary` 模型里。

### 2.5 Android / Desktop 对等矩阵

| 能力 | Android | Desktop | 备注 |
|---|---|---|---|
| 本地库根 | SAF 目录 | 文件系统目录 | 都要支持切换与持久化 |
| 数据库 | Room | Room | 同一 schema |
| 漫画/小说阅读 | 支持 | 支持 | 同一 core:reader |
| 视频播放 | ExoPlayer | MPV（macOS/Windows；Linux 不支持） | 已确认维持现状；UI 明确提示 |
| 弹幕 | 支持 | 支持（依赖播放后端） | 不支持时明确提示 |
| 下载 | 前台/后台策略待定 | 前台下载 | 漫画/小说/在线视频可用；后台策略待定 |
| 分享/打开方式 | Android Share/VIEW | 命令行/文件关联 | 已实现最小切片；deep link 待立项 |
| 发布 | APK/AAB | 原生分发包 | 当前 CI 只编译，未打包 |
| 测试 | jvmTest + 仪器测试（待补） | Compose Desktop 测试 | 双端都要进 CI |

> “同等优先”不等于“所有后端完全一致”；Linux 视频是唯一已确认例外，差异必须在功能矩阵和 UI 中明确，而不是静默缺失。

---

## 3. 依赖规则

```text
app:android ─┐
app:desktop ─┼─> app:shell ─> feature:* ─> core:*
             └─> app:application ─> core:*
```

硬规则：

- `feature:*` **不得**依赖另一个 `feature:*`；共享 UI 放 `core:ui`，共享逻辑放对应 `core:*`。
- `core:*` **不得**依赖 `feature:*` 或 `app:*`。
- `core:model`、`core:data`、`core:source-api` **不得** import `androidx.compose.*`。
- `feature:*` **不得**直接 import Room/DAO；只能通过 `core:data` 的端口。
- `feature:*` **不得**直接 import QuickJS；只能通过 `core:source-api` / `core:source-manager`。
- 新代码包名统一 `app.miyomu.*`；移植代码放 `app.miyomu.ported.*`，不得新增 `tachiyomi` / `eu.kanade` 包根。
- `app:application` **不得**依赖 `app:shell`；由 shell 依赖 application。
- 所有跨模块公开签名中的类型，必须在构建文件中以 `api` 声明；禁止靠传递暴露。

建议用 Konsist 或 Gradle dependency-analysis 把上述规则变成测试，而不是只写在文档里。

---

## 4. 当前模块 → 目标模块映射

> 这是迁移示意，不是 1:1 重命名；目标是收敛到约 20–22 个模块，避免把当前 34 个模块原样改名为 34 个新名字。

| 当前模块 | 目标归属 | 说明 |
|---|---|---|
| `app:android` / `app:desktop` | `app:android` / `app:desktop` | 保留 |
| `shared:application` | `app:application` | 组合根；graph 从 `ui-core` 移入 |
| `shared:ui` | `app:shell` | MiyomuApp/NavHost/Home |
| `shared:model` | `core:model` | 纯模型 |
| `shared:contract` | `core:data`（端口） | `Category` 移入 `core:model` |
| `shared:domain` | 拆分 | 纯模型 → `core:model`；偏好 → `core:preferences`；源接口 → `core:source-api`；UI 适配器 → `core:ui` |
| `shared:data` | `core:database` + `core:sync` | Room 与备份分离 |
| `shared:local` + `shared:novel` | `core:library` | 扫描、解析、封面、格式 |
| `shared:reader-comic` / `shared:reader-novel` | `core:reader` + `feature:reader-*` | 引擎与 UI 分离 |
| `shared:network` | `core:network` | |
| `shared:source-api` | `core:source-api` | |
| `shared:source-runtime` | `core:source-runtime` | |
| `shared:source-manager` / `source-repo` / `source-novel` | `core:source-manager` + `feature:sources` | 安装/仓库/规则 |
| `shared:download` | `core:download` + `feature:downloads` | |
| `shared:danmaku` | `core:danmaku` | |
| `shared:track` | `core:track` | AniList/Bangumi 客户端与绑定存储 |
| `shared:player` | `core:player` + `feature:player` | |
| `shared:i18n*` | `core:i18n` | 合并或保留双模块 |
| `shared:ui-core` | `core:ui` + `app:application` + `app:shell` | graph/nav/theme 三分 |
| `shared:presentation-core` | `core:ui` | 设计系统 |
| `shared:presentation` | `core:ui-ported`（临时）+ `feature:*` | 按 screen 逐步迁出；先删重复主题 |
| `shared:ui-entries` | `feature:details` | |
| `shared:ui-library` | `feature:library` | |
| `shared:ui-novel` | `feature:reader-novel` + `feature:details` | |
| `shared:ui-reader` | `feature:reader-comic` | |
| `shared:ui-sources` | `feature:sources` | |
| `shared:ui-player` | `feature:player` | |
| `shared:ui-settings` | `feature:settings` | |

---

## 5. 关键架构决策

### 5.1 组合根与依赖注入

- `app:application` 构建 `AppContainer`，持有数据库、HTTP client、源管理器、下载队列、同步服务、长生命周期 `CoroutineScope`。
- `AppContainer` 只暴露**窄接口**：
  - `LibraryPort`、`ProgressPort`、`SourceSearchPort`、`DownloadPort`、`SyncPort`、`PlayerPort`。
  - 不把 20 个具体仓库塞进一个 `MiyomuGraph`。
- 每个 feature 提供 `XxxRoute(container/ports)`，由 `app:shell` 注入。
- 容器实现 `Closeable`；数据库/HTTP/下载/源运行时在 Android Activity 重建时不销毁。

### 5.2 状态与 effect

- 每个 screen 一个 plain `XxxStateHolder`：
  - `StateFlow<XxxUiState>`；
  - `onEvent(event)`；
  - 构造参数只接收需要的 port；
  - 不持有 `MiyomuGraph`。
- Compose screen 分为：
  - `XxxRoute`：负责收集状态、绑定 effect、导航；
  - `XxxContent`：预览/测试友好的 immutable state + callbacks。
- 禁止在 composition 中创建长生命周期服务；禁止主线程 `runBlocking`。
- 导航 back stack 使用保存型实现；NavKey 只携带身份。

### 5.3 导航

- `app:shell` 拥有唯一的 `NavDisplay` 和 `entryProvider`。
- 每个 feature 暴露 `@Serializable` route key + `entry` 注册函数，不直接操作全局导航。
- `feature:*` 不 import 其他 feature 的 key；共享 key 放 `app:shell` 或 `core:ui` 的导航原语。
- Deep link 由 `app:android` 解析并映射到 key；`app:desktop` 支持命令行/文件打开。

### 5.4 数据与同步

- `core:data` 定义端口，`core:database` 提供 Room 实现；`feature:*` 只依赖端口。
- `core:sync` 负责备份/恢复/WebDAV 双向/冲突（Q7 已确认）；`feature:settings` 只做 UI。
- 备份格式版本化；增加可选加密；偏好纳入同步（当前 v1 未包含）。
- 本地库是 source of truth；在线源数据只缓存必要字段。

### 5.5 源插件

- `core:source-api`：`ComicSource` / `AnimeSource` / `NovelSource` 接口，纯 Kotlin。
- `core:source-runtime`：QuickJS、HTML DOM、网络桥、超时/内存/大小限制。
- `core:source-manager`：安装、仓库、版本、哈希、健康状态。
- `feature:sources`：浏览、搜索、错误展示、安装 UI。
- 新源类型（Legado、Venera、Apple CMS）作为 adapter，不进入 UI 模块。
- 安全边界：源脚本只获得显式授权的网络/Cookie/KV；不暴露文件系统与任意反射。

### 5.6 阅读与播放

- `core:reader` 只处理阅读会话、分页、进度、书签、字符偏移；不依赖 Compose。
- `feature:reader-comic` / `feature:reader-novel` 负责 UI 与交互。
- `core:player` 包装 mediamp；`feature:player` 负责控制栏、弹幕、下一集。
- 平台差异（ExoPlayer vs MPV）留在 `core:player` 的 actual/backend 选择层。

### 5.7 UI 与移植代码

- `core:ui` 是唯一设计系统：主题、排版、组件、图标、预览工具。
- 移植的 Mihon/Aniyomi 代码先放 `core:ui-ported` 或保留在原模块，但**不得**成为 feature 的公共依赖。
- 新代码包名统一 `app.miyomu.core.*` / `app.miyomu.feature.*` / `app.miyomu.app.*`。
- 移植代码逐步迁到 `app.miyomu.ported.*` 或删除；不再新增 `tachiyomi` / `eu.kanade` 包根的新文件。

---

## 6. 三种改造方案

| 方案 | 内容 | 成本 | 风险 | 本轮结论 |
|---|---|---|---|---|
| **A. 渐进式收口** | 保留当前 34 个模块，只修 `api`/`implementation`、domain Compose、`ui-core` 再导出、导航/生命周期 | 1–2 周 | 低 | 未选择；可作为未来止血备选 |
| **B. 目标态 + 分阶段迁移** | 定义 20–22 个目标模块，按 vertical slice 迁移，每阶段可发布 | 2–3 个月 | 中 | **设计参考；本轮不执行** |
| **C. 全面重写/重命名** | 按目标架构重建工程，直接迁移代码 | 3–6 个月 | 高 | 未选择 |

**本轮结论**：按用户决策“只出文档”，A/B/C 均不执行。若未来批准实施，推荐 B：先把 P0 运行时问题止血，再按“本地库 → 阅读器 → 源/下载/播放器 → 设置/同步”的顺序迁移；每个 Phase 都保留旧模块作为 facade，避免大爆炸。

---

## 7. 未来迁移路线（未批准执行）

> 以下 Phase 仅作为未来实施参考；本轮“只出文档”，不执行任何代码/模块变更。

### Phase 0：决策与止血（3–5 天）

- 产品级决策已确认；Phase 0 执行 ADR 与剩余验收（不等待产品决策）。
- 写 ADR：目标模块图、依赖规则、包名策略、移植代码边界。
- 修复 AR-01（导航保存/服务生命周期）、AR-02（主线程 `runBlocking`）、AR-05（graph 泄漏）。
- 加架构规则测试雏形（Konsist/ArchUnit）与 CI lint。

**退出标准**：旋转不丢页、下载不中断；规则测试能拦截 `domain → Compose`。

### Phase 1：core 抽取（1–2 周）

- 新建 `core:model`、`core:common`、`core:preferences`、`core:network`、`core:database`、`core:i18n`。
- `domain` 去 Compose；`contract` 改为 `core:data` 端口；`Category` 移到 `core:model`。
- 旧模块保留，作为新 core 的 facade/适配层。
- 删除重复主题、未使用依赖。

**退出标准**：`domain` 无 Compose import；所有现有测试通过；依赖图无环。

### Phase 2：本地库与阅读器切片（2–4 周）

- 新建 `core:library`、`core:reader`。
- 新建 `feature:library`、`feature:details`、`feature:reader-comic`、`feature:reader-novel`。
- 先迁移“本地漫画/小说”完整链路：扫描 → 书架 → 详情 → 阅读 → 进度。
- 视频本地链路可暂留旧 UI，作为下一阶段。

**退出标准**：本地漫画/小说全流程通过；旋转/返回/进程死亡不丢位置；扫描 1,000 文件达到性能目标。

### Phase 3：源、下载、播放器（2–4 周）

- 新建 `core:source-api`、`core:source-runtime`、`core:source-manager`、`core:download`、`core:player`、`core:danmaku`。
- 新建 `feature:sources`、`feature:downloads`、`feature:player`。
- 迁移源仓库、全局搜索、Aniyomi/Legado 源、漫画下载、视频播放/弹幕。
- 保留旧 source 测试作为 golden tests。

**退出标准**：安装/搜索/阅读/下载/播放全链路通过；源失败隔离；下载断点恢复。

### Phase 4：收尾与重命名（1–2 周）

- 删除 `ui-core` 的再导出；拆分/删除 `presentation` 重复部分。
- 把仍被使用的移植 UI 收敛到 `core:ui-ported`，逐步迁入 feature。
- 包名统一：新代码 `app.miyomu.*`；移植代码 `app.miyomu.ported.*` 或明确保留。
- 删除旧模块/facade；CI 增加 architecture tests、lint、canary。
- 更新 `AGENTS.md`、架构文档与 onboarding 指南。

**退出标准**：目标模块图落地；无 feature→feature；无 domain→Compose；双端构建通过；测试覆盖关键路径。

---

## 8. 风险与缓解

| 风险 | 影响 | 缓解 |
|---|---|---|
| 源兼容性回归 | 用户源失效 | 保留 Aniyomi/Legado/Venera golden tests；每个 Phase 跑真实源夹具 |
| 移植 UI 迁移量大 | 进度拖延 | strangler 模式：旧 UI 保留，新 feature 逐屏替换 |
| Room 迁移 | 数据丢失 | 保留 schema 历史；每版迁移测试；备份/恢复回归 |
| 模块拆分过细 | 维护成本上升 | 目标 20–22 个模块；feature 不再按技术层细分 |
| 包名重命名 | diff 巨大、冲突 | 放到 Phase 4；先建 `ported/` 边界 |
| 平台播放后端许可 | Linux 不可用 | 继续排除 Linux MPV；在 UI 明确支持矩阵 |
| 产品范围膨胀 | 无法交付 | 以 P0 为合同；P1/P2 单独立项 |
| 个人维护精力 | 源生态/同步维护成本 | 在线源作为可选插件；WebDAV 优先于自建云 |

---

## 9. 目标态验收（未来实施时使用）

- [ ] `core:model`、`core:data`、`core:source-api` 无 Compose import。
- [ ] 无 `feature:* → feature:*` 依赖。
- [ ] `MiyomuGraph` 被 `AppContainer` + 窄端口取代；screen 不再接收全量服务图。
- [ ] 导航 back stack 可保存/恢复；旋转、进程死亡、返回键有测试。
- [ ] 长生命周期服务由 `app:application` 持有并关闭；无未管理 scope/client。
- [ ] 本地漫画/小说/视频、源搜索/阅读/下载/播放、备份/恢复全链路测试通过。
- [ ] Android + Desktop 构建通过；平台差异有文档。
- [ ] 新代码包名统一为 `app.miyomu.*`；移植代码使用 `app.miyomu.ported.*` 并有明确边界与来源说明。
- [ ] CI 包含 `jvmTest`、`lintDebug`、静态分析、架构规则测试。
- [ ] 现有 72 个测试全部通过，新增关键路径测试。

---

## 10. 决策状态与待决问题

### 10.1 已确认（2026-10-30 / 10-31）

- 产品定位：Mihon-like 漫画库 + 强本地能力 + 视频/小说一等公民。
- 平台：Android/Desktop 同等优先。
- 来源：本地 + 可安装源；源为可选插件。
- 改造力度：2026-10-31 批准“立刻 + 本迭代”档重构；Phase 0–4 模块级迁移仍未批准。
- 分发范围：暂不规划。
- 外部迁移导入：不做 Mihon/Aniyomi/Legado 备份兼容。
- 同步：WebDAV 双向（P1-5 已实现）。
- 追踪：AniList + Bangumi，默认关闭；不做 MAL。
- Desktop 视频：维持 macOS/Windows MPV；Linux 不做。
- 包名：统一 `app.miyomu.*`；移植代码用 `app.miyomu.ported.*` 过渡。

### 10.2 仍待确认（目标架构实施细节）

1. `presentation` 是保留为 `core:ui-ported` 过渡，还是在 Phase 2 就按 screen 拆完？
2. `core:source-manager` 是否独立，还是并入 `core:source-runtime`？
3. 目标模块数量是否控制在 20–22 个，还是允许更多细粒度模块？
4. 包名迁移的落地顺序与 `ported/` 边界。
5. 追踪令牌存储/隐私边界与默认关闭策略（AniList + Bangumi）。
6. 外部源仓库/扩展签名与安全模型。
7. P2 项（视频混流/字幕、词典/翻译、生态项）的立项顺序。

> 产品级决策已确认；这些问题会改变未来 Phase 的边界，但不阻塞当前代码。
