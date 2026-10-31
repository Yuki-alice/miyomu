# 05 · 项目初始化方案

> **方向修正（2026-10-31）：** 本文为初稿，部分结论已被 [07-架构优先方案](07-architecture-first.md) 与 [08-MD3E 设计系统](08-md3e-design.md) 取代。冲突处以后两者为准。

> **按 07 的修正：** 初始化顺序改为“基线 → core 抽取 → data → feature → UI”；数据库/ID/UI 以 07/08 为准。**当前初始化方案见 [09-initialization.md](09-initialization.md)**；本文的 Gradle/CI 细节仅作历史参考。

> 目标：用一周把“三端能跑、依赖能锁、CI 能绿、设计系统能看”的骨架搭起来；再用 4–6 周完成第一条纵切。
> 原则：**先骨架后功能，先纵切后横向，先门禁后优化。**

## 1. 前置检查

### 1.1 本机环境

| 项 | 当前值 | 要求 |
| --- | --- | --- |
| macOS | 26.6.2 (Apple Silicon) | ✅ |
| JDK | 21.0.12 Corretto | ✅ |
| Android SDK | platforms 35/36/37，build-tools 37 | ✅ |
| Xcode | 26.6，iOS SDK 26.5 | ✅ |
| Node | 24.19 | 仅文档 |
| RAM | 8 GB | ⚠️ 偏紧 |
| 磁盘 | 约 36 GB 可用 | ⚠️ 偏紧 |

**建议先做**：

```bash
# 清理 Xcode/Gradle 旧缓存（确认路径后再执行）
du -sh ~/Library/Developer/Xcode/DerivedData ~/.gradle/caches 2>/dev/null
# 目标：至少 60 GB 可用
```

### 1.2 环境变量

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
export ANDROID_HOME="$HOME/Library/Android/sdk"
export PATH="$ANDROID_HOME/platform-tools:$ANDROID_HOME/cmdline-tools/latest/bin:$PATH"
```

### 1.3 验证

```bash
java -version
adb devices
xcodebuild -version
```

## 2. 仓库结构

```text
miyomu/
├── .editorconfig
├── .gitignore
├── .gitattributes
├── LICENSE
├── NOTICE
├── README.md
├── settings.gradle.kts
├── build.gradle.kts
├── gradle.properties
├── gradle/
│   ├── libs.versions.toml
│   └── wrapper/
│       ├── gradle-wrapper.jar
│       └── gradle-wrapper.properties
├── build-logic/
│   ├── settings.gradle.kts
│   ├── build.gradle.kts
│   └── src/main/kotlin/
│       ├── MiyomuKmpLibraryPlugin.kt
│       ├── MiyomuKmpComposePlugin.kt
│       ├── MiyomuAndroidLibraryPlugin.kt
│       ├── MiyomuAndroidApplicationPlugin.kt
│       └── MiyomuJvmLibraryPlugin.kt
├── app/
│   ├── shared/
│   │   ├── build.gradle.kts
│   │   └── src/
│   │       ├── commonMain/kotlin/app/miyomu/App.kt
│   │       ├── commonMain/kotlin/app/miyomu/di/
│   │       ├── commonMain/kotlin/app/miyomu/navigation/
│   │       ├── androidMain/kotlin/app/miyomu/platform/
│   │       ├── desktopMain/kotlin/app/miyomu/platform/
│   │       └── iosMain/kotlin/app/miyomu/platform/
│   ├── android/
│   │   ├── build.gradle.kts
│   │   └── src/main/
│   │       ├── AndroidManifest.xml
│   │       └── kotlin/app/miyomu/android/MainActivity.kt
│   └── desktop/
│       ├── build.gradle.kts
│       └── src/main/kotlin/app/miyomu/desktop/Main.kt
├── iosApp/
│   ├── Miyomu.xcodeproj/
│   ├── Miyomu/
│   │   ├── iOSApp.swift
│   │   └── Info.plist
│   └── Configuration/
├── core/
│   ├── model/
│   ├── database/
│   ├── network/
│   ├── source/
│   ├── data/
│   ├── reader/
│   ├── design/
│   └── ui/
├── feature/
│   ├── library/
│   ├── discover/
│   ├── reader/
│   └── settings/
├── source/
│   └── host/
├── design/
│   └── tokens.md          # 设计 token 的文本快照（可选）
└── docs/
```

说明：

- `:app:shared` 是 KMP 组合根，包含 `App()`、导航、DI 与平台 `actual`。
- `:app:android` / `:app:desktop` 是薄壳；iOS 由 `iosApp/` Xcode 工程消费 `:app:shared` 的 framework。
- 不创建 `:app:ios` Gradle 模块，避免多一层无意义转发。

## 3. Gradle 配置

### 3.1 `settings.gradle.kts`

```kotlin
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
}

