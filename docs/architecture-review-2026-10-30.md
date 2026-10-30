# Miyomu 架构与目录结构审阅报告

- **日期**：2026-10-30
- **范围**：`settings.gradle.kts` 中的全部 33 个 Gradle 模块、`build-logic/` 约定插件、`app/` 平台入口、`shared/` 共享层、CI 配置与测试布局。
- **性质**：只读架构审阅，未修改任何生产代码、构建脚本或配置。
- **结论**：工程骨架现代、依赖无环、组合根清晰；主要债务集中在模块边界、包结构、导航状态与质量门禁，而非功能实现。
- **实施状态（2026-10-31）**：用户已批准执行重构。"立刻 + 本迭代"两档（AR-01/02/03/04/05 止血/06/08/12/14）已实施并验证，
  详见 `docs/architecture-refactor-2026-10-31.md`；AR-07/09/10/11/13 仍为中期待办。

---

## 0. 摘要

### 0.1 总体判断

Miyomu 已经具备一套可长期演进的多模块骨架：AGP 9 的 app/shared 拆分、Convention Plugins、版本目录、显式组合根、Navigation 3 + 序列化路由键都做得正确。当前最需要处理的不是“再加抽象”，而是把已经存在的边界收口：

1. `shared/ui-core` 名义上是 UI 基础层，实际上是服务定位器 + `presentation` 再导出层。
2. `shared/domain` 被编译成 Compose 模块，UI 适配器与 Compose 注解进入领域层，并通过 `contract` 传染给数据层。
3. 约一半 Kotlin 文件仍使用 `tachiyomi` / `eu.kanade` / `aniyomi` / `mihon` 包根，缺少“移植代码”的显式边界。
4. Android 旋转/进程死亡会丢失返回栈，并取消正在进行的下载。
5. CI 只跑 `jvmTest`、桌面编译和 Android debug 打包，没有 lint、静态分析、Android 测试或 canary 套件。

### 0.2 最优先的 5 个动作

| 顺序 | 动作 | 预期收益 |
|---|---|---|
| 1 | 用 `rememberSaveable` / 保存型 back stack 恢复导航，并把长生命周期服务移出 composition | 修复旋转丢页、下载被取消 |
| 2 | 移除 `HomeScreen` 点击回调中的主线程 `runBlocking` | 消除 ANR/卡顿风险 |
| 3 | 修正 `ui-core` 的 `api`/`implementation` 与 `presentation` 再导出 | 让模块边界与编译图一致 |
| 4 | 让 `domain` 去 Compose 化，`contract` 只依赖 `model` | 恢复领域层可移植性 |
| 5 | CI 增加 `lintDebug`、静态分析与 canary 任务 | 阻止上述问题回归 |

---

## 1. 审阅方法

### 1.1 使用的 Skill

| Skill | 用途 |
|---|---|
| `analyze-code` | 采用其 P1–P10 审阅框架、健康度评分与优先级输出格式 |
| `kotlin-agent-skills` → `kotlin-tooling-agp9-migration` | 核对 AGP 9 / KMP 模块拆分、`com.android.kotlin.multiplatform.library`、源集与命名空间 |
| `chrisbanes-skills` → `kotlin-api-design` | 审阅公开 API 归属、`api`/`implementation`、平台边界 |
| `chrisbanes-skills` → `compose-state-and-effects` | 审阅导航状态、screen 状态持有者、effect 生命周期 |

未加载 Material 3 / MD3E / Compose 性能类 skill：本次是架构与目录审阅，不是组件设计或性能调优。

`analyze-code` 指定的 `consultant` 子代理类型在当前会话不可用，因此使用 3 个只读 general-purpose 子代理，按同一套 P1–P10 提示分别复核构建、分层与运行时组合；关键结论均由主代理重新核验。

### 1.2 已执行的验证

```bash
./gradlew projects --console=plain -q   # exit 0
```

同时通过 `grep`/`find`/`wc` 核验了模块依赖、文件规模、包根分布、主题重复、测试作用域等事实。

### 1.3 已修正的推断

