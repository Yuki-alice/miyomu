# Miyomu

> 一个书架，装下所有故事。跨平台漫画 / 小说 / 视频阅读器。

Miyomu 以 [Kototoro](reference/Kototoro) v2.3.4 为基线，做三件事：

1. **先改架构**：把 Kototoro 堆在 `app` 里的核心功能，搬进清晰的 `core / feature / platform / source` 模块。
2. **功能先照搬**：数据、源协议、阅读器几何、数据库、备份、扩展运行时直接复用，先保 parity，不主动裁剪。
3. **UI 全新 MD3E**：不复用旧页面，按 Material 3 Expressive 重新设计；原型与架构迁移并行。

当前处于 **P1 进行中**：三端可构建/启动；核心模块与 **374 个测试**通过；第一条真实数据流（Fixture → Repository → UI）已打通。

## 文档

完整索引见 [docs/README.md](docs/README.md)。

| 文档 | 内容 | 状态 |
| --- | --- | --- |
| [架构优先修正方案](docs/07-architecture-first.md) | **当前权威方案**：功能照搬、模块重构、迁移顺序 | ✅ |
| [MD3E 设计系统](docs/08-md3e-design.md) | **当前权威 UI 方向**：色彩/字体/形状/动效/组件 | ✅ |
| [初始化方案](docs/09-initialization.md) | **当前权威初始化**：20 模块 + build-logic、Room、MD3E、迁移顺序 | ✅ |
| [P0 状态](docs/10-p0-status.md) | 三端构建结果、截图、已知问题与 P1 下一步 | ✅ |
| [P1 状态](docs/11-p1-status.md) | reader-core/source/net 复制、372 测试 | ✅ |
| [build-logic](docs/12-build-logic.md) | 约定插件、模块写法与验证 | ✅ |
| [Navigation](docs/13-navigation.md) | Navigation Compose KMP、路由与返回栈 | ✅ |
| [数据流](docs/14-data-flow.md) | Fixture → Repository → UI 的第一条真实链路 | ✅ |
| [参考项目分析](docs/00-reference-analysis.md) | Kototoro 的规模、机制、可借鉴点与风险 | 分析材料 |
| [技术栈初稿](docs/01-tech-stack.md) | KMP + CMP 选型；DB/ID 部分已被 07 修正 | 初稿 |
| [架构初稿](docs/02-architecture.md) | 模块/SourceRuntime/DB；部分结论已被 07 修正 | 初稿 |
| [功能矩阵](docs/03-feature-matrix.md) | 功能 parity 矩阵与迁移映射 | ✅ |
| [UI 初稿](docs/04-ui-ux-design.md) | Sumi 方向已作废，保留布局/阅读器思路 | 初稿 |
| [初始化初稿](docs/05-initialization-plan.md) | Gradle/CI 细节可参考；迁移顺序已被 07 修正 | 初稿 |
| [路线图初稿](docs/06-roadmap.md) | 里程碑可参考；阶段已被 07 调整 | 初稿 |
| [ADR](docs/adr/) | 技术决策记录 | 部分需更新 |

## 快速开始

```bash
./gradlew :app:android:assembleDebug
./gradlew :app:desktop:run
./gradlew :app:desktop:test --tests app.miyomu.desktop.ScreenshotTest

cd iosApp && xcodegen generate
xcodebuild -project Miyomu.xcodeproj -scheme Miyomu \
  -configuration Debug -sdk iphonesimulator \
  -destination 'platform=iOS Simulator,name=iPhone 17' \
  CODE_SIGNING_ALLOWED=NO build
```

## 当前决策

- **架构优先**，不做“先换皮再重构”。
- **核心功能照搬**，保留 Room schema、Long ID、备份格式、SourceRuntime、reader-core。
- **新 UI 用 MD3E**，接受 Material3 Expressive alpha 依赖并封装隔离。
- **原型并行**，但不阻塞架构迁移。

## 非目标

- 不在第一阶段重写数据库、ID 体系、解析器 ABI。
- 不为了“极简”删除核心功能。
- 不把旧 UI 的玻璃拟态/多套卡片带进新设计。
- 不内置、不推荐任何第三方内容源或仓库；不随包分发受版权保护的内容。

## 许可

计划采用 Apache License 2.0。复用 Kototoro（Apache-2.0）代码时保留版权头与 `NOTICE`；不把 GPL/AGPL 实现直接并入共享内核。第三方扩展与解析器由用户自行安装，遵循其各自许可。

Miyomu 与 Kototoro、Kotatsu、Mihon、Aniyomi 等项目不存在隶属关系。
