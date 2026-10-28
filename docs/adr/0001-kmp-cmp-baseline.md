# ADR-0001：采用 KMP + Compose Multiplatform 作为目标技术栈

- 状态：条件接受（Phase 0 验证通过；iOS 播放待 App 壳验证）
- 日期：2026-10-28
- Phase 0 结果：S3 headless 三屏/1,000 项滚动通过；S1 桌面+Android 通过；S2 三端源运行时通过。继续 KMP+CMP。

## 背景

项目要做一个跨 Android / Windows / macOS / Linux（后续 iOS）的三合一阅读器，同时希望：

- 复用 Aniyomi 的 Compose 屏幕与产品信息架构；
- 共享库、进度、下载、同步、源运行时等业务逻辑；
- 使用 Material 3 Expressive，而 Compose 是 M3 的参考实现；
- 视频播放有跨平台封装（mediamp）。

候选栈：Flutter + media_kit（成熟、单 UI）、KMP + CMP、原生多端、Tauri + Web UI。

## 决策

采用 **Kotlin Multiplatform + Compose Multiplatform**：

- `commonMain` 承载模型、数据、网络、源运行时、播放器抽象、阅读器逻辑。
- `androidTarget` + JVM desktop 首发；iOS 后续。
- 播放器用 mediamp（见 ADR-0005）。
- 不做 Flutter 与 KMP 的双栈。

本决策以 S3（CMP/MD3E 迁移）和 S1（mediamp）通过为前提。

## 后果

正面：

- Aniyomi 的 Compose 屏幕与 Kotlin 领域逻辑可部分复用。
- Android 端可以直接使用 AndroidX 生态与 Aniyomi 的成熟模块。
- 一套语言、一套构建、一套数据层。

负面：

- CMP 的桌面/iOS 生态不如 Flutter 成熟；媒体、WebView、富文本等需要平台实现或替代。
- MD3E 在 CMP 的三端一致性 **待验证**。
- OkHttp/Injekt/Voyager 等 Android/JVM 依赖需要替换或 expect/actual。
- iOS 端仍受侧载、后台执行、审核限制。

## 备选方案

- **Flutter + media_kit**：跨平台媒体更成熟、UI 单栈；但放弃 Aniyomi 的 Compose 代码复用，MD3E 适配较弱，团队 Kotlin 资产无法继承。
- **原生多端**：体验上限最高，工作量约 3–5 倍，不适合当前团队规模。
- **Tauri + Web UI**：桌面强、移动端媒体与后台能力弱，不适合三媒。

## 验证

- S3：屏幕迁移成本、依赖替代、MD3E 可用性。
- S1：mediamp 三端能力。
- 任一失败：按 Phase 0 决策门 Pivot，不在错误栈上继续投入。