子代理初稿曾认为 `kotlinx-coroutines-test` 被放在 `commonMain` 生产依赖中。人工核验后确认它们实际位于 `jvmTest.dependencies`：

- `shared/domain/build.gradle.kts:19-21`
- `shared/novel/build.gradle.kts:22-24`
- `shared/reader-novel/build.gradle.kts:18-20`
- `shared/source-novel/build.gradle.kts:27-29`

因此该问题**未写入结论**。类似地，“`api` 边占比 78/99”等未独立复算的数字不作为硬结论使用。

---

## 2. 项目概况

### 2.1 规模指标

| 指标 | 数值 |
|---|---|
| Gradle 模块 | 33（31 个 `shared:*` + 2 个 `app:*`） |
| Kotlin 文件 | 约 486 |
| Kotlin 代码行 | 约 54,000 |
| 测试文件 | 55（全部 `jvmTest`，约 7,700 行） |
| 最大模块 | `shared:presentation` 16,730 行（约 31%） |
| 次大模块 | `shared:presentation-core` 5,057 行 |
| 上游包根文件占比 | 约 50%（`tachiyomi` / `eu.kanade` / `aniyomi` / `mihon`） |
| 工具链 | Gradle 9.6、AGP 9.4.1、Kotlin 2.3.21、Compose Multiplatform 1.12.2、JDK 21 |
| Android SDK | compileSdk 37、targetSdk 36、minSdk 26 |

### 2.2 模块依赖图

```
app/android ─┐
app/desktop ─┴─> shared/application（组合根：Room/网络/下载/源运行时）
                       └─> shared/ui（MiyomuApp / NavHost / Home）
                              ├─> ui-entries
                              ├─> ui-library
                              ├─> ui-novel
                              ├─> ui-reader
                              ├─> ui-sources
                              ├─> ui-player
                              ├─> ui-settings
                              └─> shared/ui-core（MiyomuGraph + Navigation + Theme）
                                     ├─> presentation / presentation-core（Mihon/Aniyomi UI 移植）
                                     └─> domain / data / local / novel / source-* / download / danmaku
```

主路径为 `model → contract → data/source-* → ui-* → ui → application`，**未发现模块依赖环**。

### 2.3 模块规模分布

| 模块 | 文件数 | 行数 | 备注 |
|---|---:|---:|---|
| `presentation` | 127 | 16,730 | 移植 UI 巨石 |
| `presentation-core` | 53 | 5,057 | 设计系统组件 |
| `domain` | 59 | 3,266 | 含 UI 类型与 Compose 适配器 |
| `source-runtime` | 21 | 3,263 | JS/HTML 源运行时 |
| `ui` | 18 | 3,109 | App Shell |
| `ui-novel` | 13 | 2,694 | |
| `ui-core` | 35 | 2,353 | Graph + Nav + Theme |
| `novel` | 20 | 1,782 | |
| `ui-sources` | 7 | 1,761 | |
| `source-novel` | 11 | 1,575 | |
| `data` | 10 | 1,424 | Room |
| `ui-reader` | 9 | 1,237 | |
| `player` | 11 | 1,126 | |
| `ui-settings` | 5 | 1,080 | |
| `ui-library` | 8 | 1,039 | |
| `ui-entries` | 6 | 999 | |
| `local` | 11 | 778 | |
| `reader-novel` | 9 | 784 | |
| `source-repo` | 3 | 575 | |
| `network` | 8 | 539 | |
| `download` | 4 | 514 | |
| `reader-comic` | 5 | 454 | |
| `source-manager` | 3 | 441 | |
| `danmaku` | 4 | 333 | |
| `ui-player` | 2 | 312 | |
| `source-api` | 10 | 300 | |
| `contract` | 7 | 264 | |
| `model` | 7 | 182 | |
| `application` | 1 | 107 | |
| `app/android` | 1 | 70 | |
| `app/desktop` | 1 | 38 | |
| `i18n` / `i18n-aniyomi` | 0 | 0 | 仅 Moko XML 资源 |

