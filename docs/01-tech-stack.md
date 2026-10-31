# 01 · 技术栈选型（按架构优先修正）

> 本文已按 [07-架构优先方案](07-architecture-first.md) 修正：保留 Kototoro 的 Room/Long ID/阅读器核心，UI 改用 MD3E，新模块用 Koin。
> 目标不是“技术更新”，而是**用最小代价复用核心功能，同时得到更好的模块边界和全新 UI**。

## 1. 约束

| 约束 | 含义 |
| --- | --- |
| 功能 parity | 核心功能照搬 Kototoro，不主动裁剪 |
| 跨平台 | Android、Desktop、iOS |
| 复用 | 能直接复制/适配 Kototoro 的 Kotlin 代码 |
| 源生态 | 运行 Mihon/Aniyomi/Tsundoku 等 JVM/Android 扩展 |
| 新 UI | Material 3 Expressive，自定义设计系统 |
| 可迁移 | 旧代码/旧 UI 可逐步替换，可回滚 |
| 性能 | 大书库、长条漫、跨章阅读不卡顿 |

## 2. 结论

| 层 | 选型 | 理由 |
| --- | --- | --- |
| 语言/UI | Kotlin Multiplatform + Compose Multiplatform | 复用 Kotlin 核心；一套 UI 三端 |
| 设计系统 | Material 3 Expressive（MD3E） | 全新视觉；组件/动效完整 |
| 数据库 | **Room KMP** | 直接复用 `core-db` schema/DAO/迁移 |
| ID | **Long + 源前缀**，值类包装 | 不破坏 parser ABI/备份/DB |
| DI | 新模块 **Koin**；旧 Hilt 隔离 | 避免 Hilt 污染新架构 |
| 导航 | Navigation Compose KMP | 类型安全、与 ViewModel/SavedState 集成 |
| 网络 | Ktor + OkHttp（Android/JVM）/ Darwin（iOS） | 共享协议；扩展宿主用 OkHttp |
| 图片 | Coil 3 | KMP、缓存、请求尺寸、Fetcher 扩展 |
| 播放器 | Media3 / libmpv / AVPlayer | 平台硬解，不做跨平台解码 |
| 扩展宿主 | Android 进程内 + Desktop 独立 JVM | 复用 Kototoro `source-host` |
| 构建 | Gradle 9.7 + AGP 9.3.1 + Kotlin 2.4.21 | 与基线一致 |
| 测试 | JUnit5 + Kotest + Turbine + Room migration | 共享/迁移/Flow |

## 3. 候选对比

| 维度 | KMP + CMP | Flutter | 三端原生 |
| --- | ---: | ---: | ---: |
| 复用 Kototoro Kotlin | **5** | 1 | 2 |
| 运行 JVM 扩展 | **5** | 2 | 3 |
| 一套 UI 三端 | **5** | 5 | 1 |
| 桌面体验 | **4** | 3 | 4 |
| iOS 性能 | 3 | 4 | **5** |
| 自绘/阅读器 | **5** | 4 | 5 |
| 单开发者成本 | **4** | 4 | 2 |

**选择 KMP + CMP。** Flutter 的 UI 一致性很好，但会丢掉 Kotlin 复用和 JVM 扩展，最终还是要 sidecar；三端原生维护成本不可接受。

## 4. 版本锁定

| 组件 | 版本 | 说明 |
| --- | --- | --- |
| JDK | 21 | Gradle/桌面 |
| Kotlin | 2.4.21 | 稳定版 |
| Compose Multiplatform | 1.12.2 | Android/Desktop/iOS |
| Material3 Expressive | `1.12.0-alpha03` | MD3E；有意接受 alpha |
| Material3 Adaptive | 1.3.0 | ListDetail/Supporting pane |
| AGP | 9.3.1 | 与 Gradle 9.7 验证 |
| Gradle | 9.7.0 | 与基线一致 |
| Android SDK | compile/target 37，min 26 | 本机已装 |
| iOS deployment | 18.0（建议） | Xcode 26.6 |
| Room | 2.8.5 | KMP |
| KSP | 2.3.11 | 与 Kotlin 兼容性待构建验证 |
| Koin | 4.2.2 | KMP DI |
| Navigation | 2.10.0 | KMP |
| Lifecycle | 2.11.0 | ViewModel/SavedState |
| Ktor | 3.6.1 | OkHttp/Darwin |
| Coil | 3.6.3 | KMP |
| kotlinx.serialization | 1.12.0 | |
| coroutines | 1.11.0 | |
| Okio | 3.9.2 | |
| Media3 | 1.11.1 | Android |
| JNA | 5.17.0 | libmpv |
| JUnit5 | 5.11.x | |
| Kotest | 5.9.1 | |
| Turbine | 1.2.1 | |

