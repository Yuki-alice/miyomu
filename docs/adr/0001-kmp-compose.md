# ADR-0001：Kotlin Multiplatform + Compose Multiplatform

- 状态：Accepted
- 日期：2026-10-31
- 决策者：项目负责人

## 背景

Miyomu 需要同时覆盖 Android、Desktop 与 iOS，并尽可能复用 Kototoro 的 Kotlin 领域规则、阅读器几何与 SourceRuntime 语义。第三方源生态（Mihon / Aniyomi / Tsundoku）本质是 JVM/Android 字节码。

候选：

1. Kotlin Multiplatform + Compose Multiplatform
2. Flutter
3. 三端原生
4. React Native / Tauri

## 决策

采用 **Kotlin Multiplatform + Compose Multiplatform**，共享 UI 与业务逻辑；平台只保留入口、播放器、WebView、文件选择、安全存储、后台任务等能力。

## 理由

- Kotlin 复用：领域模型、阅读器算法、协议 DTO 可直接进入 commonMain。
- JVM 生态：Android 进程内、Desktop 独立进程均可加载 JVM 扩展；Flutter/RN 需要额外 sidecar。
- 一套 UI：避免 Kototoro 的 Android/桌面/iOS 三套页面分叉。
- 自绘能力：Compose 适合阅读器缩放、分块、手势、自定义布局。
- 工具链：与参考项目一致，迁移成本低。

## 后果

### 正面

- 页面、状态、业务规则只写一次。
- 平台适配集中，测试可共享。
- 阅读器/播放器控制层可共享。

### 负面

- iOS 的 Compose 性能与新系统适配需要持续验证。
- 原生播放器/WebView 需要 `UIKitView`/`AndroidView` 桥接。
- 团队需要熟悉 Kotlin/Native 与 Gradle KMP。

## 缓解

- M0 就做 iOS 滚动/图片基准；不达标则把 iOS UI 降级为原生，但保留共享内核。
- 视频/WebView/文件选择始终用平台原生。
- 避免全屏 blur、重阴影、复杂嵌套滚动。
- 版本锁定，升级单独 PR。

## 备选方案

- **Flutter**：UI 一致性好，但源生态与 Kotlin 复用归零，最终仍需 JVM sidecar；放弃。
- **三端原生**：体验最好，但三套 UI/阅读器/状态管理，单开发者不可维护；放弃。
- **Tauri/RN**：桌面/移动能力与自绘阅读器不匹配；放弃。