规模分布明显不对称：`presentation` 一个模块占全部共享代码的约三成，而 `model`、`contract`、`source-api`、`source-manager` 都小于 500 行。小模块本身不是问题，问题是大模块没有按职责拆分。

---

## 3. 健康度评分

> 主观评分，基于本次只读审阅；用于横向比较，不作为精确度量。

| 维度 | 分数 | 说明 |
|---|---:|---|
| 构建与模块化 | 8/10 | Convention Plugins、版本目录、AGP 9 拆分、无环 |
| 分层清晰度 | 4/10 | `ui-core` 再导出、`domain` 被 Compose 化、feature 依赖未声明 |
| 目录与命名一致性 | 5/10 | 约一半上游包根，namespace 与包名多处不一致 |
| 运行时状态与生命周期 | 5/10 | 导航不可恢复、graph 泄漏 scope/client、主线程阻塞 |
| 可测试性 | 6/10 | 数据/源运行时测试扎实；presentation 与 Android actual 缺失 |
| CI / 质量门禁 | 5/10 | 仅 jvmTest + 编译 + assembleDebug |

---

## 4. 优势

- **AGP 9 Path C 拆分正确**：Android 与 Desktop 各只有 1 个入口文件，共享层不含平台入口；31 个共享模块均迁移到 `com.android.kotlin.multiplatform.library` 并显式声明 namespace。
- **组合根明确**：`shared/application/.../MiyomuApplication.kt:43-106` 统一装配 Room、偏好、源运行时、下载队列和 Ktor；平台层只负责 SAF、目录、窗口等 OS 细节。
- **导航集中且类型安全**：27 个 `@Serializable` NavKey、单一 `entryProvider`、`MiyomuNavigator` 不依赖全局 controller，方向正确。
- **依赖图无环**，Room schema 历史已提交，source runtime 有真实测试覆盖。
- **构建纪律良好**：版本目录、`MiyomuBuild` 常量、CI 双平台、`reference/` 已 gitignore、LICENSE/NOTICE 完整。
- **数据层 UI 干净**：`data`、`contract`、`local`、`novel`、`download`、`network` 的源码本身不 import `androidx.compose`；Compose 泄漏是经由 `domain` 传递进来的。

---

## 5. 发现与建议

> 优先级定义：**P0** = 用户可见的正确性问题；**P1** = 架构边界正在失效；**P2** = 目录/维护性债务；**P3** = 构建与质量门禁改进。

### P0 — 运行时正确性

#### AR-01 旋转/进程死亡丢失返回栈，并中断下载

- **证据**：
  - `shared/ui-core/src/commonMain/kotlin/app/miyomu/ui/navigation/MiyomuNavigator.kt:81-84`：back stack 只用 `remember`，注释明确说明恢复是 follow-up。
  - `shared/ui/src/commonMain/kotlin/app/miyomu/ui/MiyomuNavHost.kt:67-72`：直接消费该 back stack。
  - `shared/application/src/jvmShared/kotlin/app/miyomu/application/MiyomuApplication.kt:76-85`：composition 销毁时关闭数据库、源运行时与下载队列。
  - `app/android/src/main/AndroidManifest.xml:10-17`：未声明 `configChanges`，Activity 默认随旋转重建。
  - `shared/ui-core/.../navigation/MiyomuKey.kt:10-13`：注释声称支持“restored state、deep link”，但清单中没有 deep link，恢复也未实现。
- **影响**：阅读中旋转屏幕会回到 Home；进程死亡后丢失全部导航位置；正在下载的任务会被取消。对阅读器/下载器是用户可见缺陷。
- **建议**：
  1. 使用 `rememberSaveable` + NavKey 序列化，或 Navigation 3 提供的保存型 back stack。
  2. 把数据库、源运行时、下载队列移出 composition，由 Android `ViewModel`/Application 级持有；桌面端由窗口级 holder 持有。
  3. 补一个旋转/返回键冒烟测试。
- **工作量**：S–M（1–2 天）。

#### AR-02 主线程 `runBlocking` 查询数据库

