# S2 Android Spike

在 Android 模拟器上验证 quickjs-kt 1.0.15 与同一套 Venera 风格源。旧规格/报告已随旧文档删除（git `14c495e`）。

## 已验证（2026-10-28，API 36 arm64 / 16KB AVD）

- JSON 源：搜索、详情、章节图片 URL、源级 KV 全部通过。
- HTML 源：Ksoup 0.2.6 驱动的 `HtmlDocument`，搜索/详情/章节图片、CSS 选择器、DOM 操作全部通过。
- Cookie：set/get/delete + `Cookie` 请求头往返通过。
- 文档释放后旧句柄被拒绝。
- 5 个 300ms 请求并发 973ms（低于串行 1500ms）；脚本异常可捕获；死循环 1.5s 取消。
- 冷启动 36ms（含 HTML/Ksoup）；100 次 QuickJS 实例创建/销毁 89ms；native heap 16MB。

## 构建与运行

```bash
# 终端 1：宿主 fixture server（监听 127.0.0.1:18080）
./gradlew -p experiments :spikes:s2-js-runtime:runFixtureServer

# 终端 2：模拟器
$HOME/Library/Android/sdk/emulator/emulator -avd han1me-16kb \
  -no-window -no-audio -no-boot-anim -gpu swiftshader_indirect -no-snapshot
adb wait-for-device

# 独立 Android 工程（AGP 8.13.2 + Kotlin 2.4.21 + Gradle 8.13）
cd experiments/spikes/s2-js-runtime-android
./gradlew installDebug
adb shell am start -n org.projectv.spike.js.android/.MainActivity
adb logcat -s S2_ANDROID
```

## 注意事项

- **AGP 版本冲突**：AGP 9.4.1 内置 Kotlin 2.2.0 读不了 quickjs-kt（Kotlin 2.4 元数据）。本 Spike 因此使用独立的 AGP 8.13.2 + Kotlin 2.4.21 + Gradle 8.13 工程；Phase 1 必须统一 AGP/Kotlin 版本策略。
- **网络地址**：该 AVD 上 `adb reverse` 不可用，App 使用 `http://10.0.2.2:18080` 访问宿主。
- **不在根构建中**：本目录是独立 Gradle 工程，根 `./gradlew build` 不会构建它。
- 运行前先启动 fixture server；否则搜索会报连接错误。
- 模拟器为 16KB page-size AVD；quickjs native 库加载正常（未观察到对齐问题）。
- Ksoup 会传递 `androidx.annotation`，Android 工程必须启用 `android.useAndroidX=true`。
- HTML/Cookie host 与桌面参考实现同源（`HtmlDomHost.kt`、`SimpleCookieJar.kt`），仅 HTTP 层用 HttpURLConnection。
