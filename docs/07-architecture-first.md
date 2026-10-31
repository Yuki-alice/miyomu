# 07 · 修正方案：架构优先 + 功能照搬 + MD3E 新 UI

> 本文是对前六份文档的**方向修正**。前六份保留作为分析材料，但凡与本文冲突之处，以本文为准。
>
> 修正的核心：
> 1. **不裁剪核心功能**。Kototoro 的核心能力先照搬、保 parity，再谈优化。
> 2. **不做 Sumi 极简风**。UI 采用 **Material 3 Expressive（MD3E）** 全新设计。
> 3. **不是先换皮，而是先改架构**。用更好的模块/目录承载原有功能，UI 原型并行设计。

## 1. 我们之前错在哪

| 之前的结论 | 问题 | 修正 |
| --- | --- | --- |
| 为了“精简”，v1 只做原生源 + 少量功能 | 把“架构精简”误解成“功能裁剪” | 功能面先对齐 Kototoro，架构/目录变好 |
| 用 SQLDelight 重写数据库 | 与“直接复用核心实现”冲突 | 保留 Room KMP + 原 schema/DAO/迁移 |
| 用复合字符串 ID 替代 Long | 会破坏 parser ABI、DB、备份兼容 | 先保留 Long + 源前缀，值类包装，后续再演进 |
| iOS v1 不内嵌 JVM / 不做扩展 | 与“功能照搬”冲突 | 先按 Kototoro 的能力边界复用；iOS 扩展作为 parity 项推进 |
| Sumi 纸感极简 UI | 与 MD3E 方向背离 | 全新 MD3E 设计系统 |
| 先写文档、原型暂停 | 缺少并行设计 | 架构迁移与 MD3E 原型两条线并行 |

## 2. 目标

做一个**架构更好、目录更清晰、UI 全新 MD3E 的 Kototoro**：

- 功能：以 Kototoro v2.3.4 为 parity 基线，核心功能直接复用/照搬实现，不主动删减。
- 架构：把原先堆在 `app` 里的功能拆成清晰的 core / feature / platform / source 模块；依赖方向可执行。
- UI：不复用旧页面，按 MD3E 重新设计；旧 UI 只作为行为参考。
- 迁移：先架构、后 UI；UI 原型与架构迁移并行。
- 可回滚：每一步都能构建、能对比、能回退。

## 3. 两种路线与选择

### 路线 A：先换皮 UI，再改架构

- 优点：最快看到新视觉；用户可早期体验。
- 缺点：旧 `app` 的耦合、Hilt、ViewModel、平台分叉会全部被新 UI 继承；换皮后还要再改一次，返工大。
- 结论：不选。

### 路线 B：先改架构，再换 UI（采用）

- 先把核心模块从 `app` 中抽出来，建立新的模块边界与目录。
- 新 UI 只依赖新模块的接口，不依赖旧 `app` 的内部类。
- 旧 UI 暂时保留，作为行为对照；新 UI 按 feature 逐步替换。
- 并行做 MD3E 原型，但原型不阻塞架构迁移。

```text
时间 →
架构线：  baseline → 抽 core → 抽 data → 抽 feature → 接 source → 删旧壳
设计线：  MD3E tokens → 关键页原型 → 组件库 → 真机走查 → 全量替换
```

## 4. 基线策略

### 4.1 基线

- 以 `reference/Kototoro` 的 `devel` 分支、v2.3.4 为基线。
- 记录 commit：`dfcbd102c`（当前 HEAD）。
- 保留一份**只读基线**用于 diff；实际开发在 `miyomu` 仓库进行。
- 许可证：Kototoro 为 Apache-2.0；复制其代码需保留版权头与 `NOTICE`。
- 禁止直接复制 GPL/AGPL 代码（如 Kotatsu 的 GPL 部分）；只复用 Kototoro 自有 Apache-2.0 代码，或通过依赖/运行时集成。

### 4.2 仓库形态

推荐：**新仓库 + 选择性复制模块**。