- **证据**：`shared/ui/src/commonMain/kotlin/app/miyomu/ui/home/HomeScreen.kt:99,117`。
- **影响**：点击历史/继续阅读时在主线程同步查 Room，数据库繁忙时会卡顿甚至 ANR。
- **建议**：优先使用已 collect 的状态；否则在 `rememberCoroutineScope().launch` 中查询后再导航。
- **工作量**：S（数小时）。

### P1 — 架构边界

#### AR-03 `ui-core` 是伪核心：公开 API 泄漏 `implementation` 依赖，并再导出 `presentation`

- **证据**：
  - `shared/ui-core/build.gradle.kts:15-27`：local、novel、reader-*、download、danmaku、source-*、data 均为 `implementation`，但 `api(presentationCore)` / `api(presentation)`。
  - `shared/ui-core/.../MiyomuGraph.kt:31-58`：公开构造参数/属性直接暴露上述模块类型。
  - `shared/ui/build.gradle.kts:12-22` 未声明 `MiyomuApp.kt:31-48` 公开签名用到的 local/novel/download/source-repo/source-manager。
  - 6 个 `ui-*` feature 模块的构建文件均未直接依赖 `presentation`，却大量 import `eu.kanade.presentation.*`，依赖的是 `ui-core` 的再导出。
- **影响**：模块边界与 Gradle 编译图不一致；消费者靠“偶然的传递暴露”编译；改动 `ui-core` 会牵动全部 feature；`presentation` 无法独立拆分。
- **建议**：
  1. 立即可做：把 `MiyomuGraph` / `MiyomuApp` 公开签名涉及的依赖提升为 `api`。
  2. 然后删除 `ui-core` 对 `presentation` / `presentation-core` 的再导出，让每个 feature 显式声明。
  3. 更彻底：把 graph 移到 `application`，按 feature 暴露窄接口（`LibraryPort`、`ReaderPort` 等）。
- **工作量**：S（提升 `api`）+ M（拆再导出/窄接口）。

#### AR-04 `domain` 被 Compose 化，并通过 `contract` 传染给数据层

- **证据**：
  - `shared/domain/build.gradle.kts:2`：应用 `miyomu.kmp-compose`。
  - `build-logic/src/main/kotlin/miyomu.kmp-compose.gradle.kts:16-21`：把 Compose runtime/foundation/material3/materialIconsExtended/ui 全部作为 `api` 暴露。
  - `shared/domain/.../eu/kanade/core/preference/PreferenceMutableState.kt:3-4`：在 domain 中实现 Compose `MutableState`。
  - `shared/domain/.../eu/kanade/core/preference/CheckboxState.kt:3`：依赖 Compose `ToggleableState`。
  - `shared/domain/.../tachiyomi/domain/entries/manga/model/Manga.kt:3`：`@Immutable` 注解。
  - `shared/contract/build.gradle.kts:14`：`api(projects.shared.domain)`，而 contract 只为 `Category` 一个类型使用 domain（`CategoryRepository.kt:5`）。
- **影响**：领域层无法脱离 Compose；`contract`、`data` 的消费者被动继承 Compose API；未来增加 iOS/Web target 会被隐形阻塞；UI 适配器放错层。
- **建议**：
  1. 把 `PreferenceMutableState`、`asToggleableState` 等 Compose 适配器移到 `presentation-core`。
  2. `domain` 改回 `miyomu.kmp-library`；`AppTheme`/`ThemeMode` 若只是纯枚举可保留，但不要依赖 Compose。
  3. 把 `Category` 移到 `shared:model`，`contract` 改为依赖 `model`。
  4. `@Immutable` 若确需保留，需明确接受“领域模型带 Compose 注解”的取舍，并在 README/ADR 中记录。
- **工作量**：M（3–5 天）。

#### AR-05 `MiyomuGraph` 是上帝对象，生命周期无人负责

