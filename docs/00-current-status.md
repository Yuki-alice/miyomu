# 当前状态（2026-10-30）

> 单一事实来源。其他文档与此冲突时，以本文为准。
> 本文写于用户实机测试之后，不再使用“完成/可用/兼容”作为结论。

## 结论

- 项目**未达到可用 / 可发布状态**。用户实机测试后的结论是“功能基本上就是做做样子，在真实的使用环境下完全不可用”。
- 功能开发已暂停；打包 / 发布暂停。
- 目前没有整理出完整的用户失败清单。下一步必须按真实 UI 路径逐项复现，而不是继续加功能。

## 已验证（窄范围）

- Android 与桌面代码能编译；桌面 App 能启动窗口。
- 213 个模块 / fixture 测试通过（数量不等于可用性）；其中 `NavWalkthroughTest` 是真实点击走查。
- Venera：13 个参考源能零改构造；静态 API 审计 0 缺口；hitomi 单源 search → details → pages 在桌面与 Android 模拟器能取到数据。
- 用预置数据 + 本地 fixture 做过少量冒烟：本地 TXT、legado 发现链路、本地 MKV、设置/备份/源仓库页面。

这些只能说明特定代码路径能跑，**不能说明**普通用户能导入真实数据/源并完成阅读或播放。

## 2026-10-30 结构重构（窄范围）

- 路由已从 Voyager 迁到 **Navigation 3**：20 个页面改为 `MiyomuKey` + composable，映射集中在
  `MiyomuNavHost`；`HomeScreen` 的六个 Tab 改为内部状态。
- `:shared:ui` 不再依赖 Voyager；`:shared:presentation` 只剩 `ScreenModel`（状态持有者）。
- `:shared:ui` 从单模块（98 文件 / 13.8k 行）拆成 shell + 8 个模块：`ui-core`、`ui-library`、
  `ui-entries`、`ui-novel`、`ui-reader`、`ui-sources`、`ui-player`、`ui-settings`。依赖方向单向，
  功能模块不再互相引用，只有 shell 依赖全部。
- 数据层端口化：新增 `:shared:contract`（8 个仓储接口 + 18 个记录类型 + `DataPort`），
  `MiyomuData : DataPort`；`ui-core` 的 `api` 从 14 个模块降到 6 个，其余 11 个转 `implementation`。
  功能模块的 classpath 现在只含自己声明的模块（例如 `ui-library` 看不到 `source-novel`）。
- 顺带修掉 `InMemoryPreferenceStore` 写入丢失：`set()` 之前只写进临时实例，导入源之后立刻查不到。
- 验证范围：Android + 桌面编译通过、213 个模块测试通过，并新增 `NavWalkthroughTest` 做真实点击走查
  （六个 Tab、七个设置页、源仓库/备份/下载、漫画详情↔阅读器、番剧源详情↔播放器、逐层 pop）。
  截图见 `evidence/nav-*.png`；发现的 3 个非路由问题记在 `03-verification.md`。
- 仍未覆盖：全局搜索入口、进程重建后的回退栈恢复、真实第三方源。

## 2026-10-30 架构对齐（build-logic + app/ + 组合根）

- 新增 `build-logic/` included build：`miyomu.kmp-library`、`miyomu.kmp-compose`、
  `miyomu.android-application`、`miyomu.jvm-compose` 四个约定插件。30 个模块脚本不再重复
  `jvmToolchain` / `compileSdk` / Android / JUnit 配置。
- 入口迁移：`androidApp/` → `app/android`（`:app:android`），`desktopApp/` → `app/desktop`
  （`:app:desktop`）。
- 新增 `:shared:application` 组合根：统一构造 `MiyomuData`、`JsonPreferenceStore`、源安装/运行时、
  下载队列、小说 HTTP 客户端；平台入口只提供 `MiyomuAppEnvironment`。Android 的 SAF URI 改存
  平台 `SharedPreferences`，避免两个 `JsonPreferenceStore` 同时写 `preferences.json`。
- 启用 typesafe project accessors，构建脚本改用 `projects.shared.*`。
- 保留标准 KMP 源集 `src/<sourceSet>/kotlin`；不采用 animeko 的扁平源集，也没有新增 iOS 目标。
- 验证范围：桌面/Android 编译 + 模块 jvmTest（见 [03-verification.md](03-verification.md)）；
  **仍不是可用性证据**。

## 未验证 / 已知失败面

- **真实用户路径**：全新安装 → 导入本地书/真实源 → 搜索/详情 → 阅读/播放 → 重启 → 进度/书架/源保留。
- 用户已判定整体不可用；详细失败项待补。
- 除 hitomi 单路径外的真实第三方源、登录态、错误处理、长时稳定性。
- 性能、Windows、iOS、安装包/发布产物。
- 已观察的 UX 问题：阅读器首段文字被顶栏遮挡；本地 TXT 详情有 “Missing 1 item” 封面占位；底部 Tab 中文、小说 UI 英文。

## 下一步（按顺序）

1. **收集并复现真实失败**：用户给具体步骤/截图，或用全新数据按真实 UI 路径自查。
2. **修阻塞性可用性**：导入、源加载、搜索/详情、阅读/播放、状态持久化优先；不要先加新功能。
3. **重新定义验收**：每条功能必须走真实 UI 路径并重启复验；不再用 fixture/构造/静态审计当验收。
4. 真实路径通过之前，不安排性能、打包或新功能。

## 不要做的事

- 不要把 fixture / 构造 / 静态审计写成“功能完成”或“可用”。
- 不要在真实使用路径复现前谈发布。
- 不要用测试数量证明可用性。

## 历史文档

旧 ADR、计划、交接、清单与 Spike 文档已于 2026-10-30 删除，正文可在 git 历史 `14c495e` 查看。本文与 `01-architecture`、`02-decisions`、`03-verification` 是当前最小文档集。