```text
miyomu/
├── baseline/                 # 可选：只读的 Kototoro 基线（git submodule 或本地副本，不参与构建）
├── app/
│   ├── shared/               # KMP 组合根；产出 MiyomuShared.framework
│   ├── android/
│   └── desktop/
├── iosApp/                   # Xcode 壳，消费 app:shared framework
├── core/
│   ├── model/
│   ├── database/             # 从 core-db 复制
│   ├── network/              # 从 core-net / core-cloudflare 复制
│   ├── source/               # 从 core-source / parser-api 复制
│   ├── reader/               # 从 reader-core / reader-novel 复制
│   ├── data/                 # 从 core-domain / core-library / core-local + app 仓库层复制
│   ├── design/               # 新写：MD3E 设计系统
│   └── ui/                   # 新写：自适应壳、导航、通用组件
├── feature/
│   ├── library/              # home/favourites/history/tracker/bookmarks
│   ├── discover/             # explore/search/details/filter
│   ├── reader/               # reader/novel
│   ├── video/                # video/cloudstream 播放
│   ├── downloads/            # download
│   ├── sync/                 # backups/sync
│   ├── settings/             # settings
│   └── extensions/           # extensions/mihon/aniyomi/ireader/tsundoku UI
├── source/
│   └── host/                 # source-host/parser-host/dex-convert
├── build-logic/
├── design/                   # MD3E 原型与设计资产
└── docs/
```

为什么不直接 fork 整个仓库再原地改？

- fork 会继承 30 个模块、Hilt、ViewBinding、旧 `app` 的目录习惯，容易“改着改着又回到原样”。
- 新仓库可以只复制需要的模块，强制新边界。
- 旧代码仍可通过 `baseline/` 对照，不会丢失行为参考。

如果后续发现复制成本过高，可以退化为“fork 后原地重构”，但目录目标不变。

## 5. 目标模块结构

### 5.1 模块清单（目标 21 个，含 build-logic；当前 20 个 Gradle 模块）

| 层 | 模块 | 职责 |
| --- | --- | --- |
| App | `:app:shared` | KMP 组合根：App()、导航、Koin、平台 actual |
| App | `:app:android` | Android 入口、权限、后台任务、播放器/WebView 装配 |
| App | `:app:desktop` | Compose Desktop 入口、窗口、菜单、打包 |
| Core | `:core:model` | 内容/章节/源/进度/错误的纯模型；Long ID 值类 |
| Core | `:core:database` | Room KMP；从 Kototoro `core-db` 复制 |
| Core | `:core:network` | Ktor/OkHttp、Cookie、Cloudflare 桥、DoH |
| Core | `:core:source` | SourceRuntime、Registry、原生源引擎、扩展适配器接口 |
| Core | `:core:reader` | `reader-core` + `reader-novel` 的纯逻辑 |
| Core | `:core:data` | Repository、Room 查询、进度、更新、下载、备份、同步 |
| Core | `:core:design` | MD3E tokens、主题、基础组件、图标 |
| Core | `:core:ui` | 自适应壳、导航、通用列表/卡片/空态 |
| Feature | `:feature:library` | Home、书库、历史、更新、书签、统计入口 |
| Feature | `:feature:discover` | 浏览、搜索、详情、筛选、来源页 |
| Feature | `:feature:reader` | 漫画/小说阅读器 UI 与状态 |
| Feature | `:feature:video` | 视频播放 UI 与状态 |
| Feature | `:feature:downloads` | 下载队列、离线、CBZ 导出 |
| Feature | `:feature:sync` | 备份/恢复、WebDAV |
| Feature | `:feature:settings` | 设置、外观、阅读设置、关于 |
| Feature | `:feature:extensions` | 来源管理、扩展仓库、安装/更新、源测试 |
| Source | `:source:host` | 独立 JVM 扩展宿主；dex2jar/AndroidCompat |
| Build | `:build-logic` | convention plugins |

### 5.2 依赖规则

```text
app:android/desktop -> app:shared；iosApp 由 Xcode 消费 app:shared framework
app:shared   -> feature:*, core:ui, core:data, core:source
feature:*    -> core:ui, core:design, core:data, core:source, core:reader, core:model
core:data    -> core:database, core:network, core:source, core:model
core:source  -> core:network, core:model
core:reader  -> core:model
core:ui      -> core:design, core:model
core:design  -> 仅 Compose
core:model   -> 纯 Kotlin
source:host  -> core:source, core:model
```

硬规则：