- **证据**：
  - `shared/ui-core/.../MiyomuGraph.kt:31-70`：20 余个依赖；`libraryUpdater` 创建不受管理的 `CoroutineScope(SupervisorJob() + Dispatchers.Default)`；`danmaku` 创建裸 `HttpClient()` 且无 `close()`。
  - `shared/ui/.../MiyomuApp.kt:31-54`：`MiyomuApp` 用约 18 个参数接收服务；`:59-79` 在 `remember` 中重建 graph。
  - `shared/application/.../MiyomuApplication.kt:43-106`：application 已经持有并关闭大部分服务。
  - 每个 screen 直接接收整个 graph，自行 collect flow、启动 DB/源任务（如 `ui-library`、`ui-entries`、`ui-sources`、`ui-novel` 的代表性 screen）。
- **影响**：任何 screen 都能访问任何服务；预览和测试必须构造完整图；graph 重建会泄漏 scope/client；`MiyomuApp` 的 18 参数签名难以维护。
- **建议**：
  1. 由 `MiyomuApplication` 或独立 factory 构建 graph，`MiyomuApp` 只接收一个对象。
  2. graph 持有 application 级 `CoroutineScope` 与已托管的 Ktor client，并实现 `Closeable`。
  3. 按 feature 拆端口；screen 只接收自身需要的端口与状态。
- **工作量**：M–L（1–2 周，分步）。

#### AR-06 Voyager `ScreenModel` 残留与 Navigation 3 并存

- **证据**：
  - `shared/presentation/build.gradle.kts:31`：`api(libs.voyager.screenmodel)`。
  - `shared/presentation/.../ui/library/manga/MangaLibrarySettingsScreenModel.kt:24-33`：继承 `ScreenModel`，在属性初始化时使用 `screenModelScope`。
  - `shared/presentation/.../ui/reader/setting/ReaderSettingsScreenModel.kt:13-29`：使用全局 `ioCoroutineScope`。
  - `shared/presentation/.../ui/entries/manga/MangaScreenModel.kt:27`：继承 `ScreenModel`，但从未被实例化（应用只使用其 `State`/`Dialog`）。
  - 运行时导航是 Navigation 3（`shared/ui/.../MiyomuNavHost.kt:18-20`），不经过 Voyager。
- **影响**：两个状态框架并存；`remember` 创建的 ScreenModel 不会经过 Voyager 注册，其 `stateIn` 作用域不会被释放；概念噪音增加。
- **建议**：改成普通类，构造参数注入 `CoroutineScope`；删除 `MangaScreenModel` 的 `ScreenModel` 继承；最终移除 `voyager-screenmodel` 依赖。
- **工作量**：S–M（1–2 天）。

### P2 — 目录与包结构

#### AR-07 包根分裂，缺少“移植代码”边界

- **证据**：
  - 约 50% 文件位于 `tachiyomi` / `eu.kanade` / `aniyomi` / `mihon` 包根；其余在 `app.miyomu`。
  - `shared/domain/build.gradle.kts:7`：namespace 为 `tachiyomi.core.common`。
  - `shared/presentation/build.gradle.kts:7`：namespace 为 `tachiyomi.presentation.manga`，但模块同时托管 anime。
  - `shared/source-api/build.gradle.kts:7`：namespace 为 `app.miyomu.source.api`，包名却是 `app.miyomu.source`。
  - `shared:domain` 一个模块跨 6 个包根，且不少文件已被裁剪改写，并非逐字镜像上游。
- **影响**：无法区分“上游同步代码”与“Miyomu 自有代码”；grep `tachiyomi` 会命中大量本地修改；namespace 与包名/职责不一致，增加认知成本。
- **建议**：为真正上游镜像的代码建立显式 `ported/` 边界（如 `miyomu.ported.eu.kanade.*`），Miyomu 自有代码迁出；至少先统一 namespace 与包名、模块职责。
- **工作量**：M（包重命名可机械化，但需分阶段）。

#### AR-08 主题系统重复约 1.6k 行

