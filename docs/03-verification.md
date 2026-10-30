# 验证记录（窄范围）

> 本文只记录“测过什么、没测什么”。**不是可用性报告。** 当前状态见 [00-current-status.md](00-current-status.md)。

## 模块 / fixture 测试

- 213 个 JVM 测试通过（2026-10-30），0 失败、0 跳过。
- 分布：`novel` 30、`source-novel` 29、`source-runtime` 22、`ui` 21、`reader-novel` 16、`player` 12、`network` 10、`data` 10、`source-repo` 9、`ui-entries` 9、`reader-comic` 8、`ui-reader` 7、`local` 7、`ui-novel` 4、`danmaku` 4、`ui-library` 4、`download` 4、`domain` 3、`source-manager` 3、`ui-core` 1。
- 这些测试主要用 fixture、内存数据或 headless UI，**不覆盖**真实用户导入、真实第三方源、真实网络稳定性或重启后的完整状态。

## 架构（构建验证）

- `build-logic` 约定插件接管 30 个模块的 toolchain / compileSdk / Android / JUnit 配置。
- 入口为 `:app:android` / `:app:desktop`，组合根为 `:shared:application`。
- 验证命令：`./gradlew jvmTest`、`:app:desktop:compileKotlin`、`:app:android:assembleDebug`。
- 这只证明结构改动可以编译并跑通既有模块测试，**不证明**真实导入、阅读/播放或重启持久化。

## 导航走查（2026-10-30，真实点击 + 真实图 + 真实 Room）

`NavWalkthroughTest`（3 个用例）在真实 `MiyomuApp` / `MiyomuNavHost` 上逐页点击：

- 六个 Tab 都能切换，窄/宽两种壳（底部栏 / 侧栏）都正常。
- 设置链：设置 → 外观 / 阅读 / 下载 / 数据与存储 / 弹幕 / 关于 Miyomu，**每一页都能打开并 pop 回设置页**。
- 更多页可达：源仓库、备份与同步、下载队列，均能打开并 pop 回。
- 漫画：书架 → 详情 → 阅读器 → pop 回详情 → pop 回书架，三层都对。
- 番剧源：浏览 → 源 → 搜索 → 详情 → 播放器（MPV 真的起来了，返回详情正常）。
- 仍未覆盖：全局搜索入口、`OnlineReaderKey`（由 `SourceBrowseFlowTest` 覆盖）、进程重建后的回退栈恢复。

### 走查发现的非路由问题（未修）

1. `MoreTab` 的「本地库」和「关于 Miyomu」两行没有 `onClick`，点了没反应；`MiyomuGraph.onOpenLibraryFolder` 已存在但没接上。
2. 播放器失败态直接显示 `mpv playback failed (mpv_error=-13)`：未本地化，且把 mpv 错误码暴露给用户，没有重试。
3. 本地库扫描会把**空目录**当成一本书（走查里 `sources/` 目录出现在书架上，封面空白）。

## 导航（2026-10-30）

- 4 个 UI 流程测试改为渲染真实的 `MiyomuNavHost`（`start = <Key>`），因此它们现在覆盖键到页面的映射，
  而不只是单个 composable：`GlobalSearchNovelTest`、`NovelExploreFlowTest`（2 个）、其余 `MiyomuApp` 流程测试不变。
- 仍未覆盖：进程重建后的回退栈恢复（`rememberMiyomuNavigator` 用的是 `remember`，未接 `rememberNavBackStack`）。

## Venera 源

- 13 个参考源：13/13 可零改构造；静态 API 审计 0 缺口。
- hitomi：search 25 → details 1 章 → pages 30 页，桌面 JVM 与 Android 模拟器能跑通这条路径。
- comick：HTTP 522；manga_dex：TLS `ProtocolVersion` alert（本机网络/站点问题）。
- 这些只覆盖构造、静态 API 面和 hitomi 单路径，**不证明**阅读器整体可用，也不证明其他源能真实阅读。

## 手动冒烟（预置数据 / 本地 fixture）

- 本地 TXT：扫描 → 详情 → 阅读器。
- legado 本地 fixture：浏览 → 发现 → 分类 → 详情 → 章节 → 阅读。
- 本地 MKV：详情 → 播放器。
- 设置、备份、源仓库、下载队列等页面能打开。
- 这些路径用的是我们准备的数据，不是普通用户的真实使用路径。

## 不能由上述结果推出的结论

- 不能证明全新安装后能导入真实书/真实源。
- 不能证明阅读、播放、搜索、进度、书架、源配置在重启后仍然可用。
- 不能证明真实第三方源覆盖、登录态、错误处理、性能或长期稳定性。
- 不能把测试数量、构造成功或静态审计当作功能完成/可用。

## 复现命令

```bash
# 模块测试（不代表可用性验收）
./gradlew jvmTest

# 入口编译
./gradlew :app:desktop:compileKotlin :app:android:assembleDebug

# Venera 静态审计 + 构造（需要本地 reference/）
python3 tools/venera_compat.py reference/venera-yuan/*.js --summary
./gradlew :shared:source-runtime:jvmTest --tests '*VeneraSourceCompatTest*'
```