- `core:*` 不依赖 `feature:*` / `app:*`。
- `feature:*` 不互相依赖；跨 feature 通过导航参数或 `core:model` 通信。
- 只有 `app:*` 和 `source:host` 可以依赖平台 SDK。
- `core:reader` 不依赖 Compose/Android，可在 JVM/iOS 单测。
- 每个 feature 只能通过 `core:data` 的 Repository 读写数据，不直接碰 DAO。

### 5.3 与 Kototoro 的目录映射

| Kototoro | Miyomu 目标 | 处理 |
| --- | --- | --- |
| `reader-core/` | `core/reader/` | 直接复制，改包名 |
| `reader-novel/` | `core/reader/` | 直接复制 |
| `core-db/` | `core/database/` | 直接复制，保留 schema/DAO/迁移 |
| `core-domain/` | `core/data/` + `core/model/` | 按聚合拆分 |
| `core-source/` | `core/source/` | 直接复制，补 native engine |
| `parser-api/` | `core/source/api/` | 复制需要的 DTO/ABI 垫片 |
| `core-net/` | `core/network/` | 直接复制 |
| `core-backup/` | `core/data/backup/` | 直接复制 |
| `core-library/` | `core/data/library/` | 直接复制 |
| `core-local/` | `core/data/local/` | 直接复制 |
| `core-ui/` | `core/design/` + `core/ui/` + `feature/*` | 拆：tokens/组件/页面 |
| `app/favourites`, `history`, `home`, `tracker`, `bookmarks` | `feature/library` | 复用数据/规则，重写 UI |
| `app/explore`, `search`, `details`, `filter` | `feature/discover` | 复用数据/规则，重写 UI |
| `app/reader` | `feature/reader` | 复用 `reader-core`，重写 UI |
| `app/video` | `feature/video` | 复用播放器逻辑，重写 UI |
| `app/download` | `feature/downloads` | 复用队列/存储，重写 UI |
| `app/sync`, `app/backups` | `feature/sync` | 复用协议，重写 UI |
| `app/settings` | `feature/settings` | 重写 UI，复用偏好键 |
| `app/extensions`, `mihon`, `aniyomi`, `ireader`, `tsundoku` | `feature/extensions` + `source/host` | 复用运行时，重写 UI |
| `source-host/`, `parser-host/`, `dex-convert/` | `source/host/` | 直接复制 |
| `desktop-app/`, `desktop-runtime/`, `desktop-player/` | `app/desktop` + `platform/desktop` | 复用运行时/播放器，重写 UI |
| `ios-shared/`, `iosApp/` | `app/shared` iosMain + `iosApp/` | 复用共享逻辑，重写 UI |
| `cloudstream-*` | `source/host` 可选 | 按 parity 排期 |

## 6. 技术决策修正

### 6.1 数据库：Room KMP（不是 SQLDelight）

- 直接复用 `core-db` 的 35 张表、DAO、85+ 迁移。
- 目标模块 `:core:database` 保留原 schema，包名可先保留 `org.skepsun.kototoro.core.db`，稳定后再改。
- 不在第一阶段重写数据库；重写会破坏 parity 和备份兼容。
- 只在 Repository 边界做投影/缓存/查询优化。

### 6.2 ID：先保留 Long + 源前缀

- `ContentId`/`ChapterId` 用 `@JvmInline value class` 包装 `Long`，但底层不改。
- 源前缀（`MIHON_`、`LOCAL_` 等）保留，避免 parser ABI/DB/备份不兼容。
- 复合 `SourceId + remoteId` 作为后续演进，不在 parity 阶段做。

### 6.3 DI：新模块构造注入 + Koin；旧 Hilt 不进入新代码

- `core:*` / `feature:*` 不写 Hilt 注解。
- `app:*` 用 Koin 装配新模块；旧 Android `app` 的 Hilt 代码如果复用，先隔离在 `legacy` 适配层，不扩散。
- 目标是全链路 Koin；迁移期允许 Hilt 桥接。

### 6.4 UI：Material 3 Expressive

- 共享 UI 使用 `org.jetbrains.compose.material3:material3:1.12.0-alpha03`（MD3E）。
- 使用 `MaterialExpressiveTheme`、`MotionScheme.expressive()`、`MaterialShapes`、`ButtonGroup`、`SplitButton`、`FABMenu`、`LoadingIndicator`、`ShortNavigationBar` 等。
- 接受 alpha 风险：锁定版本、封装组件、不让实验 API 扩散到业务层。
- 设计系统放 `:core:design`；页面只消费 token/组件。

