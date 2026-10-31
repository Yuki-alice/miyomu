# ADR-0004：自适应单一 UI

- 状态：Accepted
- 日期：2026-10-31

## 背景

Kototoro 的桌面端长期是“Android 平板布局的移植”，iOS 另写 SwiftUI；同一功能存在多套页面、状态与交互，维护成本高，体验也不一致。

## 决策

1. Android、Desktop、iOS 共用同一套 Compose 页面与设计系统。
2. 布局由 **WindowSizeClass + 能力** 驱动，而不是 `if (isDesktop)`。
3. 断点：
   - Compact < 600dp：底部栏 + 单栏 + sheet
   - Medium 600–839dp：rail + 单栏/可选双栏
   - Expanded 840–1199dp：rail + 列表-详情
   - Large ≥ 1200dp：侧栏 + 内容 + 上下文栏
4. 使用 `material3-adaptive` 的 `ListDetailPaneScaffold` / `SupportingPaneScaffold`。
5. 平台只替换能力：播放器、WebView、文件选择、安全存储、通知、后台任务。

## 理由

- 一处修改，三端生效。
- 避免“移动端优先、桌面端补丁”的结构性债务。
- 断点/能力比平台判断更可测试。
- 桌面可以拥有真正适合鼠标/键盘的布局，而不是放大手机。

## 后果

### 正面

- UI 代码共享率 > 85%。
- 设计系统一致，回归测试集中。
- 新页面默认三端可用。

### 负面

- 某些平台特有交互需要 `expect/actual` 或能力接口。
- iOS 的导航/手势需要额外的平台适配。
- Compose 在 iOS 的性能需要持续基准。

## 约束

- 页面不得直接引用 `java.io.File`、`android.*`、`UIKit`；通过接口/回调。
- 页面不得读取平台类型判断；只读 `PlatformCapabilities`。
- 桌面与手机布局差异用约束/断点表达，不复制页面。
- 破坏性平台差异必须有明确的空态/说明。

## 备选方案

- **桌面单独一套 Compose**：短期快，长期三套；放弃。
- **iOS 原生 SwiftUI**：性能上限高，但三套 UI；作为性能不达标时的降级方案，不作为默认。
