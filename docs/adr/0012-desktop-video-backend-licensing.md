# ADR-0012: 桌面视频后端的许可证边界

- 状态：**待决策**（2026-10-29）
- 关联：[ADR-0004](0004-licensing-and-clean-room.md)（Apache-2.0 与 GPL 清洁边界）· [ADR-0005](0005-mediamp-player.md)（mediamp 播放器）· [ADR-0011](0011-kmp-aniyomi-port.md)（Phase B）

## 背景

Phase B 引入 mediamp 作为跨平台播放器（ADR-0005）。在落地时发现：mediamp 的**代码**与**原生运行时**是两个许可证。

| Artifact | 内容 | 许可证 |
|---|---|---|
| `mediamp-api` | 播放器抽象 + Compose Surface | Apache-2.0 |
| `mediamp-exoplayer` | Android 后端（ExoPlayer / media3） | Apache-2.0 |
| `mediamp-mpv` | 桌面后端封装（JNI 胶水） | Apache-2.0 |
| `mediamp-mpv-runtime-macos-arm64` / `-macos-x64` / `-windows-x64` / `-windows-arm64` | 各平台原生 MPV/FFmpeg 二进制 | Apache-2.0 + **LGPL-2.1-or-later** |
| `mediamp-mpv-runtime-linux-x64` | Linux 原生二进制 | **GPL-3.0** |
| `mediamp-mpv-runtime`（聚合包） | 全平台二进制 | **GPL-3.0**（因包含 Linux 构建） |

也就是说：只要依赖聚合包 `mediamp-mpv-runtime`，整个桌面分发就被 GPL-3.0 覆盖，这与项目自 ADR-0004 起坚持的 Apache-2.0 直接冲突。

实测确认：未加原生运行时依赖时，桌面端在 `MediampPlayer(Unit)` 构造阶段即抛
`IllegalStateException: mpv native runtime not found: 'mpv-natives-macos-arm64.txt' is not on the classpath`。

## 当前处置（保守、可回退）

- `shared/player` **不打包**任何 MPV 原生运行时；`mediamp-mpv` 封装保留，桌面端因此无法播放。
- 新增 `isVideoPlaybackAvailable()`（expect/actual）：Android 恒为 `true`；桌面探测
  `mpv-natives-<os>-<arch>.txt` 资源是否存在。缺失时播放页显示说明而不是崩溃。
- Android 侧不受影响：ExoPlayer 为 Apache-2.0，视频功能完整。

## 待选方案

| 方案 | 做法 | 许可证影响 | 代价 |
|---|---|---|---|
| **A. 按平台分发运行时** | 只依赖 macOS/Windows 的 `mediamp-mpv-runtime-*`（LGPL-2.1+），Linux 不提供桌面视频 | 可维持 Apache-2.0；LGPL 要求动态链接且允许用户替换库文件 | Linux 桌面无视频；需在 NOTICE 登记 LGPL 组件并提供源码/替换途径 |
| **B. 用户自带 MPV** | 不打包二进制，启动时调用 `MpvMediampPlayer.prepareLibraries(系统 mpv 路径)` | 项目不分发 GPL/LGPL 二进制，最干净 | 用户需自行安装 mpv；三端体验不一致 |
| **C. 接受 GPL-3.0** | 依赖聚合包 `mediamp-mpv-runtime` | 桌面端整体转为 GPL-3.0（README/NOTICE 需同步改写） | 与现有 Apache-2.0 定位冲突，Android 与桌面许可证不一致 |
| **D. 桌面不做视频** | 视频仅 Android | 无影响 | 与"跨平台三合一"目标冲突 |

倾向：**A 或 B**。若目标是"开箱即用"，A 更合适；若目标是"许可证最干净"，B 更合适。

## 后果

- 在决策前，桌面端打开视频文件会看到明确的"未内置桌面播放器"说明页，不会崩溃。
- 无论选哪个方案，`NOTICE` 都需要登记 MPV/FFmpeg 组件及其许可证，并说明获取源码的途径。
