# Miyomu

**跨平台三合一内容阅读器** · 名称来自 見る（看）+ 読む（读）

漫画、小说、视频，一套库、一套源、一套进度。以 Aniyomi 的 Android 体验为设计基准，用 Kotlin Multiplatform 共享核心、Compose Multiplatform 构建跨平台 UI，用 JavaScript 扩展源替代 Android-only 的 APK 扩展模型。

> **当前状态（2026-10-30）**：项目**未达到可用 / 可发布状态**；用户实机测试判定整体不可用。功能开发与打包/发布暂停。
> 测试数量、源构造和静态审计都不是可用性证据。详见 [00 当前状态](docs/00-current-status.md)。

## 代码结构

| 模块 | 作用 |
|---|---|
| `:shared:model` | 领域模型、源描述、章节/页面 |
| `:shared:network` | Ktor 3.6、OkHttp/CIO、持久 Cookie |
| `:shared:source-api` | `ComicSource` / `AnimeSource` 及其托管接口 |
| `:shared:source-runtime` | 每源 QuickJS、Ktor、Ksoup DOM、KV、配额 |
| `:shared:source-repo` | 源仓库索引、安装/更新/卸载与本地清单 |
| `:shared:source-manager` | 已安装源的运行时加载与搜索/详情/页面/图片/视频解析 |
| `:shared:source-novel` | legado 书源 JSON 导入、规则求值器、在线内容源 |
| `:shared:download` | 漫画章节下载队列、离线页源 |
| `:shared:danmaku` | dandanplay 弹幕客户端（用户自配凭据） |
| `:shared:contract` | UI 端口：仓储接口与记录类型（无 Room / 网络 / 平台代码） |
| `:shared:data` | Room 2.8.5 + KSP（schema v4）；实现 `:shared:contract` 的仓储 |
| `:shared:local` | 本地文件抽象、书库扫描、目录/ZIP 页加载 |
| `:shared:novel` | 小说解析：TXT/EPUB/FB2/HTML/Markdown、小说库扫描 |
| `:shared:reader-comic` | 漫画阅读状态、模式、页配对 |
| `:shared:reader-novel` | 小说阅读偏好、分页数学、阅读控制器 |
| `:shared:player` | mediamp 播放器、播放器 chrome、音轨/字幕轨选择 |
| `:shared:domain` | Aniyomi 域模型、偏好 API、排序/集号识别 |
| `:shared:i18n` / `:shared:i18n-aniyomi` | moko-resources 字符串资源（en + zh-CN） |
| `:shared:presentation-core` / `:shared:presentation` | 移植的 Aniyomi 组件与屏幕层 |
| `:shared:ui-core` | `MiyomuGraph`、主题、Navigation 3 键、共享组件 |
| `:shared:ui-library` / `ui-entries` / `ui-novel` / `ui-reader` / `ui-sources` / `ui-player` / `ui-settings` | 功能界面模块（单向依赖） |
| `:shared:ui` | App 壳：`MiyomuApp`、`MiyomuNavHost`、`HomeScreen` |
| `:shared:application` | 组合根：数据、偏好、源运行时、下载、小说抓取的统一装配 |
| `:app:android` / `:app:desktop` | Android / 桌面入口，只提供平台环境 |

工具链：AGP 9.4.1 + Kotlin 2.3.21 + Gradle 9.6.0 + compileSdk 37 + JDK 21。
构建约定集中在 `build-logic/`（`miyomu.kmp-library`、`miyomu.kmp-compose`、
`miyomu.android-application`、`miyomu.jvm-compose`），模块脚本不再重复 toolchain/sdk/test 配置。

## 构建与运行

```bash
# 桌面（先生成一个样例书库）
python3 tools/make_sample_library.py
./gradlew :app:desktop:run

# Android debug 包
./gradlew :app:android:assembleDebug

# 模块测试（不代表可用性验收）
./gradlew jvmTest
```

## 原则

- **一个源引擎**：JS/QuickJS，不再维护两套扩展模型。
- **一个播放器抽象**：mediamp，按平台后端实现。
- **一套数据层**：库、进度、下载、同步共享。
- **清洁实现**：不复制 GPL 代码，不内置任何内容源，不托管内容。

## 文档

- [00 当前状态](docs/00-current-status.md)（单一事实来源）
- [01 架构概览](docs/01-architecture.md)
- [02 关键决策](docs/02-decisions.md)
- [03 验证记录](docs/03-verification.md)
- [文档索引](docs/README.md)

## 许可证与免责声明

Apache-2.0（见 [02 关键决策](docs/02-decisions.md)）。本项目只提供阅读器与扩展运行环境，不提供、内置、托管或推荐任何内容源；第三方源由用户自行配置，其内容与可用性由相应服务决定。