### 6.5 源运行时

- 直接复用 `SourceRuntime` 契约和 `core-source` 实现。
- 新增一层 `SourceRegistry` 门面，统一内置源、扩展源、本地源、JSON 源。
- 扩展宿主复用 `source-host`/`parser-host`；Android 进程内、Desktop 独立进程、iOS 按 Kototoro 现有能力逐步 parity。

### 6.6 构建

- Gradle 9.7 + AGP 9.3.1 + Kotlin 2.4.21。
- 版本目录统一；convention plugins。
- 保留 Kototoro 的 CMake/native 依赖时，先隔离在 `platform` 模块，避免污染 core。

## 7. 迁移顺序（架构线）

### P0 · 基线与骨架（1–2 周）

- 建新仓库/目录，导入基线。
- `build-logic` + 三端壳 + CI。
- `:core:design` 的 MD3E 主题骨架。
- 许可证审计：列出可复制/需依赖/不可复制的模块。

**验收**：三端空壳可启动；基线可 diff；许可证清单完成。

### P1 · 纯核心抽取（2–3 周）

按依赖从低到高复制：

1. `reader-core` → `:core:reader`
2. `reader-novel` → `:core:reader`
3. `core-source` + `parser-api` 必要部分 → `:core:source`
4. `core-net` → `:core:network`
5. `core-backup` → `:core:data`
6. `core-domain` → `:core:model`/`:core:data`

**验收**：原模块的单测全部迁移并通过；`compileCommonMainKotlinMetadata` 通过。

### P2 · 数据库与仓库（2–4 周）

- 复制 `core-db` → `:core:database`。
- 复制 `core-library`/`core-local` 和 `app` 中的 Repository 实现 → `:core:data`。
- 用接口隔离旧 Hilt/Android 依赖。
- 保留原 schema/迁移/备份格式。

**验收**：能打开原数据库；收藏/历史/章节/进度读写通过；迁移测试通过。

### P3 · Feature 逻辑（3–5 周）

- 逐 feature 复制状态/用例（不复制 UI）：
  - library：home/favourites/history/tracker/bookmarks
  - discover：explore/search/details/filter
  - reader：reader/novel
  - downloads：download
  - sync：backups/sync
  - video：video
  - settings：preferences
- 新 ViewModel/StateHolder 只依赖 `core:data` 接口。

**验收**：每个 feature 有可运行的 headless 测试或最小 Compose 壳。

### P4 · 源与扩展（3–5 周）

- 复制 `source-host`/`parser-host`/`dex-convert`。
- Android Mihon/Aniyomi 适配。
- Desktop JVM 宿主。
- 扩展仓库/安装/偏好/源测试。
- iOS 按 Kototoro 现有能力 parity。

**验收**：Android/Desktop 能加载同一扩展；源测试通过。

### P5 · MD3E UI 全量替换（与 P1–P4 并行）

- D0：tokens/主题/基础组件。
- D1：Home/Library/Browse/Details 关键页原型 + 真机组件。
- D2：Reader/Video 控制层。
- D3：Settings/Downloads/Sync/Extensions。
- D4：桌面/平板/iOS 自适应走查。
- D5：旧 UI 删除。

**验收**：新 UI 覆盖全部 P0/P1 功能；旧 UI 入口关闭；三端视觉一致。

### P6 · Parity 与发布（2–4 周）

- 对照功能矩阵逐项验收。
- 性能/无障碍/本地化。
- 备份/同步/扩展回归。
- 打包与发布。

## 8. 并行设计线（MD3E）

设计不是“最后美化”，而是与架构并行的独立工作流：

| 阶段 | 产出 | 与架构的关系 |
| --- | --- | --- |
| D0 | MD3E token（颜色/字体/形状/动效）、主题壳 | 与 P0 同步 |
| D1 | Home/Library/Browse/Details 高保真原型 | 与 P1 同步 |
| D2 | 组件库（按钮/卡片/导航/Sheet/FAB） | 与 P2 同步 |
| D3 | Reader/Video 控件与手势 | 与 P3 同步 |
| D4 | 桌面/平板/iOS 适配 | 与 P4 同步 |
| D5 | 全量视觉走查与替换 | 与 P5 同步 |