- **证据**：`shared/presentation/.../theme/colorscheme/` 有 20 个 palette 文件、约 1,605 行；`shared/ui-core/.../theme/*Colors.kt` 有 20 个 palette、约 1,421 行。`TachiyomiTheme.kt:37` 无运行时调用方，只有 `TachiyomiPreviewTheme` 被 10 个 preview 文件引用（26 处）。
- **影响**：两套主题会视觉漂移；`presentation` 体积无谓增大；preview 与运行时主题不一致。
- **建议**：删除 `TachiyomiTheme` 与重复 palette，preview 统一指向 `MiyomuTheme`；若需要 preview 包装，在 `ui-core` 提供一个薄封装。
- **工作量**：S（半天，机械替换）。

#### AR-09 `presentation` 巨型模块与 feature 间耦合

- **证据**：`presentation` 16,730 行、127 个文件，`AnimeScreen.kt` 单文件约 1,186 行；`ui-reader` 为了 `toDomainChapter`/`toManga` 两个 mapper 依赖 `ui-entries`/`ui-library`；`ui-novel` 也依赖 `ui-library`。
- **影响**：`presentation` 无法独立演化；feature 模块之间形成隐式层级；编译粒度粗。
- **建议**：先删重复主题，再抽 `presentation-components`（`components` + `util` + settings widget，约 1.8k 行）；把两个 mapper 移到 `ui-core`。拆分 `entries/` 放在边界稳定之后。
- **前置条件**：先完成 AR-03，否则 feature 无法命名新模块。
- **工作量**：M–L。

#### AR-10 同一包名跨模块

- **证据**：`app.miyomu.ui` 同时存在于 `ui-core`（`MiyomuGraph.kt:1`）与 `ui`（`MiyomuApp.kt:1`）；`app.miyomu.ui.novel`、`app.miyomu.reader` 也跨模块。
- **影响**：不是编译错误，但“哪个模块拥有这个文件”不可回答；JPMS 下会形成 split package；IDE 导航与代码归属变模糊。
- **建议**：让包名与模块后缀对齐（如 `app.miyomu.ui.core` / `app.miyomu.ui.app`），或至少把 `ui-core` 改名为 `ui-foundation` 并把 graph 移到 application。
- **工作量**：S–M。

### P3 — 构建、CI 与测试

#### AR-11 Convention Plugins 的 API 面过宽

- **证据**：
  - `build-logic/src/main/kotlin/miyomu.kmp-compose.gradle.kts:16-21`：每个 Compose 模块都 `api` 获得 runtime/foundation/material3/materialIconsExtended/ui。
  - `build-logic/src/main/kotlin/miyomu.kmp-library.gradle.kts:23`：所有模块 `androidResources.enable = true`。
  - `build-logic/src/main/kotlin/miyomu.kmp-library.gradle.kts:35-36`：`jvmShared` 只是 `srcDir` 别名，没有独立依赖块，导致 ksoup/quickjs 等依赖在 `jvmMain`/`androidMain` 重复声明；`source-novel/build.gradle.kts:21,25` 甚至重复声明了 quickjs。
- **影响**：编译内存与时间增加；桌面产物携带未使用的 icons；API 面不受控；IDE 无法把 `jvmShared` 识别为共享源集。
- **建议**：拆出 runtime-only 约定（`domain`/`application` 用）；`materialIconsExtended` 改为按需 `implementation`；`androidResources` 默认关闭、按模块开启；用 hierarchy template 表达 `jvmShared`。
- **工作量**：S–M。

#### AR-12 CI 质量门禁偏薄

- **证据**：`.github/workflows/ci.yml:37-39,57` 只跑 `jvmTest`、`:app:desktop:compileKotlin`、`:app:android:assembleDebug`；没有 Android lint、ktlint/detekt、`check`、release/R8。`shared/source-runtime/build.gradle.kts:29-35` 默认排除 `canary` tag，而 CI 从未传 `-Pcanary`，因此 `CanarySourceTest` 永不执行。
- **影响**：Android 资源/manifest/API 问题不会被拦截；静态分析缺失；canary 源测试形同虚设。
- **建议**：增加 `:app:android:lintDebug`、ktlint/detekt、定时 `-Pcanary` job；评估启用 Gradle configuration cache。
- **工作量**：S（CI 配置）+ M（清理既有告警）。

#### AR-13 测试布局与覆盖倒挂

