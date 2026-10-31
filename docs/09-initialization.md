# 09 · 初始化方案（架构优先 + MD3E）

> 配套 [07-架构优先方案](07-architecture-first.md) 与 [08-MD3E 设计系统](08-md3e-design.md)。
> 目标：先把“能构建的骨架 + 可复制的核心”立起来，再按 feature 迁移；UI 原型并行。

## 1. 前置检查

| 项 | 当前值 | 要求 |
| --- | --- | --- |
| macOS | 26.6.2 Apple Silicon | ✅ |
| JDK | 21.0.12 Corretto | ✅ |
| Android SDK | platforms 35/36/37，build-tools 37 | ✅ |
| Xcode | 26.6，iOS SDK 26.5 | ✅ |
| RAM | 8 GB | ⚠️ 构建时关闭其他大应用 |
| 磁盘 | 约 36 GB 可用 | ⚠️ 建议清理到 60 GB |

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
export ANDROID_HOME="$HOME/Library/Android/sdk"
java -version && adb devices && xcodebuild -version
```

## 2. 基线策略

- 基线：`reference/Kototoro`，commit `dfcbd102c`，v2.3.4。
- 新仓库：`miyomu`。
- 只读基线放在 `baseline/kototoro`（本地副本或 submodule，不参与构建）。
- 复制代码时保留 Apache-2.0 版权头；在 `NOTICE` 记录来源与 commit。
- 不复制 GPL/AGPL 代码；Kotatsu 等只通过依赖/运行时集成。

```text
baseline/kototoro/    # 只读参考，不参与 Gradle
```

## 3. 仓库结构

```text
miyomu/
├── baseline/kototoro/          # 只读基线
├── app/
│   ├── shared/                 # KMP 组合根；产出 MiyomuShared.framework
│   ├── android/                # Android 壳
│   └── desktop/                # Compose Desktop 壳
├── iosApp/                     # Xcode 壳
├── core/
│   ├── model/                  # 纯模型/ID/错误
│   ├── database/               # Room KMP（复制 core-db）
│   ├── network/                # Ktor/OkHttp/Cookie/Cloudflare
│   ├── source/                 # SourceRuntime/Registry/native engine
│   ├── reader/                 # reader-core + reader-novel
│   ├── data/                   # Repository/进度/更新/下载/备份
│   ├── design/                 # MD3E token/主题/基础组件
│   └── ui/                     # 自适应壳/导航/通用组件
├── feature/
│   ├── library/
│   ├── discover/
│   ├── reader/
│   ├── video/
│   ├── downloads/
│   ├── sync/
│   ├── settings/
│   └── extensions/
├── source/
│   └── host/                   # JVM 扩展宿主
├── build-logic/
├── design/md3e/                # 原型与设计资产
├── docs/
└── gradle/
```

模块数：当前 20 个 Gradle 模块；`build-logic` 约定插件为下一步（目标 21）。

## 4. Gradle 配置

### 4.1 版本锁定

```toml
[versions]
kotlin = "2.4.21"
agp = "9.3.1"
gradle = "9.7.0"
compose = "1.12.2"
md3e = "1.12.0-alpha03"
adaptive = "1.3.0"
room = "2.8.5"
ksp = "2.3.11"
koin = "4.2.2"
ktor = "3.6.1"
coil = "3.6.3"
serialization = "1.12.0"
coroutines = "1.11.0"
okio = "3.9.2"
navigation = "2.10.0"
lifecycle = "2.11.0"
media3 = "1.11.1"
jna = "5.17.0"
```

> KSP 必须与 Kotlin 版本匹配。参考项目使用 Kotlin 2.4.0 + KSP 2.3.11；升级 Kotlin 时先查官方兼容表。
> MD3E 使用 alpha 版本是**有意决策**：锁定、封装、只在 `core:design` 出现。

### 4.2 `settings.gradle.kts`

```kotlin
pluginManagement {
    repositories { google(); mavenCentral(); gradlePluginPortal() }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google(); mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
}

