# S2 报告：QuickJS 源运行时验证

- 状态：**三端 JSON + 三端 HTML/Cookie 通过；剩余 Cookie 持久化与正式网络层**
- 日期：2026-10-28
- 时间盒：8 个工作日（已用约 3 天）
- 结论：**三端 JSON + HTML/Cookie Conditional Go**；S2 读路径与 Venera 兼容骨架验证完成，剩余工程化项

## 1. 结论摘要

在 JVM 桌面、Android（API 36 arm64，16KB AVD）与 iOS 模拟器（iPhone 17，iosSimulatorArm64）上，quickjs-kt 1.0.15 都成功运行了自写的 Venera 风格 JSON 漫画源：搜索、详情、章节图片 URL、源级 KV 全部通过；并发、异常、死循环取消均正常。桌面、Android 与 iOS 均用 Ksoup 实现了 `HtmlDocument`/元素句柄 DOM 与基础 Cookie jar，HTML 源读路径、DOM 查询、Cookie 写入/读取/删除与请求头往返全部通过。三端读路径与 Venera 兼容骨架已验证完成。

## 2. 环境

| 项目 | 实际值 |
|---|---|
| 机器 | macOS 26.6.2（aarch64） |
| JDK | Corretto 21.0.12 |
| quickjs-kt | 1.0.15（JVM 变体） |
| kotlinx-coroutines | 1.10.2 |
| 传输 | JDK HttpClient + 本地 `com.sun.net.httpserver` fixture |
| Android | 模拟器 API 36 / arm64 / 16KB page size；独立工程 AGP 8.13.2 + Kotlin 2.4.21 + Gradle 8.13；Ksoup 0.2.6；`android.useAndroidX=true` |
| iOS | iPhone 17 模拟器 / iosSimulatorArm64；独立工程 Kotlin/Native 2.4.10 + Gradle 8.13；Ksoup 0.2.6 |
| HTML 解析 | Ksoup 0.2.6（MIT，KMP，API 对齐 jsoup），桌面 JVM 验证 |

## 3. 结果

| 检查 | 结果 | 数据/证据 |
|---|---|---|
| 运行时初始化 + 源加载 | 通过 | 冷启动 486ms（含类加载/首次 native 初始化） |
| 搜索（JSON 源） | 通过 | 返回 `Demo Comic`、`maxPage=1` |
| 详情 | 通过 | 返回章节 `Chapter 1` |
| 章节图片 URL | 通过 | 返回 `http://127.0.0.1:.../images/1.jpg` |
| 源级 KV | 通过 | 写入 `abc` 后读回 `abc` |
| 5 并发请求（各 300ms） | 通过 | 总耗时 309ms（并发生效） |
| 脚本异常 | 通过 | 捕获 `QuickJsException` |
| 死循环中断 | 通过 | `withTimeout(1500)` → 1507ms 取消，类型 `TimeoutCancellationException` |
| 100 次 QuickJS 实例创建/销毁 | 通过 | 24ms（桌面 JVM） |
| 进程内存（粗略） | 12MB | 总-空闲堆，非单实例精确值 |
| 登录 / 设置 / UI | 未实现 | 首版只做读路径 |

### 桌面 HTML / Cookie（JVM + Ksoup）

| 检查 | 结果 | 数据/证据 |
|---|---|---|
| HTML 搜索 | 通过 | `a.comic` + `.title` + `.author`，返回 2 条 |
| HTML 详情 | 通过 | `h1.title`、`.author`、`.chapters a`，返回 2 章 |
| HTML 章节图片 | 通过 | `img[data-src]` → 2 个绝对 URL |
| DOM 操作 | 通过 | classNames / text / attributes / localName / children / parent 全部正确 |
| 文档释放 | 通过 | dispose 后访问抛错，宿主拒绝旧句柄 |
| Cookie set/get/delete | 通过 | 设置 `sid=abc123` → 请求头收到 → 列表 1 条 → 删除后为空 |
| 文档上限 | 未测 | 宿主设 16 个文档上限，未做压力测试 |

### Android（API 36 arm64 / 16KB AVD）

