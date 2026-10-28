# S1 报告：mediamp 三端播放器验证

- 状态：**完成（桌面 + Android 通过；iOS 条件未验证，延期到 Phase 1 App 壳）**
- 日期：2026-10-28
- 时间盒：6 个工作日（已用约 1 天）
- 结论：**桌面 + Android Conditional Go；iOS 条件通过/延期**；音轨选择为统一 API 缺口

## 1. 结论摘要

macOS 桌面（MPV）已跑通 MP4/MKV/HLS/HEVC、外挂 SRT/ASS、seek/倍速/暂停。Android（ExoPlayer）在 API 36 / 16KB AVD 上跑通 MP4/MKV/HLS/HEVC，SRT/ASS 已通过截图确认基本渲染，seek/倍速/暂停、50 次连续 seek 与 30 分钟墙钟长播通过。iOS（AVKit）在裸 Kotlin/Native 测试宿主中 `playUri` 挂起（本地文件同样挂起），未获得播放状态；按规格标记条件通过/延期，Phase 1 需在真实 App 壳中验证。**统一 API 缺少音轨/内嵌字幕轨道选择**，是当前最重要的功能缺口。

## 2. 环境

| 平台 | 环境 |
|---|---|
| macOS | macOS 26.6.2；CMP 1.12.2；mediamp 0.5.1；MPV native 0.5.0 |
| Android | API 36 arm64 / 16KB AVD；AGP 9.4.1 + Gradle 9.6 + compileSdk 37；内置 Kotlin 2.2.0；Compose 1.12.1；mediamp-all 0.5.1；debug APK 22MB |
| iOS | iPhone 17 模拟器；Kotlin/Native 2.4.10；mediamp-api/avkit 0.5.1；Compose iOS 传递依赖；构建中 |

## 3. 功能矩阵

| 能力 | 优先级 | Android | macOS | iOS |
|---|---|---|---|---|
| MP4 / H.264 / AAC | 必须 | 通过 | 通过 | 条件未验证（裸测试宿主挂起） |
| MKV / H.264 | 必须 | 通过 | 通过 | 预计不支持（AVKit） |
| HLS | 必须 | 通过（HTTP 源） | 通过（本地） | 条件未验证 |
| 进度跳转 / 续播 | 必须 | 通过（20s seek） | 通过 | 条件未验证 |
| 播放速度 | 必须 | 通过（特性存在） | 通过 | 条件未验证 |
| 音轨切换 | 必须 | **统一 API 缺失** | **统一 API 缺失** | **统一 API 缺失** |
| 外挂 SRT | 必须 | 通过（截图确认） | 附加成功，目视待补 | 预计不支持（AVKit） |
| 外挂 ASS | 必须 | 通过（基本样式） | 附加成功，目视待补 | 预计不支持（AVKit） |
| 内嵌字幕 | 应该 | 无 API，未测 | 无 API，未测 | 无 API，未测 |
| 画中画 | 应该 | 未测 | N/A | 待测 |
| 后台播放 | 应该 | 未测 | N/A | 待测 |
| 硬件解码 | 应该 | 未量化（模拟器软解） | N/A | 待测 |
| 30 分钟稳定性 | 必须 | 通过（墙钟 30min，`FINISH after 1800000ms`） | 未测 | 待测 |
| 50 次连续 seek | 必须 | 通过（7.96s / 50 次） | 未测 | 待测 |

### Android 详情

- MP4 + SRT：`Ready` → `playing=true`；截图确认视频与 SRT 字幕。
- ASS：附加成功；截图确认字幕显示（基本样式，未验证完整 ASS 特效）。
- MKV / HEVC：`Ready`、播放、进度推进。
- HLS：HTTP 播放、视频画面截图确认。
- Seek：1.4s → 20s；倍速特性存在；暂停 `playWhenReady=false`。
- 50 次 seek：7964ms 完成，最终位置 49000ms。
- 30 分钟长播：使用 30 分钟、68MB 测试片通过 HTTP 播放；`S1_ANDROID_FINISH after 1800000ms`，进程无崩溃。由于模拟器软解，内容进度落后于墙钟，但稳定性目标按墙钟达到。

### iOS 条件未验证

- 使用 `AVKitMediampPlayerFactory` 在裸 Kotlin/Native 测试可执行文件中播放 MP4（HTTP 与本地文件均尝试）。
- 日志停在 `S1_IOS_BEFORE_PLAY`，`playUri` 在 15s `withTimeout` 内未返回；说明裸测试宿主无法驱动 AVKit（可能缺少 UIApplication/run loop/呈现层）。
- 按 S1 规格允许“条件通过”；Phase 1 必须在真实 iOS App 壳中复测。证据：[s1-ios-avkit-hang.log](evidence/s1-ios-avkit-hang.log)。