rootProject.name = "miyomu"
includeBuild("build-logic")

include(
    ":app:shared", ":app:android", ":app:desktop",
    ":core:model", ":core:database", ":core:network", ":core:source",
    ":core:reader", ":core:data", ":core:design", ":core:ui",
    ":feature:library", ":feature:discover", ":feature:reader",
    ":feature:video", ":feature:downloads", ":feature:sync",
    ":feature:settings", ":feature:extensions",
    ":source:host",
)

project(":app:shared").projectDir = file("app/shared")
project(":app:android").projectDir = file("app/android")
project(":app:desktop").projectDir = file("app/desktop")
project(":source:host").projectDir = file("source/host")
```

> 代码里的 `projects.core.ui` 是 Gradle 类型安全项目访问器。若当前 Gradle 未启用，在 `settings.gradle.kts` 加 `enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")`，或全部改用 `project(":core:ui")`。

### 4.3 `gradle.properties`（8 GB）

```properties
org.gradle.jvmargs=-Xmx3072m -XX:MaxMetaspaceSize=768m -Dfile.encoding=UTF-8
kotlin.daemon.jvmargs=-Xmx2048m
org.gradle.parallel=true
org.gradle.caching=true
org.gradle.configuration-cache=true
android.useAndroidX=true
android.nonTransitiveRClass=true
kotlin.code.style=official
```

### 4.4 `core:database`（Room KMP）

```kotlin
plugins {
    id("miyomu.kmp.library")
    id("com.google.devtools.ksp")
    id("androidx.room")
}

kotlin {
    android { namespace = "app.miyomu.core.database"; compileSdk = 37; minSdk = 26 }
    jvm("desktop")
    iosArm64(); iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            api(libs.room.runtime)
            api(libs.coroutines.core)
            api(projects.core.model)
        }
    }
}

dependencies {
    add("kspAndroid", libs.room.compiler)
    add("kspDesktop", libs.room.compiler)
    add("kspIosArm64", libs.room.compiler)
    add("kspIosSimulatorArm64", libs.room.compiler)
}

room { schemaDirectory("$projectDir/schemas") }
```

要点：

- 复制 Kototoro `core-db` 的实体/DAO/迁移后，先保持包名 `org.skepsun.kototoro.core.db`，减少改动。
- Room schema JSON 纳入版本控制；迁移测试从基线开始跑。
- 不在 P2 改表结构。

### 4.5 `core:design`（MD3E）

```kotlin
plugins {
    id("miyomu.kmp.compose")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation("org.jetbrains.compose.material3:material3:1.12.0-alpha03")
            implementation(compose.components.resources)
        }
    }
}
```

```kotlin
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MiyomuTheme(content: @Composable () -> Unit) {
    MaterialExpressiveTheme(
        colorScheme = miyomuColorScheme(),
        motionScheme = MotionScheme.expressive(),
        typography = miyomuTypography(),
        shapes = miyomuShapes(),
        content = content,
    )
}
```

- 只允许 `core:design` 直接引用 MD3E experimental API；其他模块用封装后的组件。
- 每个基础组件有 `@Preview` 与 golden 测试。

### 4.6 `app:shared`

```kotlin
kotlin {
    android {
        namespace = "app.miyomu.shared"
        compileSdk = 37
        minSdk = 26
    }
    jvm("desktop")
    listOf(iosArm64(), iosSimulatorArm64()).forEach {
        it.binaries.framework {
            baseName = "MiyomuShared"
            isStatic = true
        }
    }
    sourceSets {
        commonMain.dependencies {
            implementation(projects.feature.library)
            implementation(projects.feature.discover)
            implementation(projects.feature.reader)
            implementation(projects.feature.video)
            implementation(projects.feature.downloads)
            implementation(projects.feature.sync)
            implementation(projects.feature.settings)
            implementation(projects.feature.extensions)
            implementation(projects.core.ui)
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
        }
        androidMain.dependencies { implementation(libs.androidx.activity.compose) }
        val desktopMain by getting { dependencies { implementation(compose.desktop.currentOs) } }
    }
}
```

