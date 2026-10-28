# Miyomu 生产构建

Phase 1 的正式多模块构建。工具链：AGP 9.4.1 + Kotlin 2.3.21 + Gradle 9.6 + compileSdk 37；JVM 桌面与 Android 共享 `:shared:*`。

> 此目录包含调试壳与本地 fixture 源，不是最终阅读器；fixture 不含任何第三方内容。

## 模块

| 模块 | 作用 |
|---|---|
| `:shared:model` | 领域模型、源描述、章节/页面 |
| `:shared:network` | Ktor 3.6、OkHttp/CIO、持久 Cookie |
| `:shared:source-api` | `ComicSource` / `ManagedComicSource` |
| `:shared:source-runtime` | 每源 QuickJS、Ktor、Ksoup DOM、KV、配额 |
| `:shared:data` | Room 2.8.5 + KSP；库/进度 schema 与 repository |
| `:app:desktop` | Compose Desktop 调试壳 |
| `:app:android` | Compose Android 调试壳 |

## 验证

```bash
./gradlew -p miyomu :shared:source-runtime:jvmTest
./gradlew -p miyomu :shared:source-runtime:assemble
./gradlew -p miyomu :shared:data:jvmTest
./gradlew -p miyomu :app:desktop:compileKotlin
./gradlew -p miyomu :app:android:assembleDebug
```

Room schema 导出在 `shared/data/schemas/`；修改实体后需同时更新 schema/迁移。

## 本地 demo

先启动自造 HTML/图片 fixture：

```bash
python3 miyomu/tools/fixture_server.py --port 18080
```

桌面：

```bash
./gradlew -p miyomu :app:desktop:run
```

搜索 `demo` → 打开结果 → 打开章节；应显示 3 页并加载首图。

Android：

```bash
./gradlew -p miyomu :app:android:assembleDebug
adb install -r miyomu/app/android/build/outputs/apk/debug/android-debug.apk
```

模拟器通过 `10.0.2.2:18080` 访问主机 fixture；真机需改 `MiyomuAndroid` 的 `FixtureBaseUrl` 或使用端口转发。

## 源脚本

`demo-resources/sources/demo_source.js` 是自写的 Venera 风格源，使用：

- `class ... extends ComicSource`（宿主构造并注册，不需要手动注册）
- `search.load` / `comic.loadInfo` / `comic.loadEp`
- `Network.get`、`HtmlDocument`、`this.loadData/saveData`

真实第三方源只用于本地验证，不提交；兼容范围见 [ADR-0008](../docs/adr/0008-venera-source-runtime.md)。

## 已知限制

- 真实 Venera 源尚未零修改验证。
- `Network.setCookies/getCookies/deleteCookies` 同步 JS API、`fetch`、search options、explore/categories/settings/login 未实现。
- 调试壳只显示首图与页面 URL；正式阅读器、库/进度、下载/同步、播放器轨道选择在后续里程碑。