原型先做静态高保真，再做 Compose 组件；每个组件必须有真实数据/状态，不接受“假页面”。

## 9. MD3E 设计方向（摘要）

> 详细见 [08-md3e-design.md](08-md3e-design.md)。

- **Color**：Material 3 动态色 + 品牌 seed；高饱和 primary/secondary/tertiary；surface container 分层。
- **Type**：MD3E 大字标题、强调 label、可读正文；中文用系统字体，阅读正文可选 serif。
- **Shape**：大圆角、胶囊、shape morph；MaterialShapes 用于选中/加载/空态，不滥用。
- **Motion**：`MotionScheme.expressive()` 弹簧；共享元素、容器变换、FAB 展开；支持减少动效。
- **Components**：ButtonGroup、SplitButton、FABMenu、LoadingIndicator、ShortNavigationBar、NavigationRail、SearchBar、Carousel、BottomSheet。
- **Layout**：手机底栏、平板 rail + list-detail、桌面 rail + content + supporting pane；MD3E 自适应。
- **Reader**：沉浸式、浮动工具栏、进度条、章节 sheet；控件用 MD3E，但内容区保持中性。

## 10. 功能 Parity 原则

- 以 Kototoro 的**用户可见行为**为验收标准，不以代码相同为标准。
- 优先复用：数据模型、数据库、源协议、阅读器几何、更新生命周期、备份格式、扩展运行时。
- 允许重写：UI、ViewModel/状态、导航、DI、平台装配。
- 每个功能在矩阵中标记：`copy` / `adapt` / `rewrite UI` / `later`。
- 不做“看起来像但行为不同”的页面；行为差异必须有记录。

## 11. 风险

| 风险 | 影响 | 缓解 |
| --- | --- | --- |
| 直接复制 60 万行导致“新瓶装旧酒” | 架构没变好 | 只复制 core/feature 逻辑；UI/DI/导航全部新写 |
| Room 86 迁移/旧 schema | 数据损坏 | 保留原 schema/迁移测试；先只读验证 |
| Hilt 残留扩散 | 新架构被污染 | Hilt 只允许出现在 `legacy` 适配层；CI 禁用新模块 Hilt 注解 |
| MD3E alpha API 变更 | UI 编译/行为不稳定 | 锁定版本；组件封装；实验 API 只出现在 `core:design` |
| 扩展 ABI/许可证 | 法务/运行风险 | 许可证清单；GPL 代码不复制；扩展运行时隔离 |
| iOS 扩展/性能 | parity 不达标 | 按 Kototoro 能力边界排期；先原生化关键路径 |
| 设计与迁移不同步 | 返工 | D0–D5 与 P0–P5 对齐；每周联合评审 |

## 12. 首批提交

1. `chore: create miyomu workspace and license audit`
2. `chore: add gradle wrapper, version catalog, build-logic`
3. `feat(app): three platform shells with MD3E theme`
4. `feat(core): copy reader-core and reader-novel`
5. `feat(core): copy core-source and parser contracts`
6. `feat(core): copy core-db with Room schema`
7. `feat(core): copy core-data repositories behind interfaces`
8. `feat(core): add Koin composition root`
9. `feat(design): MD3E tokens and base components`
10. `feat(ui): adaptive shell and navigation`
11. `feat(feature): port library vertical slice`
12. `ci: three-platform build and migration tests`

## 13. 需要确认

1. 基线策略：新仓库选择性复制（推荐） vs fork 后原地重构。
2. 数据库：保留 Room（推荐） vs 重写 SQLDelight。
3. ID：先保留 Long（推荐） vs 立即改复合字符串。
4. DI：新模块 Koin + 旧 Hilt 隔离（推荐） vs 全量 Hilt。
5. MD3E：接受 `material3 1.12.0-alpha03`（推荐） vs 等稳定版。
6. 桌面/ iOS 扩展：按 Kototoro parity 推进（推荐） vs 先只做 Android。

## 14. 相关文档

- [08 · MD3E 设计系统](08-md3e-design.md)
- [ADR-0005：保留 Room KMP 与 Long ID](adr/0005-room-and-ids.md)
- [ADR-0006：架构优先的 Strangler 迁移](adr/0006-architecture-first.md)
- [MD3E 原型](../design/md3e/prototype.html)
