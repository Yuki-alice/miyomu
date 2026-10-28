# ADR-0005：以 mediamp 作为播放器抽象

- 状态：条件接受（桌面/Android 通过；iOS 待 App 壳；轨道选择缺口）
- 日期：2026-10-28
- Phase 0 结果：桌面 MPV、Android ExoPlayer 播放/字幕/seek/50 seek/30min 墙钟通过；iOS 裸 K/N 测试宿主无法驱动 AVKit；统一 API 无音轨/内嵌字幕选择，Phase 1 必须补齐。

## 背景

CMP 没有 media_kit 的等价物。mediamp（open-ani，Apache-2.0）提供 CMP 统一播放器 API：

- Android：ExoPlayer（含 HLS）
- 桌面 JVM：MPV（Windows/macOS/Linux x86_64）
- iOS：AVKit
- Browser：HTMLVideoElement

它是 pre-1.0，minor 版本可能有破坏性变更；三后端能力并不等价。

## 决策

- 采用 mediamp 作为播放器抽象，但**包一层自有 `PlayerController` 接口**，业务代码不直接依赖 mediamp 类型。
- 锁定版本（Phase 0 用 0.5.1），升级前跑 S1 功能矩阵回归。
- 三端差异记录在 `PlayerCapabilities`，UI 按能力降级，不假设一致。
- 若 S1 失败：Android 暂保留 Aniyomi 的 mpv 播放器；桌面/iOS 另选后端，并重新评估 ADR。

## 后果

正面：

- 不需要为三端分别设计播放器 API。
- 复用 Animeko 的实战验证。
- Apache-2.0，许可兼容。

负面：

- pre-1.0 API 不稳定，升级有回归风险。
- ASS 字幕样式、画中画、后台播放、HDR 等能力三端不一致。
- Linux 仅 x86_64；ARM64 桌面暂不支持。
- 播放器 UI 仍需自建；mediamp 只解决内核。

## 验证

S1：HLS、MKV/H.265、外挂 SRT/ASS、续播、画中画、30 分钟稳定性。通过标准见 [S1 规格](../spikes/s1-player.md)。

## 备选方案

- **平台原生播放器 + 自有统一接口**：能力最强，工作量最大。
- **全平台 libmpv**：桌面强，移动端集成与商店合规成本高。
- **Flutter + media_kit**：换栈，违背 ADR-0001。