> KSP 与 Kotlin 强绑定。参考基线是 Kotlin 2.4.0 + KSP 2.3.11；升级 Kotlin 时先查官方兼容表。
> MD3E 使用 alpha 是项目决策：锁定版本、只在 `core:design` 引用、组件封装，升级单独 PR。

## 5. 关键决策

### 5.1 数据库：Room KMP，而不是 SQLDelight

- Kototoro 已有 35 张表、86 个版本、完整迁移与备份格式。
- 重写数据库会破坏 parity、备份兼容、扩展 ABI。
- Room KMP 已在 Android/JVM/iOS 验证。
- 新代码不直接用 Room 实体；通过 `core:data` 的 Repository/投影访问。

### 5.2 ID：Long + 源前缀，值类包装

- parser ABI、备份、DB 都依赖 Long；先不改。
- `ContentId(Long)`、`ChapterId(Long)`、`SourceId(String)` 提高类型安全。
- 复合 `SourceId + remoteId` 作为后续演进，不在 parity 阶段做。

### 5.3 DI：新模块 Koin，旧 Hilt 隔离

- 复制过来的 `app` 代码可能带 Hilt；只允许留在 `legacy` 适配层。
- 新 `core:*`/`feature:*` 一律构造注入，不写 Hilt 注解。
- `app:shared` 启动 Koin；平台 actual 绑定。
- CI 用 Detekt 规则禁止新模块出现 `@Hilt*`/`@Inject`。

### 5.4 UI：Material 3 Expressive

- 使用 `MaterialExpressiveTheme` + `MotionScheme.expressive()`。
- 组件：ButtonGroup、SplitButton、FABMenu、LoadingIndicator、ShortNavigationBar、NavigationRail、SearchBar、Carousel、BottomSheet。
- MD3E 的 blur/大阴影默认关闭；提供性能模式。
- 旧 UI 的玻璃拟态、多套卡片不复用。

### 5.5 源

- 直接复用 `SourceRuntime` 契约与 `core-source` 实现。
- 加 `SourceRegistry` 门面，统一内置/扩展/本地/JSON 源。
- 扩展宿主复用 `source-host`/`parser-host`：Android 进程内，Desktop 独立进程，iOS 按基线能力 parity。

### 5.6 播放器

| 平台 | 引擎 | 理由 |
| --- | --- | --- |
| Android | Media3 | 硬解/HLS/DASH/PiP |
| Desktop | libmpv | 格式/硬解/字幕 |
| iOS | AVPlayer | 系统硬解/AirPlay |

共享层只定义状态机与控制接口；视频面用平台 view 嵌入。

## 6. 平台能力

| 能力 | Android | Desktop | iOS |
| --- | --- | --- | --- |
| Compose UI | ✅ | ✅ | ✅ |
| Room | ✅ | ✅ | ✅ |
| Ktor | OkHttp | OkHttp/CIO | Darwin |
| 视频 | Media3 | libmpv | AVPlayer |
| WebView | Android WebView | WebView2/WKWebView | WKWebView |
| 文件 | SAF | AWT | UIDocumentPicker |
| 后台 | WorkManager | 协程/托盘 | BGTask |
| 安全存储 | Keystore | Keychain/Credential Manager/libsecret | Keychain |
| JVM 扩展 | ✅ 进程内 | ✅ 独立进程 | 按基线 parity |

UI 通过 `PlatformCapabilities` 判断能力，不写 `if (android)`。

## 7. 风险

| 风险 | 影响 | 缓解 |
| --- | --- | --- |
| 复制代码带入旧架构 | 架构没变好 | 只复制 core/feature 逻辑；新 UI/DI/导航新写 |
| Room 迁移/旧 schema | 数据损坏 | 保留原迁移；迁移回放；先只读验证 |
| MD3E alpha | 编译/行为变化 | 锁版本；core:design 封装；升级单 PR |
| Hilt 残留 | 新架构污染 | Detekt 规则；legacy 适配层 |
| 扩展 ABI/许可 | 运行/法务 | 许可证清单；GPL 不复制；独立进程 |
| 8GB 内存 | OOM | 3G Gradle/2G Kotlin；串行构建；CI |
| iOS CMP 性能 | 滚动卡顿 | 早测；原生播放器/WebView；避免 blur |

## 8. 本机环境

- macOS 26.6.2，Apple Silicon，8 GB RAM，36 GB 可用。
- JDK 21，Android SDK 37，Xcode 26.6。
- 建议清理到 60 GB；构建时关闭其他应用；iOS/Android 不同时构建。

## 9. 结论

**KMP + CMP + Room + Koin（新模块）+ Long ID + MD3E + 平台原生播放器。**

这套组合的核心不是“新”，而是：
- 能直接复用 Kototoro 的核心；
- 能用新模块边界隔离旧代码；
- 能用 MD3E 彻底重写 UI；
- 能在 Android/Desktop/iOS 上保持一致。
