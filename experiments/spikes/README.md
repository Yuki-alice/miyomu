# Spikes

Phase 0 验证代码。**允许丢弃、允许粗糙、不进入正式架构。**

| 目录 | 内容 | 状态 | 规格 |
|---|---|---|---|
| `s1-player/` | mediamp 桌面播放验证 | 桌面部分通过；Android/iOS 未开始 | [S1](../../docs/spikes/s1-player.md) |
| `s2-js-runtime/` | QuickJS 源运行时（JVM + fixture） | 三端 JSON + HTML/Cookie 通过 | [S2](../../docs/spikes/s2-js-runtime.md) |
| `s2-js-runtime-android/` | S2 Android 独立工程 | JSON + HTML/Cookie 通过（API 36 / 16KB AVD） | [S2](../../docs/spikes/s2-js-runtime.md) |
| `s2-js-runtime-ios/` | S2 iOS 独立工程 | JSON + HTML/Cookie 通过（iPhone 17 模拟器） | [S2](../../docs/spikes/s2-js-runtime.md) |
| `s3-ui/` | Compose Desktop 三屏样例 | headless 截图 + 1,000 项滚动通过 | [S3](../../docs/spikes/s3-cmp-ui.md) |

## 规则

- 每个 Spike 一个独立模块/目录，互不依赖业务代码。
- 不复制 GPL 代码；测试素材自生成或使用 CC 授权内容。
- 时间盒到期即写报告（模板见 `docs/spikes/results/TEMPLATE.md`），不无限打磨。
- 通过与否以规格中的标准为准，不以"感觉"判断。
- Phase 0 结束后，spike 代码可删除或归档，结论保留在文档中。
