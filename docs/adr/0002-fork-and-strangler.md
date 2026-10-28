# ADR-0002：Fork Aniyomi，按模块翻修（Strangler）

- 状态：提议（Phase 1 迁移验证）
- 日期：2026-10-28
- Phase 0 结果：S3 证明 CMP/MD3E 与三屏可迁移；S1 证明 Aniyomi 式 UI/阅读器逻辑可承接，但尚未做真实屏幕移植计时。Phase 1 继续 fork+strangler。

## 背景

Aniyomi 是 Apache-2.0 的成熟 Android 漫画 + 动画应用：

- `presentation` 层已有大量 Compose + Voyager 屏幕；
- 漫画阅读器的 `ReaderViewModel`、`ChapterLoader`、HTTP/压缩包/EPUB/下载 loader 是经过长期验证的逻辑；
- 数据层使用 SQLDelight 2，网络 OkHttp 5，图片 Coil 3，迁移 KMP 的阻力低于预期；
- 上游近乎停滞（近一年 21 次提交），fork 后几乎没有合并税；
- 但核心阅读器视图（`ReaderActivity`/`Viewer`/`Pager`/`WebtoonRecyclerView`/`SubsamplingScaleImageView`）是 View 体系，不能直接进入 CMP。

目标形态是 KMP 共享核心 + CMP UI。全量重写会丢失 Aniyomi 的 UI 资产与多年逻辑，且 12–18 个月不可发布；直接新建则失去持续交付能力。

## 决策

**Fork Aniyomi 作为 Android 壳，按模块进行 Strangler 迁移，不做大爆炸重写。**

- 保留 Android 版持续发版。
- 新增 `:shared` KMP 模块，先下沉模型、数据、网络、源运行时。
- Compose 屏幕逐个迁入 CMP；阅读器/播放器最后替换。
- JS 源运行时以 feature flag 形式先进入 Android 版。
- APK 扩展最多作为 Android 过渡兼容层，必须设定移除日期。

## 退出标准（何时放弃 fork）

满足任一条件即重新评估，转向"新建 CMP 壳"：

- S3 显示单屏迁移 > 1.5 人日，或存在无法绕过的依赖死路；
- 连续两个屏幕迁移超预估 2 倍；
- 双 UI 维护导致 Android 发版节奏中断超过 2 个迭代；
- CMP 在桌面/iOS 的核心交互无法达到可接受质量。

## 后果

正面：

- 保留 Android 用户体验与大量领域逻辑。
- 可增量发布、可对照旧实现、可随时回退。
- 上游停滞，合并成本低。

负面：

- 迁移期同时存在 View 与 CMP 两套 UI。
- Aniyomi 的 Android 绑定（Activity、Service、Injekt、WebView）需要平台抽象。
- 旧代码中的 workaround 与历史包袱会被继承；需要边界与清理计划。
- 品牌、包名、图标必须更换；遵守 Apache NOTICE。

## 备选方案

- **新建 CMP 仓库**：更干净，但失去持续交付与 UI/逻辑复用；在 S3 失败时采用。
- **Fork Venera-Next**：跨平台与 JS 已具备，但 GPL-3.0、仅漫画、单人上游，不符合三媒与许可目标。