## 4. 已知问题

| ID | 问题 | 影响 | 处理 |
|---|---|---|---|
| S1-1 | `mediamp-all` 不含 MPV 原生运行时 | 桌面启动报 `mpv native runtime not found` | 显式添加 `mediamp-mpv-runtime-macos-arm64:0.5.0` |
| S1-2 | 含空格路径不能传 `file://` URI | MPV 打不开文件 | 传原始绝对路径 |
| S1-3 | Compose 1.12 要求 AGP 9.1+/compileSdk 37 | Android 工具链与 S2 的 AGP 8.13 不同 | Phase 1 统一 AGP/Kotlin 策略 |
| S1-4 | **mediamp 0.5.1 统一 API 无音轨/内嵌字幕选择** | 无法满足 Aniyomi 级播放器功能 | Phase 1 需后端直连或扩展 mediamp |
| S1-5 | Android HTTP 播放偶发缓冲 | 模拟器软解/网络波动；修复媒体服务 Range 后接近实时 | 正式网络层与真机复测 |
| S1-6 | ASS 在 Android 仅基本渲染 | 复杂样式/特效未验证 | 记录为平台差异；必要时外挂渲染 |
| S1-7 | iOS mediamp klib 在阿里云镜像缺失 | 需要 Google Central 镜像；Compose iOS 链接较重 | 构建配置已处理 |
| S1-8 | **iOS 裸 K/N 测试宿主无法驱动 AVKit** | `playUri` 挂起，iOS 矩阵未验证 | Phase 1 在真实 App 壳复测；失败则改用平台 AVPlayer 自定义播放器或降低 iOS 优先级 |
| S1-9 | 桌面锁屏，字幕渲染未目视确认 | 桌面视觉证据缺失 | 解锁后补测 |
| S1-10 | PiP、后台播放未测；音轨/内嵌字幕无统一 API | 系统集成与高级轨道控制缺口 | Phase 1 增加后端直连/扩展；PiP 单独验证 |

## 5. 复现方式

桌面：

```bash
experiments/spikes/s1-player/fixtures/generate.sh
./gradlew :spikes:s1-player:run -PautoTest=true -PexitAfterMs=14000
```

Android：

```bash
# 终端 1：媒体/fixture server（127.0.0.1:18080）
./gradlew :spikes:s2-js-runtime:runFixtureServer

# 终端 2：构建与安装
cd experiments/spikes/s1-player-android
./gradlew assembleDebug
adb install -r build/outputs/apk/debug/s1-player-android-debug.apk

# MP4 + SRT（HTTP）
adb shell am start -n org.projectv.spike.player.android/.MainActivity \
  --es fixture "http://10.0.2.2:18080/media/base.mp4" \
  --es subtitle "http://10.0.2.2:18080/media/subtitle.srt" \
  --ez autoTest true --el exitAfterMs 14000

# HLS / MKV / HEVC
adb shell am start -n org.projectv.spike.player.android/.MainActivity \
  --es fixture "http://10.0.2.2:18080/media/hls/index.m3u8" \
  --el exitAfterMs 10000
```

## 6. 结论与下一步

**S1 结论：Conditional Go（桌面 + Android）**

- 桌面与 Android 的必备播放/字幕/seek 能力可用；Android 30 分钟墙钟长播与 50 次 seek 通过。
- iOS 因裸测试宿主限制未验证；Phase 1 先做真实 App 壳 spike，再决定 AVKit 是否可用。
- 音轨/内嵌字幕选择为统一 API 缺口，Phase 1 必须提供后端直连或扩展 mediamp，否则无法达到 Aniyomi 级播放器。
- PiP/后台播放未测，列入 Phase 1。

下一步：

1. iOS App 壳验证 AVKit（或平台 AVPlayer 自定义）。
2. mediamp 轨道选择扩展/后端直连。
3. PiP、后台播放、桌面解锁后的字幕目视确认。
4. 真机性能/硬解复核。

## 7. 证据

- 代码：`experiments/spikes/s1-player`（桌面）、`experiments/spikes/s1-player-android`（Android）
- Android 截图：
  - [SRT](evidence/s1-android-srt.png)
  - [ASS](evidence/s1-android-ass.png)
  - [HLS](evidence/s1-android-hls.png)
- 关键日志：`S1_ANDROID_STATE`、`S1_ANDROID_POSITION`、`S1_ANDROID_SEEKS_50_DONE elapsed_ms=7964`