| 检查 | 结果 | 数据/证据 |
|---|---|---|
| 冷启动 + 源加载 | 通过 | 36ms（包含 HTML 源与 Ksoup 初始化） |
| JSON 搜索 / 详情 / 章节 / KV | 通过 | 与桌面相同的断言 |
| HTML 搜索 / 详情 / 章节 | 通过 | Ksoup + `HtmlDocument`，返回与桌面一致 |
| HTML DOM 操作 | 通过 | classNames / text / attributes / children / parent |
| Cookie set/get/delete + 请求头 | 通过 | `sid=abc123` 往返与删除 |
| 文档释放 | 通过 | dispose 后旧句柄被拒绝 |
| 5 并发请求（各 300ms） | 通过 | 973ms（低于串行 1500ms；见 S2-18） |
| 脚本异常 | 通过 | `QuickJsException` |
| 死循环取消 | 通过 | `withTimeout(1500)` → 约 1.5s |
| 100 次实例创建/销毁 | 通过 | 89ms |
| Native heap | 16MB | `Debug.getNativeHeapAllocatedSize()` |
| 16KB page-size 加载 | 通过 | 未观察到 native 对齐问题 |

### iOS（iPhone 17 模拟器 / iosSimulatorArm64）

| 检查 | 结果 | 数据/证据 |
|---|---|---|
| 冷启动 + 源加载 | 通过 | 12ms（包含 HTML 源与 Ksoup 初始化） |
| JSON 搜索 / 详情 / 章节 / KV | 通过 | 与桌面相同的断言 |
| HTML 搜索 / 详情 / 章节 | 通过 | Ksoup + `HtmlDocument` |
| HTML DOM 操作 | 通过 | classNames / text / attributes / children / parent |
| Cookie set/get/delete + 请求头 | 通过 | `sid=abc123` 往返与删除 |
| 文档释放 | 通过 | dispose 后旧句柄被拒绝 |
| 5 并发请求（各 300ms） | 通过 | 401ms |
| 脚本异常 | 通过 | `QuickJsException` |
| 死循环取消 | 通过 | 约 1.5s，类型 `QuickJsInterruptedException` |
| 100 次实例创建/销毁 | 通过 | 336ms |
| 测试汇总 | 通过 | 1 test / 0 failures / 0 errors |

## 4. 实现范围（v0）

已实现宿主 API：

- `__hostHttpGet(url, headersJson)`：异步 HTTP，2xx 返回 body，非 2xx 抛错；**headers 暂未生效**。
- `__hostKvGet` / `__hostKvSet`：进程内源级 KV（未持久化）。
- `__hostLog`：日志。
- JS 侧最小 shim：`ComicSource` 注册、`Comic`/`ComicDetails`、`Network.get`、`__invoke(action, payload)`。
- 测试源：`fixtures/demo_source.js`（自写，非 Venera 代码）。

未实现：HTML DOM、Cookie、设置/翻译、图片字节、评论/收藏、源仓库/签名、多源隔离、内存限额。

## 5. 关键发现

