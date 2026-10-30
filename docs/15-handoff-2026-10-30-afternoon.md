# 15 交接（2026-10-30 下午）

> 给下一个接手的人/agent。上一份是 [11 号交接](11-handoff-2026-10-30.md)（Phase D 结束时），
> 本份只覆盖之后发生的事：**小说端合入主线 + 三处接线的缺口修复 + Venera 源兼容性量化**。
> 基线：`main` = `ca1d05b`，已与 `origin/main` 同步。

## 1. 一句话状态

三种媒介（漫画 / 视频 / 小说）已经在同一条 `main` 上跑通，**199 个 JVM 测试全绿**，桌面编译与 Android 打包通过。
主要缺口从"功能没做"变成了"少数宿主 API 没铺开"和"从没发布过产物"。

## 2. 这 18 个提交做了什么

| 提交 | 内容 |
|---|---|
| `ca1d05b` | 实现 Venera 的 `Convert`（编码/摘要/HMAC/AES），宿主侧 |
| `c4011d7` | Venera 源兼容性的量化手段（静态审计工具 + 构造探针）与一批廉价 API 补齐 |
| `3dd79ff` | 全局搜索覆盖 legado 小说源（此前只搜 2/3 媒介） |
| `09b11ed` | 把 `.workbuddy/` 从 git 里摘出去并加入 `.gitignore` |
| `e29d7ef` | 书内搜索接线（引擎与单测都在，UI 传的是永远返回空的 lambda） |
| `4d2376f` | **小说端合入**：`feature/novel` 的 11 个提交以 merge commit 并入 |
| 其余 | 交接文档与测试计数的更新 |

合并没有动 `shared/data` 与 Room schema（小说书签/阅读偏好/legado 书源全在 JSON `PreferenceStore` 里），
所以**没有数据迁移风险**。合并时解的冲突按"保 main 的 Phase C/D 能力 + 挂上小说"处理。

## 3. 验证基线（改完必须复跑这几条）

```bash
./gradlew :shared:ui:jvmTest :shared:novel:jvmTest :shared:reader-novel:jvmTest \
  :shared:source-novel:jvmTest :shared:player:jvmTest :shared:data:jvmTest \
  :shared:local:jvmTest :shared:source-runtime:jvmTest :shared:source-repo:jvmTest \
  :shared:source-manager:jvmTest :shared:reader-comic:jvmTest :shared:network:jvmTest \
  :shared:download:jvmTest :shared:danmaku:jvmTest :shared:domain:jvmTest \
  :desktopApp:compileKotlin :androidApp:assembleDebug
```

当前：**199 tests / 0 failures**。分布：`ui` 41、`novel` 30、`source-novel` 25、`reader-novel` 16、
`player` 12、`source-runtime` 17、`data` 10、`network` 10、`source-repo` 9、`reader-comic` 8、
`local` 7、`danmaku` 4、`download` 4、`source-manager` 3、`domain` 3。

Venera 兼容性另有两套：

```bash
python3 tools/venera_compat.py reference/venera-yuan/*.js --summary   # 静态审计
./gradlew :shared:source-runtime:jvmTest --tests '*VeneraSourceCompatTest*'
# 报告：build/reports/venera-compat.md
```

## 4. 环境前置

- **`reference/` 是本地目录，gitignored，不在仓库里**。换机器后需要自己克隆，否则
  `VeneraSourceCompatTest` 会 skip（不会失败），审计工具也没有输入：
  `git clone --depth 1 https://github.com/yzlvhell/venera-yuan reference/venera-yuan`
- JDK 21；Gradle 用仓库自带 wrapper。
- **不要提交 `reference/` 里的任何东西**：那是第三方脚本，许可证与内容都不属于本项目，
  文档只记录测量结果和文件名。

## 5. 这轮踩过的坑（别再踩一遍）

1. **`git add -A` 会把 `.workbuddy/` 卷进提交**。已经 gitignore 了，但仍要注意工作区文件。
   另外 `4d2376f` 那个合并提交的历史里还留着一份 `.workbuddy/memory` 快照，要彻底清得改写历史。