Xcode 的 Run Script 增加：

```bash
cd "$SRCROOT/.."
./gradlew :app:shared:embedAndSignAppleFrameworkForXcode
```

模拟器调试也可以手动链接：

```bash
./gradlew :app:shared:linkDebugFrameworkIosSimulatorArm64
```

## 5. 迁移顺序

### P0 · 骨架（1–2 周）

1. 建仓库、`.gitignore`、`.editorconfig`、`LICENSE`、`NOTICE`。
2. 复制/创建 `build-logic`、wrapper、version catalog。
3. 建 20 个 Gradle 模块；`app:shared` 显示 MD3E 主题。
4. Android/Desktop 启动；iOS framework 链接 + Xcode 壳。
5. CI 三端 job。

**验收**：三端显示同一 MD3E 页面；`./gradlew check` 绿；许可证清单完成。

### P1 · 纯核心（2–3 周）

按顺序复制：

1. `reader-core` → `core/reader`
2. `reader-novel` → `core/reader`
3. `core-source` + `parser-api` 必要部分 → `core/source`
4. `core-net` → `core/network`
5. `core-backup` → `core/data`
6. `core-domain` → `core/model` + `core/data`

每复制一个模块：

```bash
./gradlew :core:reader:compileKotlinMetadata
./gradlew :core:reader:desktopTest
```

**验收**：原模块单测迁移通过；核心模块不依赖 Android/Compose（reader）或只依赖允许的库。

### P2 · Room 与仓库（2–4 周）

1. 复制 `core-db` → `core/database`。
2. 复制 `core-library`、`core-local` 和 `app` 的 Repository 实现 → `core/data`。
3. 用接口隔离 Hilt/Android 依赖。
4. 打开基线数据库，跑迁移测试。

**验收**：收藏/历史/章节/进度读写通过；迁移回放通过；备份格式可读写。

### P3 · Feature 逻辑（3–5 周）

逐 feature 复制状态/用例（不复制 UI）：

- library：home/favourites/history/tracker/bookmarks
- discover：explore/search/details/filter
- reader：reader/novel
- downloads：download
- sync：backups/sync
- video：video
- settings：preferences

**验收**：每个 feature 有 headless 测试或最小 Compose 壳；不依赖旧 `app`。

### P4 · 源与扩展（3–5 周）

1. 复制 `source-host`/`parser-host`/`dex-convert` → `source/host`。
2. Android Mihon/Aniyomi 适配。
3. Desktop 独立 JVM 宿主。
4. 扩展仓库/安装/偏好/源测试。
5. iOS 按 Kototoro 能力 parity。

**验收**：Android/Desktop 加载同一扩展；源测试 PASS。

### P5 · MD3E UI（与 P1–P4 并行）

- D0 tokens/主题
- D1 Home/Library/Browse/Details
- D2 Reader/Video
- D3 Settings/Downloads/Sync/Extensions
- D4 平板/桌面/iOS
- D5 全量替换

**验收**：新 UI 覆盖 P0/P1 功能；旧 UI 入口关闭；三端视觉一致。

### P6 · Parity/发布（2–4 周）

- 对照 [03 功能矩阵](03-feature-matrix.md) 逐项验收。
- 性能/无障碍/本地化/备份/扩展回归。
- 打包 APK/DMG/MSI/deb/IPA。

## 6. 首批提交