- **证据**：55 个测试全部在 `jvmTest`，没有 `commonTest`；`presentation`（16.7k 行）与 `presentation-core`（5k 行）零测试；Android actual（数据库工厂、图片解码）只编译不测试；Compose UI 测试使用真实 JVM 服务与固定 sleep（如 `MiyomuAppTest`）。
- **影响**：平台无关逻辑没有跨目标验证；Android 专有行为无测试；旋转/返回键/SAF 等关键路径无覆盖。
- **建议**：把平台无关测试迁入 `commonTest`；用 fake `DataPort` 测导航/状态；补一个 Android 旋转/返回键冒烟测试；逐步为 `ui-core` 的导航与状态持有者加测试。
- **工作量**：M。

#### AR-14 可低风险清理的依赖

- **证据**：
  - `ui-core` 的 `player`、`reader-comic`、`data` 在 main 源码中未被使用；`data` 仅 `jvmTest` 使用（`LibraryUpdaterTest.kt:4-5`）。
  - `presentation-core/build.gradle.kts:23` 声明 `api(domain)`，但 `commonMain` 没有任何 domain import。
  - `contract/build.gradle.kts:14` 的 `api(domain)` 只为 `Category` 一个类型。
  - `source-novel/build.gradle.kts:21,25` 重复声明 quickjs。
- **建议**：删除/降级未使用依赖；`data` 改为 `jvmTest` 依赖；`Category` 迁到 `model` 后去掉 contract→domain。
- **工作量**：S。

---

## 6. 建议路线图

| 阶段 | 动作 | 验收标准 |
|---|---|---|
| **立刻（1–2 天）** | AR-01 导航保存与服务生命周期；AR-02 移除主线程 `runBlocking`；AR-03 先提升公开依赖为 `api`；AR-08 删除重复主题；AR-12 加 `lintDebug` | 旋转不丢页、下载不中断；`ui-core` 公开签名与依赖声明一致；主题只剩一套；CI 出现 lint 任务 |
| **本迭代（1–2 周）** | AR-05 graph 移入 application 并拆窄接口；AR-04 domain 去 Compose；AR-06 替换 Voyager ScreenModel；AR-14 清理依赖；AR-13 建立 `commonTest` | `domain` 不再依赖 Compose；feature 显式声明 `presentation`；无 `ScreenModel` 运行时使用；关键导航/状态有测试 |
| **中期（1–2 月）** | AR-07 建立 `ported/` 包边界并统一 namespace；AR-09 拆分 `presentation`；AR-10 包名与模块对齐；AR-11 收窄 Convention Plugins；AR-12 开启 configuration cache 与 canary CI | 包名可区分自有/移植代码；`presentation` 至少拆出 components；构建缓存命中率提升 |
| **持续护栏** | 写 ADR：移植代码边界、依赖方向、状态归属；引入 Konsist/ArchUnit 或 Gradle dependency-analysis；CI 运行架构规则 | 新代码无法再引入 `domain → Compose`、`ui → data 实现`、`feature → presentation 再导出` |

### 不建议现在做的事

- **不要先拆 `presentation`**：在 AR-03 修好之前，feature 模块无法显式依赖新模块，拆了也会被 `ui-core` 的再导出掩盖。
- **不要先大规模重命名包**：在 `ported/` 边界与 namespace 策略确定前重命名，会把上游同步成本放大。
- **不要为了“纯架构”删掉 `@Immutable`**：如果保留领域模型直接进 Compose，需用 ADR 记录取舍；优先级低于把 UI 适配器移出 domain。

---

## 7. 架构护栏建议

1. **依赖方向规则**
   - `domain` / `model` / `contract` 不得依赖 Compose。
   - `ui-*` 不得依赖具体数据实现（`data`、`local`、`source-runtime` 等），只依赖端口。
   - feature 模块不得依赖另一个 feature 模块；共享 UI 工具放 `presentation-core` 或 `ui-core`。

2. **公开 API 规则**
   - 公开类型出现在签名中的模块，必须在构建文件中以 `api` 声明。
   - 禁止用 `api` 再导出整个 `presentation`；feature 自己声明。

