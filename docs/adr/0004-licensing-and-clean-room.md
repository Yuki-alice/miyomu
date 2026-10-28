# ADR-0004：Apache-2.0 与 GPL 清洁边界

- 状态：接受（Apache-2.0，2026-10-28）
- 日期：2026-10-28
- Phase 0 结果：许可证确定为 Apache-2.0；未引入 GPL 代码；Ksoup MIT、mediamp Apache-2.0、QuickJS MIT 已审计。LICENSE 文件在 Phase 1 M0 创建（版权人待定名）。

## 背景

- Aniyomi：Apache-2.0，可复用代码（保留 NOTICE/版权声明），允许闭源衍生。
- Venera / Venera-Next：GPL-3.0，复制代码会要求整个衍生作品以 GPL-3.0 分发，并与 iOS App Store 条款存在冲突。
- 项目可能希望保留闭源/商业化选项，也可能最终选择开源；当前未定。
- 第三方媒体组件许可证各异：mediamp Apache-2.0、QuickJS MIT、Room/Ktor/Coil Apache-2.0、libmpv（可按 LGPL 构建）、MuPDF AGPL、RAR 解压授权受限。

## 决策

- 项目代码采用 **Apache-2.0**（最终确认前不创建 LICENSE 文件）。
- **不复制任何 GPL 代码**。Venera 兼容只做行为/协议级兼容：
  - 只依据公开文档与黑盒行为实现；
  - 不复制 `assets/init.js`、运行时源码、示例源；
  - 兼容层代码独立编写，保留设计记录。
- 依赖审计纳入 CI：许可证白名单（Apache/MIT/BSD/ISC 等），GPL/AGPL 需人工批准。
- 桌面播放器构建使用 LGPL 配置的 libmpv；PDF 优先 pdfium（BSD），避免 MuPDF（AGPL）；RAR 暂不支持。
- 品牌、包名、图标、应用名与 Aniyomi/Venera 明确区分，不使用其商标。

## 后果

正面：

- 保持许可自由度，保留闭源/上架的可能性。
- 避免 GPL 污染与 App Store 冲突。
- 依赖风险可控。

负面：

- Venera 兼容层工作量高于直接移植。
- 不能直接借用其成熟实现，需自行踩坑。
- 若最终想用 GPL 代码，需要重新许可整个项目，代价高。

## 待确认

- 项目最终许可证（Apache-2.0 / GPL-3.0 / 其他）。
- 是否允许任何 GPL 依赖（如 MuPDF）。
- 商业化与上架计划。

> 本文不是法律意见；最终选择需专业审查。
