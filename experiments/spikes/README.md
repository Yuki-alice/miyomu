# Spikes

Phase 0 验证代码。**允许丢弃、允许粗糙、不进入正式架构。**

> 旧 Spike 规格、报告与截图已随旧文档删除（git `14c495e`）。这里是历史实验代码，不代表产品可用。当前状态见 [docs/00-current-status.md](../../docs/00-current-status.md)。

| 目录 | 内容 | 状态 |
|---|---|---|
| `s1-player/` | mediamp 桌面播放验证 | 桌面部分通过；Android/iOS 未开始 |
| `s2-js-runtime/` | QuickJS 源运行时（JVM + fixture） | 三端 JSON + HTML/Cookie 通过 |
| `s2-js-runtime-android/` | S2 Android 独立工程 | JSON + HTML/Cookie 通过（API 36 / 16KB AVD） |
| `s2-js-runtime-ios/` | S2 iOS 独立工程 | JSON + HTML/Cookie 通过（iPhone 17 模拟器） |
| `s3-ui/` | Compose Desktop 三屏样例 | headless 截图 + 1,000 项滚动通过 |

## 规则

- 每个 Spike 一个独立模块/目录，互不依赖业务代码。
- 不复制 GPL 代码；测试素材自生成或使用 CC 授权内容。
- 时间盒到期即写报告；旧模板已随旧文档删除。
- 通过与否以当时的规格为标准，不以“感觉”判断。
- Phase 0 结束后，spike 代码可删除或归档，结论保留在 git 历史。
