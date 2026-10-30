# S1 Player Spike

mediamp 桌面播放验证样例。旧规格/报告已随旧文档删除（git `14c495e`）。

## 已验证（2026-10-28，macOS / MPV）

- MP4/H.264、MKV、本地 HLS、HEVC 均进入 `Ready` 并播放。
- 外挂 SRT/ASS 可附加，播放无错误；字幕渲染未目视确认（桌面锁屏）。
- seek、1.5x 倍速、暂停通过自动测试日志验证。

## 运行

```bash
# 首次生成测试素材（本地文件已 gitignore）
experiments/spikes/s1-player/fixtures/generate.sh

# 默认：MP4 + SRT，12 秒后退出
./gradlew -p experiments :spikes:s1-player:run -Pfixture=fixtures/base.mp4 -PexitAfterMs=12000

# 其它容器
./gradlew -p experiments :spikes:s1-player:run -Pfixture=fixtures/base.mkv -PexitAfterMs=10000
./gradlew -p experiments :spikes:s1-player:run -Pfixture=fixtures/hls/index.m3u8 -PexitAfterMs=10000
./gradlew -p experiments :spikes:s1-player:run -Pfixture=fixtures/hevc.mp4 -PexitAfterMs=10000

# 自动验证 seek / 倍速 / 暂停
./gradlew -p experiments :spikes:s1-player:run -PautoTest=true -PexitAfterMs=14000

# ASS 字幕
./gradlew -p experiments :spikes:s1-player:run -Psubtitle=fixtures/subtitle.ass -PexitAfterMs=8000
```

## 已知坑

- `mediamp-all` 不包含 MPV 原生运行时，需额外加 `mediamp-mpv-runtime-macos-arm64:0.5.0`。
- 含空格路径不能传 `file://` URI（百分号编码），直接传绝对路径。
- 当前模块只有 JVM desktop；Android/iOS 待接入。

## 待办

- Android（ExoPlayer）与 iOS（AVKit）同一套测试。
- 30 分钟长播、50 次 seek、画中画、音轨/内嵌字幕。
- 解锁桌面后目视确认字幕与画面。
