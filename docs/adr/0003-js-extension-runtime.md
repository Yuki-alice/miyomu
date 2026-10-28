# ADR-0003：以 JS + QuickJS 作为唯一扩展运行时

- 状态：接受（Phase 0 S2 三端验证）
- 日期：2026-10-28
- Phase 0 结果：JVM/Android/iOS 均跑通 JSON+HTML 源、DOM、Cookie、并发与超时；Ktor 网络层与 Cookie 持久化进入 Phase 1。

## 背景

Aniyomi 的扩展是 Android APK + extension-lib，只能在 Android 运行；跨平台目标下不可用。Venera 使用 JS 源 + QuickJS，已证明跨平台可行，且拥有现成源格式与生态。

可选方案：

- 保留 APK 扩展，Android 专用；其他平台另建 JS 引擎（双引擎）。
- 全平台 JS 源；放弃 APK 生态，接受源冷启动。
- WebView 执行 JS（安全与性能差）。
- Lua 等自定义脚本（生态为零）。

## 决策

**全平台使用 JS + QuickJS 作为唯一源运行时。**

- 引擎优先 `quickjs-kt`（Android/JVM/Native）；若不达标，vendor `quickjs-ng` + expect/actual。
- Kotlin 侧定义能力接口（Comic/Novel/Video）；JS 源通过宿主 API 实现能力。
- 首版实现 Venera 漫画源的读路径兼容（搜索/详情/章节/图片），按文档清洁实现。
- 源仓库由用户添加，不内置、不推荐。
- Android 如需过渡兼容 APK 扩展，只能作为临时层，设定移除日期；正式路径只有一个引擎。

## 后果

正面：

- 一套源跨平台运行，源作者只写一次。
- 沙箱可控：无文件系统、无原始网络、可超时/中断/限额。
- 有机会继承 Venera 的源生态，缓解冷启动。

负面：

- Aniyomi 现有 APK 源不能直接使用。
- 宿主 API、沙箱、兼容层、仓库分发都是新增工作量。
- QuickJS 版本/性能/内存问题需要持续维护。
- 视频源（hoster/提取器）复杂度高，不能假设与漫画源同构。
- Venera 是 GPL-3.0：兼容层必须清洁实现，不能复制其运行时与 `init.js`。

## 验证

S2：三端运行、读路径、超时/中断、内存、兼容 shim 清单。失败则 vendor QuickJS 或缩小源范围。

## 备选方案

- **双引擎**：Android 保留 APK 源 + 其他平台 JS。短期体验最好，长期双倍维护、能力分裂，不采用（除非过渡期且限期移除）。
- **WebView**：实现快，但难以沙箱、难以 headless、性能与一致性差。
- **仅内置解析器**：无需运行时，但无法形成源生态，不符合产品目标。
