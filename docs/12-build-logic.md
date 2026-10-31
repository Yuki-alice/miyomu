# 12 · build-logic 约定插件

日期：2026-10-31
状态：已完成

## 1. 目标

把 20 个模块里重复的 KMP/Compose/JVM 配置收敛到 `build-logic`，让模块只声明“我是哪种模块 + 我依赖谁”。

迁移前，每个模块都要写：

```kotlin
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kmp.library)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
}

kotlin {
    android {
        namespace = "app.miyomu.core.design"
        compileSdk = 37
        minSdk = 26
    }
    jvm("desktop")
    iosArm64()
    iosSimulatorArm64()
    // ...
}
```

迁移后：

```kotlin
plugins {
    id("miyomu.kmp.library")
    id("miyomu.kmp.compose")
}

kotlin {
    // 只写 sourceSets / 模块特有配置
}
```

## 2. 插件清单

| 插件 ID | 作用 | 使用模块 |
| --- | --- | --- |
| `miyomu.kmp.library` | Kotlin Multiplatform + AGP KMP library + Android/JVM/iOS targets + 命名空间 | 所有 `core:*`、`feature:*`、`app:shared` |
| `miyomu.kmp.compose` | Compose Multiplatform + Compose compiler | `core:design`、`core:ui`、`feature:*`、`app:shared` |
| `miyomu.jvm.library` | Kotlin JVM + JDK 21 toolchain | `source:host` |
| `miyomu.jvm.compose` | Kotlin JVM + Compose Desktop + Compose compiler + JDK 21 | `app:desktop` |

`core:database` 在 `miyomu.kmp.library` 之上继续声明 `ksp` / `room`，因为只有它需要 Room 代码生成。

## 3. 实现要点

### 3.1 命名空间

`miyomu.kmp.library` 用项目路径推导 Android namespace：

```text
:core:design        -> app.miyomu.core.design
:feature:library    -> app.miyomu.feature.library
:app:shared         -> app.miyomu.shared
```

模块不再手写 `namespace`。

### 3.2 AGP KMP DSL

插件通过 AGP 的 `KotlinMultiplatformAndroidLibraryTarget` 配置：

```kotlin
val kotlin = extensions.getByType(KotlinMultiplatformExtension::class.java)
val android = (kotlin as ExtensionAware).extensions
    .getByType(KotlinMultiplatformAndroidLibraryTarget::class.java)
android.namespace = miyomuNamespace()
android.compileSdk = 37
android.minSdk = 26
kotlin.jvm("desktop")
kotlin.iosArm64()
kotlin.iosSimulatorArm64()
```

### 3.3 为什么不用 `kotlin-dsl` 插件

当前环境没有缓存 `org.gradle.kotlin.kotlin-dsl:6.7.3`，且 Maven Central 访问受限。
因此 build-logic 使用：

```kotlin
plugins {
    `java-gradle-plugin`
    id("org.jetbrains.kotlin.jvm") version "2.4.21"
}
```

插件类仍用 Kotlin 写，并通过 Java/Gradle API 配置扩展。等 `kotlin-dsl` 可用时可替换，不影响插件 ID/使用方式。

### 3.4 模块依赖仍显式

插件只负责**模块类型与目标平台**，不隐藏依赖：

- `feature:library` 仍然显式写 `implementation(project(":core:ui"))`；
- `core:network` 仍然显式写 Ktor；
- `core:database` 仍然显式写 Room/KSP。

这样模块的依赖图仍然一眼可见，不会被“魔法插件”掩盖。

## 4. 验证

清理后重新构建：

```bash
./gradlew clean --offline --no-configuration-cache
./gradlew :app:android:assembleDebug \
          :app:desktop:test \
          :core:reader:desktopTest \
          :app:shared:linkDebugFrameworkIosSimulatorArm64 \
          --offline --no-configuration-cache
```

结果：

```text
BUILD SUCCESSFUL
193 reader-core tests passed
Android APK / Desktop test / iOS framework 全部通过
```

## 5. 当前模块写法

```kotlin
// core/model
plugins { id("miyomu.kmp.library") }

// core/design
plugins {
    id("miyomu.kmp.library")
    id("miyomu.kmp.compose")
}

// core/database
plugins {
    id("miyomu.kmp.library")
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
}

// feature/library
plugins {
    id("miyomu.kmp.library")
    id("miyomu.kmp.compose")
}

// source/host
plugins { id("miyomu.jvm.library") }

// app/desktop
plugins { id("miyomu.jvm.compose") }
```

## 6. 后续可选项

- `miyomu.android.application`：收敛 `app:android` 的 compileSdk/targetSdk/Compose。
- `miyomu.kmp.room`：把 KSP 配置也收进插件（需要把 room schema 目录作为参数）。
- `miyomu.kmp.serialization`：为 `core:source`/`core:net` 恢复做准备。
- 增加 build-logic 的单元测试（Gradle TestKit）。

## 7. 结论

build-logic 已落地，模块配置从“每个模块 15 行平台声明”变成“1–2 行插件声明”。

这不是为了少写几行，而是为了让**“一个新模块应该长什么样”**有唯一答案。