| # | 提交 | 验收 |
| --- | --- | --- |
| 1 | `chore: add workspace, license and baseline` | 基线可 diff；许可证清单 |
| 2 | `chore: add gradle wrapper, catalog and build-logic` | `./gradlew projects` 通过 |
| 3 | `feat(app): three MD3E shells` | 三端启动 |
| 4 | `feat(core): copy reader-core` | 原测试通过 |
| 5 | `feat(core): copy core-source` | 契约测试通过 |
| 6 | `feat(core): copy core-db with Room` | 迁移测试通过 |
| 7 | `feat(core): copy core-data repositories` | 读写/进度测试 |
| 8 | `feat(core): add Koin composition root` | 依赖图可启动 |
| 9 | `feat(design): MD3E tokens and components` | Preview/golden |
| 10 | `feat(ui): adaptive shell and navigation` | 断点切换 |
| 11 | `feat(feature): port library vertical slice` | 收藏/历史/详情可用 |
| 12 | `ci: three-platform build and migration tests` | CI 绿 |

## 7. CI

| Job | Runner | 命令 | 产物 |
| --- | --- | --- | --- |
| android | ubuntu-latest | `:app:android:assembleDebug` + unit + lint | APK |
| desktop | macos-15 | `:app:desktop:packageDmg` | DMG |
| windows | windows-latest | `:app:desktop:packageMsi` | MSI |
| ios | macos-15 | `xcodebuild -scheme Miyomu -sdk iphonesimulator` | framework/app |
| quality | ubuntu-latest | detekt + ktlint + Room migration + license | 报告 |

缓存：Gradle、Kotlin/Native、Xcode DerivedData。

## 8. 质量门禁

必须：

- `detekt` / `ktlintCheck`
- Room `verifyMigrations` + 迁移回放
- 所有 `commonTest`/`desktopTest`
- `:core:reader:compileKotlinMetadata`
- 查询计数：列表页不加载章节、不 N+1
- license/NOTICE 检查
- 新模块禁止 Hilt 注解（Detekt 自定义规则）
- MD3E 组件 golden/截图

建议：

- Macrobenchmark（启动/滚动/翻页）
- 阅读器内存/预取命中
- 三端截图对照

## 9. 并行设计线

| 阶段 | 设计产出 | 对应架构阶段 |
| --- | --- | --- |
| D0 | MD3E token/主题/字体/形状 | P0 |
| D1 | Home/Library/Browse/Details 原型 | P1 |
| D2 | 组件库（ButtonGroup/SplitButton/FABMenu/Sheet） | P2 |
| D3 | Reader/Video 控件 | P3 |
| D4 | 桌面/平板/iOS 适配 | P4 |
| D5 | 全量走查/替换 | P5 |

- 原型在 `design/md3e/`；当前第一版见 [prototype.html](../design/md3e/prototype.html)。
- 每个页面标注断点、loading/empty/error、交互。
- 原型不阻塞架构；但 P5 前必须完成 D1/D2。

## 10. 风险

| 风险 | 缓解 |
| --- | --- |
| 复制代码带入 Hilt/旧架构 | 新模块禁用 Hilt；只复制 core/feature 逻辑 |
| Room 迁移失败 | 保留原 schema/迁移；先只读验证；迁移回放 |
| MD3E alpha API 变更 | 锁定版本；封装在 core:design |
| 8GB 内存 OOM | 3G Gradle/2G Kotlin；串行构建；CI 分担 |
| 扩展 ABI/许可证 | 许可证清单；GPL 不复制；独立进程 |
| 设计/迁移不同步 | D/P 对齐；每周联合评审 |

## 11. M0 完成定义

- [ ] 三端显示 MD3E 主题壳。
- [ ] 20 个 Gradle 模块可配置，依赖方向正确；build-logic 为下一步。
- [ ] Room 空库/基线库可打开。
- [ ] reader-core 复制并通过原测试。
- [ ] CI 三端绿。
- [ ] `design/md3e/` 有 D0/D1 原型。
- [ ] LICENSE/NOTICE/ADR 完成。

## 12. 需要确认

1. 基线复制方式：新仓库选择性复制（推荐） vs fork 原地重构。
2. 是否保留 Room/Long ID（推荐是）。
3. 新模块 DI：Koin（推荐） vs 继续 Hilt。
4. MD3E alpha：接受（推荐） vs 等稳定版。
5. 首版平台：Android + Desktop 先行，iOS 同步骨架（推荐） vs 三端同权重。