rootProject.name = "miyomu"

includeBuild("build-logic")

include(":app:shared")
include(":app:android")
include(":app:desktop")
include(":core:model")
include(":core:database")
include(":core:network")
include(":core:source")
include(":core:data")
include(":core:reader")
include(":core:design")
include(":core:ui")
include(":feature:library")
include(":feature:discover")
include(":feature:reader")
include(":feature:settings")

// Desktop-only, not configured for Android/iOS builds
include(":source:host")

project(":app:shared").projectDir = file("app/shared")
project(":app:android").projectDir = file("app/android")
project(":app:desktop").projectDir = file("app/desktop")
project(":source:host").projectDir = file("source/host")
```

### 3.2 `gradle.properties`（8 GB 机器）

```properties
org.gradle.jvmargs=-Xmx3072m -XX:MaxMetaspaceSize=768m -Dfile.encoding=UTF-8
kotlin.daemon.jvmargs=-Xmx2048m
org.gradle.parallel=true
org.gradle.caching=true
org.gradle.configuration-cache=true
org.gradle.vfs.watch=true
android.useAndroidX=true
android.nonTransitiveRClass=true
kotlin.code.style=official
```

> 首次同步若因 configuration cache 失败，先加 `--no-configuration-cache` 定位，不要直接关闭全局缓存。

### 3.3 `gradle/libs.versions.toml`（节选）

```toml
[versions]
kotlin = "2.4.21"
agp = "9.3.1"
gradle = "9.7.0"
compose = "1.12.2"
adaptive = "1.3.0"
navigation = "2.10.0"
lifecycle = "2.11.0"
koin = "4.2.2"
sqldelight = "2.4.1"
ktor = "3.6.1"
coil = "3.6.3"
serialization = "1.12.0"
coroutines = "1.11.0"
datetime = "0.8.0"
okio = "3.9.2"
datastore = "1.3.0"
kermit = "2.2.0"
media3 = "1.11.1"
jna = "5.17.0"
turbine = "1.2.1"
junit = "5.11.4"
kotest = "5.9.1"

[libraries]
kotlinx-coroutines-core = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-core", version.ref = "coroutines" }
kotlinx-serialization-json = { module = "org.jetbrains.kotlinx:kotlinx-serialization-json", version.ref = "serialization" }
kotlinx-datetime = { module = "org.jetbrains.kotlinx:kotlinx-datetime", version.ref = "datetime" }
okio = { module = "com.squareup.okio:okio", version.ref = "okio" }
sqldelight-runtime = { module = "app.cash.sqldelight:runtime", version.ref = "sqldelight" }
sqldelight-coroutines = { module = "app.cash.sqldelight:coroutines-extensions", version.ref = "sqldelight" }
sqldelight-android = { module = "app.cash.sqldelight:android-driver", version.ref = "sqldelight" }
sqldelight-jvm = { module = "app.cash.sqldelight:sqlite-driver", version.ref = "sqldelight" }
sqldelight-native = { module = "app.cash.sqldelight:native-driver", version.ref = "sqldelight" }
ktor-client-core = { module = "io.ktor:ktor-client-core", version.ref = "ktor" }
ktor-client-okhttp = { module = "io.ktor:ktor-client-okhttp", version.ref = "ktor" }
ktor-client-darwin = { module = "io.ktor:ktor-client-darwin", version.ref = "ktor" }
coil-compose = { module = "io.coil-kt.coil3:coil-compose", version.ref = "coil" }
coil-network-ktor = { module = "io.coil-kt.coil3:coil-network-ktor3", version.ref = "coil" }
koin-core = { module = "io.insert-koin:koin-core", version.ref = "koin" }
koin-compose = { module = "io.insert-koin:koin-compose", version.ref = "koin" }
koin-viewmodel = { module = "io.insert-koin:koin-compose-viewmodel", version.ref = "koin" }

