# Spike S3：CMP + MD3E 屏幕迁移验证

> 时间盒：6 个工作日 · 依赖：CMP 1.12.2、Kotlin 2.4.21、JDK 21
> 代码目录：`experiments/spikes/s3-ui`（已创建，可运行）

## 1. 要回答的问题

1. Aniyomi 的 Compose 屏幕迁移到 CMP 桌面，实际成本是多少？
2. MD3 Expressive 组件在 CMP 桌面是否可用？不可用的部分有多少？
3. Aniyomi 依赖（Voyager/Injekt/Coil3/OkHttp/WebView/RichText）有哪些在 CMP 是死路？

## 2. 迁移对象

| 屏幕 | 验证点 | 备注 |
|---|---|---|
| 库（Library） | 网格、筛选、搜索入口、空状态 | 1,000 项性能 |
| 详情（Detail） | 封面、元数据、标签、章节列表、操作按钮 | 滚动与吸顶 |
| 设置（Settings） | 分组、开关、下拉、对话框 | 表单与状态 |

不迁移阅读器与播放器（视图层单独重写，见 S1 与架构文档）。

## 3. 依赖迁移审计

| 依赖 | 当前用途 | CMP 可行性 | 替代/结论 |
|---|---|---|---|
| Voyager | 导航 | 待验证 | 若不可用，评估 CMP Navigation 或自写栈 |
| Injekt | DI | 待验证 | 倾向 Koin/Kodein 或手工注入 |
| OkHttp | 网络 | JVM/Android 可用，iOS 不可 | Ktor（含 Darwin 引擎） |
| Coil 3 | 图片 | KMP 支持 | 直接评估 |
| Compose WebView | 内嵌网页 | 平台专属 | expect/actual 或移除 |
| RichText | Markdown | 待验证 | 简化或替换 |
| Material Icons | 图标 | 可用 | 检查包体积 |
| SQLDelight | 数据库 | KMP 支持 | Android 现有 2.0.2，升级评估 |
| AndroidX Material3 | 设计系统 | CMP 有 material3 | Expressive 组件逐项确认 |

## 4. MD3E 组件清单

逐项验证可用性与视觉一致性：

- Button 变体、Split Button、FAB Menu、Toolbar 变体
- Loading Indicator（形状动画）
- 形状/圆角规范、颜色角色（含 Dynamic Color：Android only）
- 排版（Typography）与字重
- 动效（容器变换、列表过渡）
- 深色模式与对比度

结论分为：可用 / 需自实现 / 平台不一致 / 不可用。不可用项若超过关键交互组件的一半，标记"MD3E 条件不成立"。

## 5. 性能与交互

- 1,000 项网格滚动：桌面帧率、内存；窗口缩放 800×600 → 1920×1080。
- 详情页 10,000 字简介 + 500 章节列表：滚动与布局耗时。
- 设置页：键盘导航、Tab 焦点、窗口失焦恢复。
- 冷启动到可交互时间（桌面）。
- 图片占位（本轮可用渐变/纯色，不接网络图片）。

## 6. 实施步骤

1. 在 `experiments/spikes/s3-ui` 搭三屏骨架（已完成初版）。
2. 对照 Aniyomi 设计语言补全组件与交互（不逐像素复刻）。
3. 填依赖审计表；对每个"待验证"给最小验证代码或结论。
4. 跑性能与窗口/输入测试。
5. 记录每屏迁移耗时（人时）、返工点、缺失组件。
6. 写报告与 Go / Conditional / Stop 建议。

## 7. 通过标准

- 三屏在 macOS 桌面可运行、可交互、视觉可接受。
- 无"死路依赖"：每个关键依赖都有明确替代或移除方案。
- 1,000 项网格滚动无明显卡顿；窗口缩放布局不崩。
- MD3E：核心交互组件可用，或退路（M3 标准组件）成本可接受。
- 迁移单屏 ≤ 1.5 人日（不含像素级打磨），否则评估放弃 fork、直接新建 CMP 壳。

## 8. 输出

`docs/spikes/results/s3-cmp-ui.md`：

- 三屏截图与运行方式
- 依赖审计表最终版
- MD3E 组件可用性表
- 每屏迁移耗时与阻塞点
- 结论：fork 迁移 vs 新建 CMP 壳

## 9. 明确不做

- 不接真实数据、不做网络图片、不做完整主题系统。
- 不迁移阅读器/播放器。
- 不做移动端适配（Android/iOS 留到 Phase 2）。