| ID | 发现 | 影响 |
|---|---|---|
| S2-1 | `evaluate<Unit>` 在脚本最后表达式返回对象时失败（JsObject → Unit） | 加载脚本用 `evaluate<Any?>` 或显式 `undefined` |
| S2-2 | 异步 action 必须用顶层 `await`，否则拿到的是 Promise 对象 | 宿主调用约定要固定为 `await __invoke(...)` |
| S2-3 | 基类构造函数执行时子类字段尚未初始化，无法在 `super()` 中读 `key` | 注册必须在实例化之后显式完成 |
| S2-4 | fixture server 使用非守护线程会阻止 JVM 退出，Gradle 多等约 60s | 使用 daemon 线程 + `shutdownNow()` |
| S2-5 | 桌面冷启动 ~486ms、100 次实例创建 24ms | 移动端需重新测量，不能外推 |
| S2-6 | 并发 host 调用可用，但未做每源隔离/配额 | 正式实现需每源实例 + 内存/超时/存储限额 |
| S2-7 | **AGP 9.4.1 内置 Kotlin 2.2.0 读不了 quickjs-kt 的 Kotlin 2.4 元数据** | Phase 0 时 Android 验证改用独立 AGP 8.13.2 + KGP 2.4.21；**Phase 1 M0 已解决**：AGP 9.4.1 + KGP 2.3.21 单轨可读取（ADR-0006） |
| S2-8 | 该 AVD 上 `adb reverse` 不可用 | 改用模拟器宿主别名 `10.0.2.2:18080` |
| S2-9 | 16KB AVD 上 quickjs native 正常加载 | 暂未发现 16KB 对齐风险，需真机复核 |
| S2-10 | K/N `NSURLSession.dataTaskWithRequest` block 重载不可用 | iOS Spike 用 POSIX socket；Phase 1 应使用 Ktor Darwin/完整 NSURLSession 封装并处理 ATS |
| S2-11 | iOS 超时表现为 `QuickJsInterruptedException` 而非 `TimeoutCancellationException` | 宿主需同时处理两类取消异常 |
| S2-12 | iOS 模拟器可直接访问宿主 `127.0.0.1` | 与 Android 的 `10.0.2.2` 不同；平台测试脚本需区分 |
| S2-13 | 测试输出在 XML `<system-out>` 中，Gradle 控制台不显示 | 结果提取需读 `build/test-results/.../*.xml` |
| S2-14 | HTML DOM 用宿主句柄 + Ksoup，三端均一次通过 | 平台差异主要在 HTTP/Cookie 集成，DOM 语义可共享 |
| S2-15 | Cookie 基础往返可用，但仍是内存实现、无持久化/过期/安全属性 | Phase 1 需持久化 jar、过期与 secure/HttpOnly 策略 |
| S2-16 | 共享 preamble 的 HtmlDocument/Cookie 类已在三端安装对应 host 函数 | 三端 HTML/Cookie 读路径均可宣称支持 |
| S2-17 | Ksoup Android 传递 `androidx.annotation` | Android 工程需启用 `android.useAndroidX=true`，否则 AAR 校验失败 |
| S2-18 | Android 5 并发 973ms，明显慢于桌面/iOS 的 ~317–401ms | HttpURLConnection/模拟器连接池限制；正式网络层用 Ktor/OkHttp 复核并发 |
| S2-19 | iOS 引入 Ksoup 后链接/测试构建约 1m51s，100 次实例 336ms | Phase 1 需测 App 启动与包体；按需初始化/缓存 |
| S2-20 | iOS Cookie jar 用 common 手写 URL 解析，未依赖 `java.net.URI` | 仅覆盖 http(s) 主机/路径；复杂端口、IPv6、IDN 需正式实现 |

## 6. 许可与合规

- shim 与测试源为自写实现，仅参考 Venera 文档的接口形状；未复制 `assets/init.js`、运行时或示例源。
- fixture 内容自造，不涉及第三方内容。

## 7. 下一步

1. Cookie 持久化、过期、secure/HttpOnly 策略；请求头透传。
2. 正式网络层：Ktor（Darwin/OkHttp/CIO 引擎）替代 POSIX/HttpURLConnection 测试实现，并复核 Android 并发。
3. 多源注册与卸载、源仓库索引；HTML 文档数量/句柄上限压力测试。
4. 用真实 Venera 兼容源做只读对照（本地测试，不提交）。
5. Phase 1 前解决 AGP 9 内置 Kotlin 2.2 与 Kotlin 2.4 依赖的版本冲突；iOS 正式 App 集成与启动/包体测量。

## 8. 复现

桌面：

```bash
./gradlew :spikes:s2-js-runtime:run
```

Android：见 `experiments/spikes/s2-js-runtime-android/README.md`（先启动 `runFixtureServer`，再安装运行独立 Android 工程）。

iOS：见 `experiments/spikes/s2-js-runtime-ios/README.md`（先启动 `runFixtureServer`，启动 iPhone 17 模拟器，再执行 `iosSimulatorArm64Test`）。

输出行以 `S2_` / `S2_ANDROID_` / `S2_IOS_` 开头；JS shim 在 `experiments/spikes/s2-js-runtime/src/main/resources/runtime/`。
