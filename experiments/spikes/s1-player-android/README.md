# S1 Android Player Spike

mediamp + ExoPlayer 的 Android 播放验证。旧规格/报告已随旧文档删除（git `14c495e`）。

## 已验证（2026-10-28，API 36 / 16KB AVD）

- MP4/MKV/HLS/HEVC：`Ready` 并播放；HLS 通过 HTTP 源验证。
- 外挂 SRT/ASS：附加成功，截图确认字幕显示（ASS 基本样式）。
- seek 20s、1.5x 倍速、暂停通过；50 次连续 seek 7.96s。
- 30 分钟长播进行中。

## 构建

```bash
# 需要 Gradle 9.6（AGP 9.4.1 最低要求）
./gradlew assembleDebug
```

工具链：AGP 9.4.1 + 内置 Kotlin 2.2.0 + Compose compiler 2.2.0 + compileSdk 37 + AndroidX Compose 1.12.1 + mediamp-all 0.5.1。

## 运行

```bash
# 终端 1：媒体/fixture server（项目根目录）
./gradlew -p experiments :spikes:s2-js-runtime:runFixtureServer

# 安装
cd experiments/spikes/s1-player-android
adb install -r build/outputs/apk/debug/s1-player-android-debug.apk

# MP4 + SRT
adb shell am start -n org.projectv.spike.player.android/.MainActivity \
  --es fixture "http://10.0.2.2:18080/media/base.mp4" \
  --es subtitle "http://10.0.2.2:18080/media/subtitle.srt" \
  --ez autoTest true --el exitAfterMs 14000

# HLS
adb shell am start -n org.projectv.spike.player.android/.MainActivity \
  --es fixture "http://10.0.2.2:18080/media/hls/index.m3u8" \
  --el exitAfterMs 10000

# 50 次 seek
adb shell am start -n org.projectv.spike.player.android/.MainActivity \
  --es fixture "http://10.0.2.2:18080/media/base.mp4" \
  --ez stressSeeks true --el exitAfterMs 20000

# 日志
adb logcat -s S1_ANDROID
```

## 注意

- 模拟器无 `/sdcard/Download`，测试统一走 HTTP 媒体（`10.0.2.2:18080/media/...`）。
- 媒体服务支持 Range，用于 seek；无 Range 时 MP4 会反复整文件传输，导致卡顿。
- 音轨/内嵌字幕切换不在 mediamp 统一 API 内，需后端直连或扩展。
- 截图证据已随旧文档删除（git `14c495e`）。
