# Spike S1：mediamp 三端播放器验证

> 时间盒：6 个工作日 · 依赖：Android SDK 37、Xcode 26.6、macOS
> 代码目录：`experiments/spikes/s1-player`（待创建）

## 1. 要回答的问题

1. mediamp 的统一 API 是否覆盖视频阅读的必备能力？
2. ExoPlayer / MPV / AVKit 三后端的关键能力差异有多大？
3. 外部字幕（SRT/ASS）、HLS、续播、画中画在哪些平台可用？
4. pre-1.0 的 API 稳定性是否可接受（锁定 0.5.1 后能否稳定构建）？

## 2. 平台与后端

| 平台 | 后端 | 设备 |
|---|---|---|
| Android | ExoPlayer (media3) | AVD `han1me-16kb` 或真机 |
| macOS | MPV | 本机 |
| iOS | AVKit | iPhone 17 模拟器 |

Windows/Linux 不在本 Spike 验证（同一 MPV 后端，留到 Phase 1 冒烟）。

## 3. 功能矩阵（必测）

| 能力 | 优先级 | Android | macOS | iOS | 备注 |
|---|---|---|---|---|---|
| MP4 / H.264 / AAC | 必须 | | | | 基准 |
| MKV / H.265 | 必须 | | | | 桌面尤其重要 |
| HLS (.m3u8) | 必须 | | | | 在线源常见 |
| 进度跳转 / 续播 | 必须 | | | | 记录精度 |
| 播放速度 0.5–2.0 | 必须 | | | | |
| 音轨切换 | 必须 | | | | |
| 外挂 SRT | 必须 | | | | |
| 外挂 ASS（样式） | 必须 | | | | 样式差异记录 |
| 内嵌字幕 | 应该 | | | | |
| 画中画 | 应该 | | | | 桌面记为 N/A |
| 后台播放 | 应该 | | | | 移动端 |
| 硬件解码 | 应该 | | | | 记录功耗/发热 |
| HDR | 可选 | | | | 无设备则标记未测 |
| 4K | 可选 | | | | 桌面 |

判定：必须项全过；整体矩阵通过率 ≥ 80%；无阻塞性 API 缺陷。

## 4. 测试素材

用 ffmpeg 自生成，避免版权问题：

```bash
# 10 分钟测试源：彩条 + 正弦音
ffmpeg -f lavfi -i testsrc2=size=1920x1080:rate=30 -f lavfi -i sine=frequency=440 \
  -t 600 -c:v libx264 -preset veryfast -c:a aac -b:a 128k fixtures/base.mp4

# HLS
ffmpeg -i fixtures/base.mp4 -c copy -f hls -hls_time 6 -hls_playlist_type vod fixtures/hls/index.m3u8

# 外挂字幕
# 自写 fixtures/subtitle.srt 与 fixtures/subtitle.ass
```

如果 ffmpeg 不可用，使用 CC-BY 的 Big Buck Bunny 测试片段，并在报告中注明来源与许可证。

## 5. 实施步骤

1. 创建 KMP 模块，接入 `mediamp-all:0.5.1`（或按官方安装文档拆分后端）。
2. 每端一个最小播放页：文件选择/固定 asset + 播放/暂停/进度/字幕开关。
3. 先跑 Android + macOS 基准，再补 iOS。
4. 逐项填写功能矩阵；对失败项记录日志与复现步骤。
5. 测试长播 30 分钟、频繁 seek 50 次、切前后台 10 次，记录崩溃/ANR/音画不同步。
6. 记录构建耗时、包体增量、mediamp 依赖树。
7. 写报告并给出 Go / Conditional / Stop 建议。

## 6. 通过标准

- 必须项全部通过；矩阵 ≥ 80%。
- 三端均能稳定播放 30 分钟无崩溃。
- 外挂 SRT 在至少 Android + 桌面可用；ASS 样式差异有明确记录。
- 续播精度满足 ±2 秒（移动端）/ ±1 秒（桌面）。
- mediamp 0.5.1 在本项目依赖组合下可重复构建。
- 若 iOS 因环境或 API 失败，允许标记"条件通过"，但必须在 Phase 1 前给出替代方案。

## 7. 输出

`docs/spikes/results/s1-player.md`：

- 实际版本与依赖树
- 完整功能矩阵（含证据链接/截图路径）
- 三端差异与宿主需要抽象的能力
- 与 Aniyomi 播放器的功能差距清单
- 风险与建议（Go / 条件通过 / 换后端）

## 8. 明确不做

- 不做完整播放器 UI、手势、投屏、BT、弹幕。
- 不做视频源解析。
- 不评估商业授权与编解码专利，只记录许可证事实（mediamp Apache-2.0、MPV/libmpv 构建选项、AVKit/ExoPlayer 平台条款）。
