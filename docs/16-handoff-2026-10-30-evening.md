# 16 交接（2026-10-30 晚）

> 给下一个接手的人/agent。上一份是 [15 号交接](15-handoff-2026-10-30-afternoon.md)，
> 本份覆盖：CI 门禁恢复、Venera 宿主 API 收尾、兼容性 13/13、历史测试竞态修复。
> 基线：本文件所在提交（`git log -1`）。

## 1. 一句话状态

CI 门禁已恢复并全绿；三种媒介同仓；**204 个 JVM 测试全绿**；Venera 参考源
**13/13 零改构造 + 13/13 静态 API 全兼容**，hitomi 已在桌面 JVM 跑通真站
search → details → pages。按用户 2026-10-30 的决定，**打包/发布延后**，
近期目标是完成度而非出产物。

## 2. 这一批提交做了什么

| 提交 | 内容 |
|---|---|
| `ab06669` | CI：runner 上使用规范 Maven 仓库（阿里云镜像仅本地），清缓存；补 download/danmaku 测试 |
| `d54233f` | `Network.fetchBytes`：二进制响应 + `ArrayBuffer`，hitomi 解除阻塞 |
| `a1c0f1b` | cookie API：`setCookies/getCookies/deleteCookies` 接持久 cookie jar，6 个登录态源转 OK |
| `dc65973` | 异步 rejection 兜底：包装源实例方法，修复 jm.js 构造失败 |
| `65f815e` | 回归：`await` 方仍能收到 rejection |
| `1c79480` | UI 测试竞态：删除前清空搜索过滤 |
| `32816b8` | 真站暴露的两个适配缺陷：DOM 上限淘汰最旧文档；单画廊源合成默认章节 |
| 文档 | `docs/14` 结果更新、README/AGENTS 状态同步、本文件 |

## 3. 验证基线

```bash
./gradlew :shared:ui:jvmTest :shared:novel:jvmTest :shared:reader-novel:jvmTest \
  :shared:source-novel:jvmTest :shared:player:jvmTest :shared:data:jvmTest \
  :shared:local:jvmTest :shared:source-runtime:jvmTest :shared:source-repo:jvmTest \
  :shared:source-manager:jvmTest :shared:reader-comic:jvmTest :shared:network:jvmTest \
  :shared:download:jvmTest :shared:danmaku:jvmTest :shared:domain:jvmTest \
  :desktopApp:compileKotlin :androidApp:assembleDebug
```

当前：**204 tests / 0 failures**。分布：`ui` 41、`novel` 30、`source-novel` 25、
`source-runtime` 22、`reader-novel` 16、`player` 12、`data` 10、`network` 10、
`source-repo` 9、`reader-comic` 8、`local` 7、`danmaku` 4、`download` 4、
`source-manager` 3、`domain` 3。

Venera 兼容性：

```bash
python3 tools/venera_compat.py reference/venera-yuan/*.js --summary
./gradlew :shared:source-runtime:jvmTest --tests '*VeneraSourceCompatTest*'
# 报告：build/reports/venera-compat.md
```

当前：**13/13 真实源零改构造成功**；静态审计 **13 个真实源全部 OK、0 缺口**
（`_template_` 另计）。`reference/` 仍是本地 gitignored 目录，换机器需自行克隆，
否则兼容测试会 skip。

## 4. 非显然的坑

1. **quickjs-kt 把未处理 Promise rejection 当致命错误**。源里的 fire-and-forget
   异步调用（jm 的 `refreshImgUrl`）会抛 `EOFException: Not enough data available`。
   `SourcePreamble` 的 `__installAsyncRejectionGuard` 给源实例方法返回的 Promise
   挂 no-op catch；不要删。`await` 方仍收到 rejection（有回归测试）。
2. **CI 的仓库顺序不能动**。`settings.gradle.kts` 在 `CI=true` 时只用
   Google / Maven Central / Plugin Portal；阿里云镜像仅本地启用。把镜像无条件放回前面
   会让 KSP 2.3.12 在 GitHub runner 上解析失败。
3. **Actions 缓存可能保存过失败结果**。这次恢复门禁时清过 `gh cache delete --all`；
   如果再次出现“制品在 Central 存在但 runner 解析不到”，先查镜像与缓存。
4. **UI 历史测试的竞态**：`MangaHistoryModelTest` 曾在搜索过滤为空的状态下等待
   “列表为空”，删除尚未落库也会提前满足。现在删除前先清空搜索并等待条目回来。
5. **二进制 body 不走 JSON**：`fetchBytes` 的 body 通过 token 在宿主暂存，
   状态/头走 JSON；改这条链路时保持边界，避免把二进制塞进字符串。
6. **源不 dispose `HtmlDocument`**：hitomi 每个搜索结果建一个文档且从不释放。
   `KsoupHtmlDomBridge` 在 16 个上限时淘汰最旧文档，而不是抛错；不要把它改回硬失败。
7. **单画廊源没有 `chapters`**：hitomi 只返回 `maxPage`/`thumbnails`。适配层合成
   `SingleChapterId` 的默认章节，取页时把它映射回 `epId=null`；改 `JsComicSource.pages`
   时保留这个映射。

## 5. 下一步队列（按完成度）

| 顺序 | 任务 | 说明 |
|---|---|---|
| 1 | 真实源桌面 + Android 真站往返 | hitomi 桌面 JVM 已跑通 search → details → pages（2026-10-30）；Android 端与 comick/manga_dex 仍待验（后两者本机网络/站点不可达），留 `evidence/` |
| 2 | 大库性能基准（1,000 部 / 10,000 章） | 启动、书架滚动、全局搜索、DB 查询；设预算并修热点 |
| 3 | 产品完整性 | 小说发现/分类页、`nextContentUrl`、在线正文搜索；播放器 MediaSession/PiP/锁屏；本地 7z/PDF；备份补设置与分类 |
| 4 | canary 自动化 / Windows job | 把手动 `-Pcanary` 变成定期任务；Windows MPV 与安装包验证 |
| 5 | iOS | 21 个模块仍无 `iosMain`，独立立项 |
| **延后** | 打包 + tag | 用户 2026-10-30 明确完成度不足，暂不打包发布 |

## 6. 不要做的事

- 不要直接向 Maven Central 批量抓元数据/枚举版本（会触发 403 限流）；本地 Gradle 缓存够用。
- 不要删除 `__installAsyncRejectionGuard` 或把它改成 rethrow；那会重新引入 jm 的构造失败。
- 不要在未跑静态审计 + `VeneraSourceCompatTest` 的情况下声称兼容性变化。
- 不要把打包/发布排进近期队列，除非用户重新开启。

## 7. 关键文件

- 宿主 API：`shared/source-runtime/src/commonMain/kotlin/app/miyomu/source/runtime/SourcePreamble.kt`
- 宿主绑定：`.../JsSourceRuntime.kt`
- cookie jar：`shared/network/src/commonMain/kotlin/app/miyomu/network/cookies/FileCookiesStorage.kt`
- 兼容性测试：`.../jvmTest/.../VeneraSourceCompatTest.kt`、`JsComicSourceTest.kt`
- 审计工具：`tools/venera_compat.py`
- CI：`.github/workflows/ci.yml`、`settings.gradle.kts`