[plugins]
kotlin-multiplatform = { id = "org.jetbrains.kotlin.multiplatform", version.ref = "kotlin" }
kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
compose-compiler = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
compose-multiplatform = { id = "org.jetbrains.compose", version.ref = "compose" }
android-application = { id = "com.android.application", version.ref = "agp" }
android-library = { id = "com.android.library", version.ref = "agp" }
android-kmp-library = { id = "com.android.kotlin.multiplatform.library", version.ref = "agp" }
sqldelight = { id = "app.cash.sqldelight", version.ref = "sqldelight" }
```

> Material3 使用 CMP 插件提供的 `compose.material3` 坐标，不手写版本；Adaptive 使用 `org.jetbrains.compose.material3.adaptive:adaptive:1.3.0`。第一次构建时用 `./gradlew dependencies` 校验，不要凭记忆写 artifact。

### 3.4 `build-logic` 约定

```kotlin
// build-logic/src/main/kotlin/MiyomuKmpLibraryPlugin.kt
class MiyomuKmpLibraryPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("org.jetbrains.kotlin.multiplatform")
        pluginManager.apply("com.android.kotlin.multiplatform.library")
        extensions.configure<KotlinMultiplatformExtension> {
            androidTarget()
            jvm("desktop")
            iosArm64()
            iosSimulatorArm64()
                }
    }
}
```

每个模块的 `build.gradle.kts` 只写：

```kotlin
plugins {
    id("miyomu.kmp.library")
    id("miyomu.kmp.compose") // 需要 UI 时
}
```

目标：

- 模块不重复写 `androidTarget/jvm/ios`。
- 统一 Java toolchain 21、compileSdk 37、minSdk 26。
- 统一 `commonTest` 依赖。
- 统一 Compose 资源目录与 compiler 配置。

### 3.5 `app:shared` 关键配置

```kotlin
kotlin {
    androidTarget()
    jvm("desktop")
    listOf(iosArm64(), iosSimulatorArm64()).forEach {
        it.binaries.framework {
            baseName = "MiyomuShared"
            isStatic = true
        }
    }
    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.ui)
            implementation(projects.feature.library)
            implementation(projects.feature.discover)
            implementation(projects.feature.reader)
            implementation(projects.feature.settings)
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.viewmodel)
        }
        androidMain.dependencies {
            implementation(libs.koin.android)
            implementation(libs.androidx.activity.compose)
        }
        val desktopMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation(libs.jna)
            }
        }
    }
}
```

## 4. 首批提交（12 步）

每一步都必须能单独构建/回滚。

| # | 提交 | 内容 | 验收 |
| --- | --- | --- | --- |
| 1 | `chore: bootstrap gradle and three targets` | wrapper、settings、version catalog、build-logic、空模块、三端 hello | Android/Desktop/iOS 都能启动 |
| 2 | `chore: add license, notice and editorconfig` | Apache-2.0、NOTICE、.editorconfig、.gitignore | `./gradlew check` 无 license 告警 |
| 3 | `feat(core): add IDs, content models and errors` | `SourceId`/`ContentKey`/`ChapterKey`、`ContentSummary`、`Outcome` | `:core:model:allTests` 通过 |
| 4 | `feat(db): add v1 sqldelight schema` | 表、索引、`MiyomuDatabase` 工厂 | `verifyMigrations` + schema 单测 |
| 5 | `feat(design): add sumi tokens and base components` | 颜色/字体/形状、按钮/卡片/空态 | Compose Preview + golden |
| 6 | `feat(ui): add adaptive shell and navigation` | 底栏/rail/sidebar、NavHost、路由 | 三端可切换 5 个页面 |
| 7 | `feat(source): add native source manifest and runtime` | manifest 解析、browse/search/details/pages、fixture | MockEngine 测试通过 |
| 8 | `feat(data): add library repository and progress saver` | 收藏/历史/进度/更新事件、事务 | 进度恢复测试 |
| 9 | `feat(discover): add browse/search/details vertical slice` | 源列表、结果、详情、收藏 | 手动走通“导入源→收藏” |
| 10 | `feat(reader): add paged and webtoon reader` | 场景、手势、chrome、进度 | 手动读 3 章并恢复 |
| 11 | `test: add query-count and migration gates` | 查询计数、迁移回放、CI | CI 全绿 |
| 12 | `ci: add android/desktop/ios matrix` | 构建、测试、产物 | 三条 job 通过 |

### 4.1 第 1 步的验收命令

```bash
./gradlew :app:android:assembleDebug
./gradlew :app:desktop:run
./gradlew :app:shared:linkDebugFrameworkIosSimulatorArm64
```

Xcode：

```bash
open iosApp/Miyomu.xcodeproj
# scheme: Miyomu, simulator: iPhone 17
```

### 4.2 第 4 步的 schema 文件

```text
core/database/src/commonMain/sqldelight/app/miyomu/db/
├── Content.sq
├── Chapter.sq
├── Library.sq
├── Progress.sq
├── Update.sq
├── Download.sq
├── Source.sq
├── Extension.sq
└── Sync.sq
```

约定：

- 一个 `.sq` 文件对应一个聚合，不写“万能 DAO”。
- 查询命名 `selectXxx` / `upsertXxx` / `deleteXxx`。
- 列表查询放 `Content.sq`，返回投影类型。
- 迁移文件 `1.sqm` 起；禁止改已发布 `.sq`。

### 4.3 第 7 步的源 manifest（示例）

```json
{
  "id": "native:example",
  "name": "Example Source",
  "version": 1,
  "language": "en",
  "contentType": "MANGA",
  "baseUrl": "https://example.com",
  "list": {
    "url": "/popular?page={page}",
    "item": "div.card",
    "title": "h3.title",
    "cover": "img@src",
    "link": "a@href"
  },
  "details": {
    "title": "h1",
    "author": ".author",
    "description": ".summary",
    "chapters": "ul.chapters li"
  },
  "pages": {
    "item": ".reader img@src"
  }
}
```

第一版只支持：

- CSS 选择器（Ksoup）
- 属性取值 `@href` / `@src`
- 分页 `{page}`
- 静态 headers/referer
- 详情/章节/页面三段

动态筛选/JS hook 在 v0.5 再加。

## 5. CI 设计

### 5.1 工作流

```text
.github/workflows/
├── ci.yml          # PR: android + desktop + ios compile + tests
├── nightly.yml     # 三端产物
└── release.yml     # tag -> GitHub Release
```

### 5.2 Job 矩阵

| Job | Runner | 命令 | 产物 |
| --- | --- | --- | --- |
| `android` | ubuntu-latest | `:app:android:assembleDebug` + `testDebugUnitTest` + `lintDebug` | APK |
| `desktop` | macos-15 | `:app:desktop:packageDmg`（或 compile+test） | DMG |
| `desktop-win` | windows-latest | `:app:desktop:packageMsi` | MSI |
| `ios` | macos-15 | `xcodebuild -scheme Miyomu -sdk iphonesimulator` | framework/无签名 app |
| `quality` | ubuntu-latest | detekt + ktlint + `verifyMigrations` + license | 报告 |

### 5.3 缓存

- `actions/setup-java` + `gradle/actions/setup-gradle`
- Kotlin/Native 缓存（`~/.konan`）
- Xcode DerivedData 缓存（谨慎，避免脏缓存）
- Gradle build cache

### 5.4 质量门禁

必须通过：

- `detekt`
- `ktlintCheck`
- `:core:database:verifyMigrations`
- 所有 `commonTest` / `jvmTest`
- `:app:shared:compileKotlinMetadata`（可移植性）
- `:feature:*:compileDebugKotlinAndroid`
- `:app:android:lintDebug`
- license/notice 检查

建议通过：

- 查询计数测试
- Compose stability 报告
- Macrobenchmark（夜间）
- Golden 测试

## 6. 测试与性能门禁

### 6.1 查询计数

```kotlin
@Test
fun libraryPageDoesNotLoadChapters() = runTest {
    val counter = QueryCounter()
    library.observeLibrary(LibraryQuery(limit = 50)).first()
    assertThat(counter.selects("chapter")).isZero()
    assertThat(counter.total).isAtMost(6)
}
```

### 6.2 性能预算

| 场景 | 预算 |
| --- | --- |
| Android 冷启动到首帧 | < 1.2s（中端机） |
| 桌面冷启动到窗口 | < 1.5s |
| 书库 10k 部首屏 | < 300ms |
| 书库滚动 | 60fps，无掉帧 > 32ms |
| 详情打开（缓存） | < 150ms |
| 阅读器翻页 | < 16ms/帧 |
| 单页解码（1080p 屏） | < 50ms |
| 内存（Android 前台） | < 300MB |
| 内存（桌面） | < 600MB |
| 基础 APK | < 30MB |
| 桌面安装包 | < 150MB（含 JVM） |

### 6.3 观测

- Debug 面板：FPS、内存、DB 查询数、图片缓存命中、源请求耗时。
- Release 只记录聚合指标；默认不上传。

## 7. 发布与签名

- Android：使用 keystore + GitHub Secrets；`applicationId app.miyomu`，debug 后缀 `.debug`。
- Desktop：
  - macOS：DMG，需签名/公证（后续）。
  - Windows：MSI/ZIP；可选签名。
  - Linux：deb/AppImage。
- iOS：未签名 IPA（侧载）或 TestFlight（后续）；bundle id `app.miyomu`。
- 版本：`0.1.0` 起步；每个 milestone 打 tag。
- 更新：应用内检查 GitHub Releases；不做 bspatch。

## 8. 风险登记

| 风险 | 概率 | 影响 | 缓解 | 负责人 |
| --- | --- | --- | --- | --- |
| 8GB 内存构建 OOM | 高 | 中 | 降堆、串行构建、CI 分担 | 开发 |
| iOS CMP 滚动性能 | 中 | 高 | 第 1 周基准；原生播放器；避免 blur | 开发 |
| SQLDelight 迁移错误 | 中 | 高 | 迁移回放 + schema diff | 开发 |
| 扩展宿主许可 | 中 | 高 | 独立模块 + NOTICE + 法律复核 | 产品/开发 |
| 源站点反爬 | 高 | 中 | 诊断、可更新源、浏览器验证 | 开发 |
| 功能范围膨胀 | 高 | 高 | 功能矩阵 + 版本冻结 | 产品 |
| 设计系统执行不一致 | 中 | 中 | token + golden + review | 设计/开发 |
| 单一开发者疲劳 | 中 | 高 | 纵切里程碑、自动化、文档 | 全体 |

## 9. 时间估算

以“1 名开发者 + AI 代理”为假设，含设计/测试/文档，单位为周：

| 阶段 | 内容 | 估算 |
| --- | --- | ---: |
| M0 | 骨架 + 三端 + CI | 1–2 |
| M1 | 原生源纵切 + 基础阅读 | 4–5 |
| M2 | 书库/历史/更新/下载 | 4–5 |
| M3 | 阅读器完善 + 桌面体验 | 4–5 |
| M4 | Mihon 扩展 + 小说 + 视频 | 6–8 |
| M5 | WebDAV + iOS + 发布 | 5–6 |
| **合计** | v1.0 | **24–31 周** |

> 这是日历估算，不是承诺。若只做 Android + Desktop，v0.5 可在约 10–12 周达到可用。

## 10. 第一个里程碑的完成定义（M0）

- [ ] `./gradlew :app:android:assembleDebug` 成功。
- [ ] Android 模拟器启动，显示 “Miyomu” + Sumi 主题。
- [ ] `./gradlew :app:desktop:run` 打开 1280×820 窗口，显示同一页面。
- [ ] Xcode 模拟器启动，显示同一页面。
- [ ] `MiyomuApp()` 在三个平台共享同一个 Composable。
- [ ] 底栏/rail/sidebar 随窗口宽度切换。
- [ ] `:core:database` 能打开空库，迁移测试通过。
- [ ] CI 三端 job 绿。
- [ ] README/架构文档/ADR 已提交。

## 11. 初始化检查清单

### 仓库

- [ ] `git init`，默认分支 `main`。
- [ ] `.gitignore` 排除 `.gradle/`、`build/`、`local.properties`、`*.keystore`、`DerivedData/`、`.idea/`。
- [ ] `.gitattributes` 统一 LF。
- [ ] `.editorconfig`：UTF-8、LF、4 空格、120 列。
- [ ] `LICENSE`（Apache-2.0）。
- [ ] `NOTICE`（若复用 Kototoro 代码）。
- [ ] `README.md` 与 docs。

### Gradle

- [ ] wrapper 9.7.0。
- [ ] `libs.versions.toml` 锁定全部版本。
- [ ] `build-logic` 可用。
- [ ] `gradle.properties` 适配 8GB。
- [ ] 三端模块可配置。
- [ ] `./gradlew projects` 无循环依赖。

### Android

- [ ] `applicationId app.miyomu`。
- [ ] `minSdk 26` / `compileSdk 37` / `targetSdk 37`。
- [ ] Compose + edge-to-edge。
- [ ] 权限最小化：网络、通知（按需）。
- [ ] debug 后缀 `.debug`。

### Desktop

- [ ] `main()` + `Window`。
- [ ] 最小窗口 1024×700。
- [ ] macOS 菜单/快捷键。
- [ ] `packageDmg` 可运行。

### iOS

- [ ] Xcode 工程引用 `MiyomuShared.framework`。
- [ ] `ComposeUIViewController` 显示。
- [ ] `Info.plist` 方向/文件类型。
- [ ] 模拟器启动。

### 质量

- [ ] detekt/ktlint。
- [ ] 迁移测试。
- [ ] 单元测试样例。
- [ ] CI。
- [ ] ADR。

## 12. 下一步

1. 确认本方案中的 3 个决策：iOS 最低版本、是否首版支持桌面扩展、品牌色。
2. 按第 4 节第 1 步初始化仓库。
3. 第 1 周结束前完成 M0 验收。
4. 之后每个 milestone 只开一个纵切，不并行铺功能。