2. **测试里绕开 `MiyomuApp` 直接渲染路由，必须先 `installLocalCoverLoader { null }`**。
   否则 coil 单例以"未播种"状态被创建，同 JVM 里后续每个测试调 `setSafe` 都会抛异常——
   一次能让十几个无关测试变红。这个坑花了很久才定位。
3. **Gradle 测试的 cwd 是模块目录**（`shared/xxx`），不是仓库根。需要仓库根路径时从
   `user.dir` 往上找 `settings.gradle.kts`。
4. **Voyager 用 `TabOptions.index` 恢复选中状态**。`HomeScreen` 现在是 6 个 Tab（小说在 index 0，
   其余顺延），加/删 Tab 必须同步改这 5 个文件里的 index，否则串台。
5. **文档编号会冲突**。main 已有 10（anime 计划）、11（交接）、12（小说计划）、13（对标）、14（Venera）；
   新文档从 15 开始，改号时要连带修代码注释里的 `docs/NN` 引用。
6. **判断"API 支持"不能看它是否被声明**。`SourcePreamble` 里有若干抛异常的桩
   （`Network.setCookies`、`Convert.decryptRsa`），把它们算作支持会得出假结论。
7. **Venera 的摘要函数返回原始字节，不是 hex**。源里写的是 `hexEncode(md5(...))`。
8. **CI 从首个提交起就是红的**（KSP 2.3.12 在 GitHub runner 上解析失败），本地能过。
   不要因为 CI 红就认为代码坏了，但也别在修好它之前指望它有门禁作用。

## 6. 下一步队列（按收益排序）

| 顺序 | 任务 | 说明 |
|---|---|---|
| 1 | `Network.fetchBytes` | hitomi 的唯一阻塞项，二进制响应 + `fetch().arrayBuffer()` |
| 2 | `Network.setCookies/getCookies/deleteCookies` | 6 个源引用，只影响登录态；做完静态全兼容会到 13/13 |
| 3 | 查 `jm.js` 的 `Not enough data available` | 已排除 `Convert`（同链路单测正常），错误来自别处，未定位 |
| 4 | 修 CI 的 KSP 解析 | 恢复门禁，成本最低收益最大 |
| 5 | 大库性能基准（1,000 部 / 10,000 章） | v1 成功标准之一，目前没有任何数字 |
| 6 | 打包 + tag（Android APK / 桌面 dmg·msi） | 另一个 v1 成功标准，当前 0 产物 |
| 7 | 全局搜索补小说**正文**、备份补设置与分类 | 见 [14](14-venera-compat.md) 与 README 已知限制 |

仍未验证的能力承诺（要在发布前解决）：真实 Venera 源**跨端**跑通（目前只验到构造 + 静态 API 面，
没有真站往返）、Windows 桌面的 MPV 与安装包、iOS（**21 个模块一个 `iosMain` 都没有**，
不是"缺壳"而是从零加目标）。

## 7. 不要做的事

- 不要 rebase `feature/novel`——它已经以 merge commit 并入，重写会毁掉历史。
  本地 `feat/novel-merge` 分支可以删（`git branch -d feat/novel-merge`）。
- 不要为了"看起来完整"给 `APP.platform` 编一个值：preamble 由 Android 与 JVM 共用，
  报错平台名会让源静默走错分支。
- 不要把 `UI.showMessage` 改回抛异常：headless 下抛错会让源在流程中途断掉。
- 不要在没有 `reference/` 的情况下断言 Venera 兼容性，`docs/14` 的数字是特定语料上测出来的。

## 8. 关键文件

- 源运行时宿主 API：`shared/source-runtime/src/commonMain/kotlin/app/miyomu/source/runtime/SourcePreamble.kt`
- Convert 宿主实现：`.../jvmShared/.../JvmConvertBridge.kt`
- 审计工具：`tools/venera_compat.py`
- 应用接线：`shared/ui/.../MiyomuApp.kt`、`MiyomuGraph.kt`、`home/HomeScreen.kt`
- 入口：`androidApp/.../MainActivity.kt`、`desktopApp/.../Main.kt`
- 文档索引：[docs/README.md](README.md)、agent 指引：[../AGENTS.md](../AGENTS.md)
