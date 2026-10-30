# S3 UI Spike

Compose Multiplatform 桌面样例，用于验证 Aniyomi 风格三屏（库 / 详情 / 设置）迁移到 CMP + Material 3 的成本。

旧规格已随旧文档删除（git `14c495e`）。

## 运行

```bash
./gradlew -p experiments :spikes:s3-ui:run
```

## 当前范围

- 三屏 + 导航栏 + 深浅色主题
- 搜索过滤、网格、章节列表、设置开关均为本地 mock
- 无网络、无真实数据、无阅读器/播放器

## 已验证（2026-10-28）

- `./gradlew -p experiments build` 通过。
- `./gradlew -p experiments :spikes:s3-ui:run` 创建窗口并可启动；因桌面锁屏，视觉验收待补。
- 已知警告：`compose.material3` 访问器废弃；`materialIconsExtended` 固定在 1.7.3。
- MD3E：显式依赖 `material3:1.12.0-alpha03`，`LoadingIndicator` 已接入 Settings 并编译通过。
- 压力模式：Library 页 “Stress 1,000” 切换 1,000 项网格。

## 验收（待补）

- [ ] 依赖审计表（Voyager/Injekt/OkHttp/Coil/WebView/RichText）
- [ ] MD3E 组件可用性表
- [ ] 1,000 项网格性能
- [ ] 每屏迁移耗时
- [ ] 旧报告已随旧文档删除（git `14c495e`）
