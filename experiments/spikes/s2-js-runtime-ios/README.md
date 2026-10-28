# S2 iOS Spike

在 iPhone 17 模拟器上验证 quickjs-kt 1.0.15 与同一套 Venera 风格源。规格见 [S2](../../../docs/spikes/s2-js-runtime.md)，报告见 [结果](../../../docs/spikes/results/s2-js-runtime.md)。

## 已验证（2026-10-28，iPhone 17 模拟器 / iosSimulatorArm64）

- JSON 源：搜索、详情、章节图片 URL、源级 KV 全部通过。
- HTML 源：Ksoup 0.2.6 驱动的 `HtmlDocument`，搜索/详情/章节图片、CSS 选择器、DOM 操作全部通过。
- Cookie：set/get/delete + `Cookie` 请求头往返通过。
- 文档释放后旧句柄被拒绝。
- 5 个 300ms 请求并发 401ms；脚本异常可捕获；死循环 1.5s 取消。
- 冷启动 12ms（含 HTML/Ksoup）；100 次 QuickJS 实例创建/销毁 336ms。
- 测试结果：1 passed / 0 failed（构建+链接约 1m51s）。

## 运行

```bash
# 终端 1：宿主 fixture server（127.0.0.1:18080）
./gradlew -p experiments :spikes:s2-js-runtime:runFixtureServer

# 终端 2：启动模拟器
xcrun simctl boot "iPhone 17" || true
xcrun simctl bootstatus "iPhone 17" -b

# 运行 KMP 测试（独立 Gradle 8.13 工程）
cd experiments/spikes/s2-js-runtime-ios
./gradlew iosSimulatorArm64Test

# 查看输出
grep -o 'S2_IOS_[^<]*' build/test-results/iosSimulatorArm64Test/*.xml | sort -u
```

## 注意事项

- **独立工程**：Kotlin/Native 2.4.10 + Gradle 8.13，使用本机已安装的 K/N prebuilt；不在根构建中。
- **HTTP 实现**：K/N 的 `NSURLSession.dataTaskWithRequest` block 重载不可用，本 Spike 用 POSIX socket 实现 `http://IP:port` 请求；Phase 1 应改用 Ktor Darwin 或完整 NSURLSession 封装，并处理 ATS 例外。
- **HTML/Cookie**：Ksoup KMP 提供 `HtmlDocument`；Cookie jar 在 commonMain 手写 URL 解析（不依赖 `java.net.URI`），HTTP 层注入 `Cookie` 与解析 `Set-Cookie`。
- **网络地址**：iOS 模拟器与宿主共享 localhost，直接用 `127.0.0.1:18080`。
- **超时类型**：iOS 上取消表现为 `QuickJsInterruptedException`（JVM/Android 为 `TimeoutCancellationException`），宿主需两者都处理。
- 运行前先启动 fixture server；测试输出主要在 XML 的 `<system-out>` 中。
