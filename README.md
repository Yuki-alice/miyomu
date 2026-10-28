# Miyomu

**跨平台三合一内容阅读器** · 名称来自 見る（看）+ 読む（读）

漫画、小说、视频，一套库、一套源、一套进度。以 Aniyomi 的 Android 体验为设计基准，用 Kotlin Multiplatform 共享核心、Compose Multiplatform 构建跨平台 UI，用 JavaScript 扩展源替代 Android-only 的 APK 扩展模型。

> 状态：**Phase 1 — M1 核心与单媒介（进行中）**
> 许可证：Apache-2.0（2026-10-28 确认，见 [ADR-0004](docs/adr/0004-licensing-and-clean-room.md)）
> 生产构建位于仓库根目录；Phase 0/1 验证代码在 `experiments/`，使用独立 Gradle 构建。

## 模块

| 模块 | 作用 |
|---|---|
| `:shared:model` | 领域模型、源描述、章节/页面 |
| `:shared:network` | Ktor 3.6、OkHttp/CIO、持久 Cookie |
| `:shared:source-api` | `ComicSource` / `ManagedComicSource` |
| `:shared:source-runtime` | 每源 QuickJS、Ktor、Ksoup DOM、KV、配额 |
| `:shared:data` | Room 2.8.5 + KSP；库/进度 schema 与 repository |
| `:androidApp` | Compose Android 调试壳 |
| `:desktopApp` | Compose Desktop 调试壳 |

工具链：AGP 9.4.1 + Kotlin 2.3.21 + Gradle 9.6 + compileSdk 37 + JDK 21。

## Phase 0 回顾

Phase 0 不做产品功能，只回答三个问题（已完成）：

| Spike | 要回答的问题 | 规格 |
|---|---|---|
| S1 播放器 | mediamp 能否在 Android / 桌面 / iOS 三端撑起播放器？ | [docs/spikes/s1-player.md](docs/spikes/s1-player.md) |
| S2 源运行时 | QuickJS 能否在 KMP 里安全、稳定地跑 Venera 兼容源？ | [docs/spikes/s2-js-runtime.md](docs/spikes/s2-js-runtime.md) |
| S3 UI | Aniyomi 的 Compose 屏幕迁移到 CMP + MD3E 的成本是否可接受？ | [docs/spikes/s3-cmp-ui.md](docs/spikes/s3-cmp-ui.md) |

决策门与详细规格见 [Phase 0 计划](docs/02-phase-0.md)。

Phase 0 已完成：决策 **Go（缩范围）** —— Android + 桌面首发，iOS 后置。S1 桌面/Android 播放通过（iOS 待 App 壳），S2 三端源运行时通过，S3 三屏 headless 验证通过。

Phase 1 M0 已完成：单轨工具链（AGP 9.4.1 + Kotlin 2.3.21 + compileSdk 37）跑通 QuickJS、mediamp、Compose、Ktor；Ktor + 持久 Cookie 网络层通过 JVM 测试与 Android 运行时验证。

Phase 1 M1 进行中：生产构建已移到仓库根目录，`:shared:source-runtime` 以每源 QuickJS + Ktor + 持久 Cookie + Ksoup DOM + KV 实现 Venera 风格读路径；桌面与 Android 调试壳用同一 HTML 源跑通搜索→详情→章节→图片。Room 2.8.5 数据层已接入，打开作品/章节会持久化库与进度。真实 Venera 源对照、正式阅读器与播放器轨道选择仍在进行。详见 [M1 清单](docs/checklists/phase-1-m1.md) 与 [ADR-0008](docs/adr/0008-venera-source-runtime.md) / [ADR-0009](docs/adr/0009-room-kmp-data-layer.md)。

## 仓库结构

```text
androidApp/     Android 应用入口（Compose）
desktopApp/     桌面应用入口（Compose Desktop）
shared/         共享 KMP 模块
  model/ network/ source-api/ source-runtime/ data/
demo-resources/ 本地自造 fixture 源
tools/          本地 fixture server
evidence/       M1 双端读路径截图
docs/           产品、架构、Phase 1 计划、ADR、风险
experiments/    Phase 0/1 验证代码（独立 Gradle 构建）
  phase1/       M0 工具链与网络层探针
  spikes/       Phase 0 S1/S2/S3
reference/      本地参考仓库（gitignored）
```