3. **移植代码规则**
   - 上游镜像代码放 `ported/` 包根，保留来源与 commit 说明。
   - Miyomu 自有修改放在独立包/文件，升级上游时只 diff `ported/`。

4. **状态与生命周期规则**
   - 长生命周期服务（DB、HTTP、下载、源运行时）不得由 composition 创建。
   - screen 状态必须可预览：immutable state + callbacks；副作用通过生命周期匹配的 effect。
   - 导航状态必须可保存/恢复，并有测试。

5. **CI 规则**
   - 必跑：`jvmTest`、`lintDebug`、静态分析、`assembleDebug`。
   - 定期跑：`-Pcanary`、release/R8、桌面打包。
   - 架构规则用测试固化（Konsist/ArchUnit），不要只靠文档。

以上规则已固化到根目录 `AGENTS.md`（《Agent 协作与 Skill 调用规范》），所有在本仓库工作的 agent 必须遵守；新增或修改规则时两份文档需同步更新。

---

## 8. 附录

### A. 复现与核验命令

```bash
# 模块列表
./gradlew projects --console=plain

# 各模块文件/行数
for d in shared/*/ app/*/; do
  n=$(find "$d/src" -name '*.kt' 2>/dev/null | wc -l | tr -d ' ')
  l=$(find "$d/src" -name '*.kt' -exec cat {} + 2>/dev/null | wc -l | tr -d ' ')
  printf '%-24s %5s %8s\n' "$d" "$n" "$l"
done

# 包根分布
grep -rhoE '^package [a-zA-Z0-9_.]+' shared app --include='*.kt' \
  | awk '{print $2}' | cut -d. -f1-2 | sort | uniq -c | sort -nr

# 主题重复
ls shared/ui-core/src/commonMain/kotlin/app/miyomu/ui/theme/*Colors.kt | wc -l
ls shared/presentation/src/commonMain/kotlin/eu/kanade/presentation/theme/colorscheme/*.kt | wc -l

# 测试作用域
grep -rn 'coroutines.test' shared/*/build.gradle.kts
```

### B. 关键文件索引

| 主题 | 文件 |
|---|---|
| 组合根 | `shared/application/src/jvmShared/kotlin/app/miyomu/application/MiyomuApplication.kt` |
| App Shell | `shared/ui/src/commonMain/kotlin/app/miyomu/ui/MiyomuApp.kt` |
| 导航入口 | `shared/ui/src/commonMain/kotlin/app/miyomu/ui/MiyomuNavHost.kt` |
| 导航状态 | `shared/ui-core/src/commonMain/kotlin/app/miyomu/ui/navigation/MiyomuNavigator.kt` |
| 路由键 | `shared/ui-core/src/commonMain/kotlin/app/miyomu/ui/navigation/MiyomuKey.kt` |
| 服务图 | `shared/ui-core/src/commonMain/kotlin/app/miyomu/ui/MiyomuGraph.kt` |
| 约定插件 | `build-logic/src/main/kotlin/miyomu.kmp-library.gradle.kts`、`miyomu.kmp-compose.gradle.kts` |
| 版本目录 | `gradle/libs.versions.toml` |
| CI | `.github/workflows/ci.yml` |
| Agent 规范 | `AGENTS.md` |
| 产品目标 | `docs/product-goals.md` |
| 目标架构 | `docs/target-architecture.md` |

### C. 待确认项

- `MiyomuGraph` 的 20 余个依赖中，是否存在只被少数 screen 使用的服务；拆分端口前应先用调用图确认。
- `@Immutable` 是否确实带来 Compose 跳过收益；若没有，移除比保留更干净。
- `presentation` 中哪些 screen 属于“仍在使用的移植 UI”，哪些只是被 preview 或历史代码引用；拆分前先做引用统计。
- `source-api` 的 namespace `app.miyomu.source.api` 与包名 `app.miyomu.source` 是否有意为之；若无，随包名统一一起修。

---

**报告结束**。本报告为 2026-10-30 的快照；修复后建议重新运行第 8 节命令并更新健康度评分。
