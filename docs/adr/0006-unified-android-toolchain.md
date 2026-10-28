# ADR-0006：单一 Android/KMP 工具链（AGP 9.4.1 + Kotlin 2.3.21）

- 状态：接受（M0 已验证）
- 日期：2026-10-28
- 取代：Phase 1 计划中的「双 Android 工具链」假设

## 背景

Phase 0 汇总时存在两个冲突：

- Compose 1.12 需要 AGP 9.1+ 与 compileSdk 37；
- quickjs-kt 1.0.15 以 Kotlin 2.4 元数据发布，而 AGP 9.4 的内置 Kotlin 默认版本较低，S2 Android Spike 因此退回 AGP 8.13.2 + KGP 2.4.21 的独立工程。

Phase 1 M0 需要确认：能否让 Android 应用、KMP library、QuickJS、mediamp、Compose、Ktor 共用一套工具链。

## 决策

Android 应用与 KMP Android library 统一使用：

| 组件 | 版本 |
|---|---|
| AGP | 9.4.1 |
| Kotlin（KGP / 编译器 / Compose compiler plugin） | 2.3.21 |
| Gradle | 9.6 |
| JDK | 21（字节码目标 17） |
| compileSdk / targetSdk / minSdk | 37 / 36 / 26 |
| Compose | BOM 2026.09.00（Compose 1.12.1） |
| quickjs-kt | 1.0.15（Kotlin 2.4 元数据） |
| mediamp | 0.5.1 |
| Ktor | 3.6.0（以 Kotlin 2.3.21 构建） |

关键机制：在 Android 应用模块应用 `org.jetbrains.kotlin.plugin.compose` 2.3.21，使构建类路径解析到 KGP 2.3.21；该编译器可读取 Kotlin 2.4 元数据。KMP library 模块直接声明 KGP 2.3.21。

KMP Android library 使用 AGP 9.4 的 `kotlin { android { ... } }` DSL；`androidLibrary {}` 在 AGP 9.4.1 中是废弃别名。

不再采用「独立 AAR + 双工具链」或 vendor QuickJS 方案。

## 验证

- `buildEnvironment`：`kotlin-gradle-plugin:2.3.21`，AGP 9.4.1。
- `experiments/phase1/toolchain-probe`：`assembleDebug` 成功；模拟器运行时显示 `quickjs=3`、`ktor=200 body=miyomu-ok`、`mediamp=org.openani.mediamp.MediampPlayer`。
- `experiments/phase1/network-probe`：KMP library（`jvm()` + `android {}`）`assemble` 成功，产出 Android AAR；`jvmTest` 8/8 通过。
- 证据：`experiments/phase1/evidence/m0-toolchain-android.png`、`experiments/phase1/evidence/m0-network-android.png`。

## 后果

正面：

- 单工具链、单 IDE 模型；Android 应用可直接消费 KMP library 的 Android 变体，无需 AAR 包装或元数据降级。
- Ktor 3.6.0 与编译器同为 Kotlin 2.3.21，减少元数据风险。

负面 / 约束：

- 必须保持 Compose compiler plugin 与 KGP 同为 2.3.21；升级 AGP/Kotlin 时需同时验证 quickjs-kt、mediamp、Ktor。
- KMP library 为单变体模型：无 BuildConfig、无 flavors、无 `applicationVariants`；相关逻辑放应用模块或运行时配置。
- `kotlin { android {} }` 是 AGP 9.4 的正式 DSL，不要按旧文档改用 `androidLibrary {}`。
- iOS 仍用同一 KGP 2.3.21 编译 K/N；iOS App 壳验证未做，不能据此宣称 iOS 可用。
