# S3 报告：CMP + MD3E 屏幕迁移验证

- 状态：**验证完成（headless UI 截图 + 1,000 项滚动）**
- 日期：2026-10-28
- 时间盒：6 个工作日（已用约 1 天）
- 结论：**Conditional Go** —— CMP + MD3E 可行；需锁 alpha 依赖并完成依赖替换

## 1. 结论摘要

三屏桌面样例可编译、可启动；依赖审计完成初版。关键发现：**CMP 1.12.2 默认解析的 Material3 1.9.0 不含 Expressive API**，需要显式依赖 `org.jetbrains.compose.material3:material3:1.12.0-alpha03` 才能使用 MD3E；该 alpha 已通过编译验证（`LoadingIndicator`），但属于 alpha 风险。桌面锁屏期间通过 headless Compose UI 测试完成三屏语义/截图验证，并将 1,000 项网格滚动到末尾耗时记录为 158ms（测试场景，非 GPU 帧率）。

## 2. 环境

| 项目 | 实际值 |
|---|---|
| 机器 | macOS 26.6.2（aarch64） |
| JDK | Corretto 21.0.12 |
| Gradle | 9.4.1（S3 测试时；当前根 wrapper 已升到 9.6.0） |
| Kotlin 插件 | 2.4.21 |
| Compose Multiplatform | 1.12.2 |
| Material3 | 显式 `1.12.0-alpha03`（默认访问器为 1.9.0） |
| 模块 | `:spikes:s3-ui`（JVM desktop） |
| 构建 | `./gradlew :spikes:s3-ui:build` 通过 |
| 启动 | `./gradlew :spikes:s3-ui:run` 创建窗口（标题已确认） |

## 3. 已完成

- 三屏骨架：Library（搜索/筛选/自适应网格/1,000 项压力模式）、Detail（封面/元数据/章节列表）、Settings（分组/开关/单选/Expressive 预览）。
- 导航（NavigationRail）、深浅色主题、Material 3 基础组件。
- Gradle 版本目录 + wrapper；仓库文档与 Spike 目录。
- MD3E 依赖 override + `LoadingIndicator` 编译验证。
- 依赖审计初版（见第 4 节）。

## 4. 依赖审计（初版）

| 依赖 | Aniyomi 用途 | CMP 可行性 | 结论/行动 |
|---|---|---|---|
| Voyager `2.2.21-1.10.3` | 导航 | 官方支持 Android/iOS/Desktop/Web；Aniyomi 是 fork，需实测 | 条件可用；Phase 1 先用最小导航验证，失败则换 CMP Navigation 或自写栈 |
| Injekt（Mihon fork） | DI | **未验证**；倾向仅 JVM/Android | 计划替换为 Koin 或手工注入，迁移时逐模块改 |
| OkHttp 5.4 | 网络 | JVM/Android 可用；iOS 无 Native 支持 | 共享层用 Ktor（Darwin/CIO/OkHttp 引擎），或 expect/actual |
| Coil 3.1 | 图片 | KMP 支持 | 可直接评估；注意桌面/iOS 解码器 |
| SQLDelight 2.0.2 | 数据库 | KMP 支持 | 保留，补 JVM/Native driver，升级版本另行评估 |
| Compose WebView | 内嵌网页 | Android 专属；需平台实现 | expect/actual（Android WebView / iOS WKWebView / 桌面 CEF/JCEF 或移除） |
| RichText | Markdown | CMP 兼容但**无 iOS** | Android/桌面可用；iOS 用简化 Markdown 渲染或移除 |
| Material Icons Extended | 图标 | 可用但被固定在 1.7.3、已废弃 | Phase 1 改用 Material Symbols 或最小向量集 |
| AndroidX Material3 | 设计系统 | CMP 有 material3，但稳定版不含 Expressive | 显式 pin alpha（见第 5 节） |

## 5. MD3E 可用性

