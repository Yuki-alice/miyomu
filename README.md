# Miyomu

**跨平台三合一内容阅读器** · 名称来自 見る（看）+ 読む（读）

漫画、小说、视频，一套库、一套源、一套进度。以 Aniyomi 的 Android 体验为设计基准，用 Kotlin Multiplatform 共享核心、Compose Multiplatform 构建跨平台 UI，用 JavaScript 扩展源替代 Android-only 的 APK 扩展模型。

> **当前状态（2026-10-30，用户实机测试后）**：项目**未达到可用/可发布状态**。用户实机测试的结论是“功能基本上就是做做样子，在真实的使用环境下完全不可用”。功能开发已暂停，打包/发布暂停。
>
> **不要用测试数量判断可用性**：当前测试以模块级 fixture、源构造和静态审计为主，只能证明部分代码路径能跑，**不能证明**全新安装后导入真实书/真实源、搜索、阅读/播放、重启持久化等真实流程可用。

## 能确认的事实

- Android 与桌面代码能编译，桌面 App 能启动窗口。
- 213 个模块/fixture 测试通过（数量不等于可用性）。
- Venera：13 个参考源能零改构造；静态 API 审计 0 缺口；hitomi 单源 search → details → pages 能取到数据（仅桌面/模拟器单路径）。
- 用预置数据 + 本地 fixture 做过少量冒烟：本地 TXT、legado 发现链路、本地 MKV、设置/备份/源仓库页面。

详细范围见 [03 验证记录](docs/03-verification.md) 与 [00 当前状态](docs/00-current-status.md)。

## 未验证 / 已知失败面

- **真实用户路径**：全新安装 → 导入本地书/真实源 → 搜索/详情 → 阅读/播放 → 重启 → 进度/书架/源保留。
- 用户已判定整体不可用；目前没有整理出完整的失败清单，需要按真实路径逐项复现。
- 除 hitomi 单路径外的真实第三方源、登录态、错误处理、长时稳定性。
- 性能、Windows、iOS、安装包/发布产物。
- 已观察的 UX 问题：阅读器首段文字被顶栏遮挡；本地 TXT 详情有 “Missing 1 item” 封面占位；底部 Tab 中文、小说 UI 英文。

## 代码结构

| 模块 | 作用 |
|---|---|
| `:shared:model` | 领域模型、源描述、章节/页面 |
| `:shared:network` | Ktor 3.6、OkHttp/CIO、持久 Cookie |
| `:shared:source-api` | `ComicSource` / `ManagedComicSource`、`AnimeSource` / `ManagedAnimeSource` |
| `:shared:source-runtime` | 每源 QuickJS、Ktor、Ksoup DOM、KV、配额 |
| `:shared:source-repo` | 源仓库索引、安装/更新/卸载与本地清单 |
| `:shared:source-manager` | 已安装源的运行时加载与搜索/详情/页面/图片/视频解析 |
| `:shared:source-novel` | legado 书源 JSON 导入、规则求值器、在线内容源 |
| `:shared:download` | 漫画章节下载队列、离线页源 |
| `:shared:danmaku` | dandanplay 弹幕客户端（用户自配凭据） |
| `:shared:contract` | UI 端口：仓储接口与记录类型（无 Room / 网络 / 平台代码） |
| `:shared:data` | Room 2.8.5 + KSP（schema v4）；库/进度/章节/剧集/分类/更新/下载/解析缓存 |
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
# 桌面
python3 tools/make_sample_library.py
./gradlew :app:desktop:run

# Android debug 包
./gradlew :app:android:assembleDebug

# 模块测试（不代表可用性验收）
./gradlew :shared:ui:jvmTest :shared:source-runtime:jvmTest :shared:source-novel:jvmTest \
  :shared:novel:jvmTest :shared:reader-novel:jvmTest :shared:data:jvmTest
```

`experiments/` 是独立的 Phase 0/1 验证构建，使用 `./gradlew -p experiments ...`。

## 实现记录（代码路径，未验收）

- 漫画/视频/小说本地与在线阅读、播放器的代码路径存在。
- 源运行时、源仓库、下载、备份/WebDAV、弹幕的代码路径存在。
- 小说端 legado 书源、发现/分类页、`nextContentUrl` 的代码路径存在。
- 上述都不代表真实使用可用；验收状态以 [00 当前状态](docs/00-current-status.md) 为准。

## 文档

- [00 当前状态](docs/00-current-status.md)（单一事实来源）
- [01 架构概览](docs/01-architecture.md)
- [02 关键决策](docs/02-decisions.md)
- [03 验证记录](docs/03-verification.md)
- [文档索引](docs/README.md)

旧 ADR、计划、交接、清单与 Spike 文档已于 2026-10-30 删除，正文可在 git 历史 `14c495e` 查看。

## 原则

- **一个源引擎**：JS/QuickJS，不再维护两套扩展模型。
- **一个播放器抽象**：mediamp，按平台后端实现。
- **一套数据层**：库、进度、下载、同步共享。
- **清洁实现**：不复制 GPL 代码，不内置任何内容源，不托管内容。

## 许可证与免责声明

Apache-2.0（见 [02 关键决策](docs/02-decisions.md)）。本项目只提供阅读器与扩展运行环境，不提供、内置、托管或推荐任何内容源；第三方源由用户自行配置，其内容与可用性由相应服务决定。