## 快速开始

前提：JDK 21。首次运行需要联网下载依赖。

```bash
./gradlew :desktopApp:run
```

编译产品模块：

```bash
./gradlew :desktopApp:build
./gradlew :androidApp:assembleDebug
./gradlew :shared:source-runtime:jvmTest :shared:data:jvmTest
```

验证代码是独立构建，从仓库根目录执行：

```bash
./gradlew -p experiments :spikes:s3-ui:run
```

## 本地 demo

先启动自造 HTML/图片 fixture：

```bash
python3 tools/fixture_server.py --port 18080
```

桌面：

```bash
./gradlew :desktopApp:run
```

搜索 `demo` → 打开结果 → 打开章节；应显示 3 页并加载首图。

Android：

```bash
./gradlew :androidApp:assembleDebug
adb install -r androidApp/build/outputs/apk/debug/*.apk
```

模拟器通过 `10.0.2.2:18080` 访问主机 fixture；真机需改 `androidApp` 的 `FixtureBaseUrl` 或使用端口转发。

Room schema 导出在 `shared/data/schemas/`；修改实体后需同时更新 schema/迁移。

## 源脚本

`demo-resources/sources/demo_source.js` 是自写的 Venera 风格源，使用：

- `class ... extends ComicSource`（宿主构造并注册，不需要手动注册）
- `search.load` / `comic.loadInfo` / `comic.loadEp`
- `Network.get`、`HtmlDocument`、`this.loadData/saveData`

真实第三方源只用于本地验证，不提交；兼容范围见 [ADR-0008](docs/adr/0008-venera-source-runtime.md)。

## 已知限制

- 真实 Venera 源尚未零修改验证。
- `Network.setCookies/getCookies/deleteCookies` 同步 JS API、`fetch`、search options、explore/categories/settings/login 未实现。
- 调试壳只显示首图与页面 URL；正式阅读器、库/进度、下载/同步、播放器轨道选择在后续里程碑。

## 文档

- [产品简报](docs/00-product-brief.md)
- [目标架构](docs/01-architecture.md)
- [Phase 0 计划](docs/02-phase-0.md)
- [风险登记](docs/03-risk-register.md)
- [开放问题](docs/04-open-questions.md)
- [Phase 0 启动清单](docs/checklists/phase-0.md)
- [Phase 1 计划](docs/05-phase-1-plan.md)
- [Phase 1 M0 清单](docs/checklists/phase-1-m0.md)
- [Phase 1 M1 清单](docs/checklists/phase-1-m1.md)
- [品牌备忘](docs/06-brand.md)
- [Skills 与参考仓库](docs/07-skills-and-references.md)
- ADR：[KMP/CMP](docs/adr/0001-kmp-cmp-baseline.md) · [Fork + Strangler](docs/adr/0002-fork-and-strangler.md) · [JS 扩展运行时](docs/adr/0003-js-extension-runtime.md) · [许可证边界](docs/adr/0004-licensing-and-clean-room.md) · [mediamp](docs/adr/0005-mediamp-player.md) · [单轨工具链](docs/adr/0006-unified-android-toolchain.md) · [网络与 Cookie](docs/adr/0007-network-and-cookie-storage.md) · [Venera 源运行时](docs/adr/0008-venera-source-runtime.md) · [Room 数据层](docs/adr/0009-room-kmp-data-layer.md)

## 原则

- **一个源引擎**：JS/QuickJS，不再维护两套扩展模型。
- **一个播放器抽象**：mediamp，按平台后端实现，功能差异显式记录。
- **一套数据层**：库、进度、下载、同步共享；三种媒介各自拥有阅读/播放界面。
- **增量交付**：迁移期间 Android 版必须持续可发布。
- **清洁实现**：不复制 GPL 代码，不内置任何内容源，不托管内容。

## 免责声明

本项目只提供阅读器与扩展运行环境，不提供、内置、托管或推荐任何内容源。第三方源由用户自行配置，其内容与可用性由相应服务决定；使用者需自行承担合规责任。