| 组件 | 1.9.0 稳定版 | 1.12.0-alpha03 | 验证状态 |
|---|---|---|---|
| `ExperimentalMaterial3ExpressiveApi` | 无 | 有 | 已确认 |
| `LoadingIndicator` | 无 | 有 | **已编译并接入 Settings** |
| `ButtonGroup` / `ButtonGroupItem` | 无 | 有 | 类存在，未使用 |
| `FloatingActionButtonMenu` | 无 | 有 | 类存在，未使用 |
| 其他 Expressive 组件 | 无 | 部分 | 未逐项验证 |

结论：**MD3E 条件可用**。代价是依赖 alpha artifact、与 CMP 稳定版的版本线不一致；Phase 1 需决定"锁 alpha + 自担升级风险"还是"稳定 M3 + 关键组件自实现"。

## 6. Headless UI 验证（2026-10-28）

使用 `runComposeUiTest` 在离屏场景中加载 `App()`，完成以下断言并截取 PNG：

- Library：12 条 mock 数据、搜索框、筛选 chip 可见。
- Detail：点击卡片进入，标题/作者/tag/章节列表可见。
- Settings：导航切换、Dark theme / E-ink / preload / sources 文案可见。
- Stress：切换 1,000 项后滚动到 index 999，`Stress Title 1000` 可见；滚动耗时 **158ms**。

截图证据：

- [Library](evidence/s3-library.png)
- [Detail](evidence/s3-detail.png)
- [Settings](evidence/s3-settings.png)
- [Stress 1,000](evidence/s3-stress.png)

命令：`./gradlew :spikes:s3-ui:test`（1 test / 0 failures）。

限制：这是离屏测试场景，不是锁屏桌面上的真实窗口；帧率/GPU 渲染仍未测。每屏迁移耗时以“从零实现三屏”为代理（约 0.5 人日内完成 spike 质量），低于 1.5 人日/屏的预算；尚未对 Aniyomi 真实屏幕做逐行移植计时。

## 7. 已知问题

| ID | 问题 | 影响 | 处理 |
|---|---|---|---|
| S3-1 | `compose.material3` 访问器解析 1.9.0，无 Expressive | MD3E 不可用 | 已改为显式 `material3:1.12.0-alpha03` |
| S3-2 | `materialIconsExtended` 固定 1.7.3 且废弃 | 旧依赖、无更新 | Phase 1 替换图标方案 |
| S3-3 | 桌面锁屏导致无法截图/交互 | 视觉验收阻塞 | 解锁后补测 |
| S3-4 | material3 alpha 与 CMP 1.12.2 非同一版本线 | 潜在运行时/API 风险 | 启动冒烟 + Phase 1 锁版本 |

## 8. 待完成

- 解锁桌面后的真实窗口/帧率检查；headless 截图已覆盖视觉语义。
- 内存、窗口缩放、键盘导航等交互质量。
- 对 Aniyomi 真实屏幕做一次移植计时（本报告的 0.5 人日/三屏只是代理）。
- iOS/Android 的依赖可行性（本 Spike 目前只做桌面）。
- Phase 1 依赖替换：Voyager/Injekt/OkHttp/WebView/RichText 的最终方案。

## 9. 结论

**Conditional Go**：

- 三屏在 CMP 上可运行/可交互，headless 截图和语义断言通过。
- 1,000 项网格可滚动到末尾（158ms，测试场景），未发现阻断性性能问题。
- MD3E 依赖 alpha artifact，需在 Phase 1 锁定版本并评估升级路径。
- 依赖替换与真实 Aniyomi 屏幕移植是 Phase 1 的前置工作。

## 8. 运行方式

```bash
./gradlew :spikes:s3-ui:run
```

压力模式：Library 页点击 “Stress 1,000” FilterChip。

## 9. 证据

- 代码：`experiments/spikes/s3-ui`
- 依赖解析：`./gradlew :spikes:s3-ui:dependencies --configuration runtimeClasspath`
- 构建：`./gradlew :spikes:s3-ui:build` → BUILD SUCCESSFUL（2026-10-28）
- MD3E：`material3-desktop-1.12.0-alpha03.jar` 中存在 `LoadingIndicatorKt`、`ButtonGroupKt`、`FloatingActionButtonMenuKt`、`ExperimentalMaterial3ExpressiveApi`
