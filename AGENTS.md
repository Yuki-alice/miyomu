# AGENTS.md — Miyomu Agent 协作与 Skill 调用规范

本文件对在本仓库工作的所有 AI agent 生效（Qoder、Claude Code、Codex、Cursor 等）。
目标：让 agent **主动、准确地调用已安装的 skills**，同时避免上下文膨胀、错误套用和架构回潮。

> 本文件是规范性文件（normative）。与 `.qoder/skills/INDEX.md` 配合使用；两者冲突时，以本文件的“项目架构红线”和“许可政策”为准。

---

## 1. 基本原则

1. **先路由，后加载**：任务开始时先读 `.qoder/skills/INDEX.md` 的 Routing shortcuts，选择**最小相关集合**，不要一次加载全部 skills。
2. **命中即强制**：任务命中第 3 节的路由表时，必须加载对应 `SKILL.md`，不得凭记忆作答。
3. **项目约定优先**：skill 的通用建议与本仓库的构建、架构、许可约束冲突时，以本仓库为准，并在回复中说明取舍。
4. **证据化交付**：最终回复和 PR 描述必须注明使用了哪些 skill、应用了哪条关键规则、运行了哪些验证命令及结果。
5. **不装饰、不滥用**：不把 skill 名称当引用装饰；不为简单任务加载无关 skill；不复制 GPL 代码。

---

## 2. Skill 的发现与调用

### 2.1 位置

| 类型 | 路径 |
|---|---|
| 路由总表 | `.qoder/skills/INDEX.md` |
| Compose 专家 | `.qoder/skills/compose-skill/skills/compose-expert/SKILL.md` |
| Material 3 | `.qoder/skills/material-3-skill/skills/material-3/SKILL.md` |
| Material 3 Expressive | `.qoder/skills/md3e-skill/SKILL.md` |
| chrisbanes 合集 | `.qoder/skills/chrisbanes-skills/skills/<skill-name>/SKILL.md` |
| Compose 性能合集 | `.qoder/skills/compose-performance-skills/<category>/<skill-name>/SKILL.md` |
| Kotlin 工具链合集 | `.qoder/skills/kotlin-agent-skills/skills/<skill-name>/SKILL.md` |

### 2.2 调用方式

1. **入口 skill**：使用 Qoder 的 `Skill` 工具调用（`compose-expert`、`material-3`、`md3e-skill`、`chrisbanes-skills`、`compose-performance-skills`、`kotlin-agent-skills`、`analyze-code` 等）。
2. **合集内嵌套 skill**：先读合集根 `SKILL.md` 或 `INDEX.md`，再按路径读取具体 `SKILL.md`；不要猜 skill 名。
3. **只读使用**：`.qoder/skills/` 是安装目录，除第 8 节的更新流程外不得修改。
4. **加载后执行**：读取 `SKILL.md` 后按其 Procedure 执行；如果其中的前置检查要求读 `references/`，按需读取，不要预加载整个 references 目录。

### 2.3 何时不必加载

- 纯事实查询（例如“这个文件在哪”“某符号被谁引用”）。
- 单行拼写、注释、格式化修复。
- 任务已有明确、局部的项目约定，且不涉及 Compose/Kotlin API/构建/测试/架构决策。
- 即使不加载，也必须在回复中说明“未命中强制路由，因此未加载 skill”。

---

## 3. 强制路由表（命中即加载）

| 任务信号 | 必读 skill | 说明 |
|---|---|---|
| 修改 `build.gradle.kts`、`settings.gradle.kts`、`build-logic/`、`gradle/libs.versions.toml` | `kotlin-agent-skills` → `kotlin-tooling-agp9-migration`（涉及 AGP/KMP/源集时）；`chrisbanes-skills` → `gradle-run`（执行 Gradle 前） | 构建改动必须先读 AGP 9 约束 |
| 任何 Gradle 命令、构建失败、CI 失败 | `chrisbanes-skills` → `gradle-run` | 包含 `./gradlew`、`gradlew*`、自定义 wrapper |
| Compose UI、状态、effect、导航、screen 状态持有者 | `compose-expert` + `chrisbanes-skills` → `compose-state-and-effects` | 两者职责不同：前者给 Compose API/模式，后者给状态归属与 effect 生命周期 |
| Compose 组件 API、Modifier、slot、content 参数 | `chrisbanes-skills` → `compose-component-design` | 设计可复用组件时必读 |
| Compose 动画、可见性/内容切换/属性过渡 | `chrisbanes-skills` → `compose-animations` | |
| Compose 焦点、TV、键盘、D-pad | `chrisbanes-skills` → `compose-focus-navigation` | |
| Material 3 / 主题 / 颜色 / 排版 / shape | `material-3` 或 `md3e-skill`（二选一；Expressive 用 md3e） | 不要同时加载两套设计系统 |
| 性能、卡顿、重组、稳定性、懒加载、Baseline Profile、R8 | `compose-performance-skills` → 按 `INDEX.md` 选择子 skill | 宽泛症状先用 `audit/auditing-compose-performance` |
| Compose UI 测试、截图测试、语义断言 | `chrisbanes-skills` → `compose-ui-testing-patterns` | 新 UI 逻辑应同时给出测试形态 |
| Kotlin API 归属、value class、data class、expect/actual、平台边界 | `chrisbanes-skills` → `kotlin-api-design` | 涉及 `domain`、`model`、`contract`、`source-api` 时必读 |
| 协程作用域、Flow、StateFlow/SharedFlow、取消、`runBlocking` | `chrisbanes-skills` → `kotlin-concurrency-and-flow` | |
| `when`/sealed 分支/空值分支/guard | `chrisbanes-skills` → `kotlin-control-flow` | |
| 架构审阅、技术债、跨模块重构、代码体检 | `analyze-code`；并对照 `docs/architecture-review-2026-10-30.md` | 审阅类任务默认加载 |
| 实现计划、多 agent 拆分、任务图 | `chrisbanes-skills` → `to-plan` / `implement-with-subagents` | |
| PR 跟进、CI 失败、评审意见 | `chrisbanes-skills` → `shepherd` | |
| 公开文档、发布说明、对外文案 | `chrisbanes-skills` → `grounded-writing` | |
| Kotlin 库发布准备 | `chrisbanes-skills` → `release-kotlin-library` | 仅发布库时使用 |
| Android 基准配置比较 | `chrisbanes-skills` → `android-benchmark-comparison` | 仅物理设备基准对比时使用 |

### 3.1 典型任务示例

| 任务 | 加载顺序 |
|---|---|
| 给设置页加一个开关 | `material-3` → `chrisbanes-skills/compose-state-and-effects` → `compose-component-design` |
| 修复阅读器列表卡顿 | `compose-performance-skills/auditing-compose-performance` → 按症状进入 `optimizing-lazy-layouts` / `debugging-recompositions` / `deferring-state-reads` |
| 升级 AGP 或调整 KMP 源集 | `kotlin-agent-skills/kotlin-tooling-agp9-migration` → `chrisbanes-skills/gradle-run` |
| 重构 `domain` 的偏好类型 | `chrisbanes-skills/kotlin-api-design` → `analyze-code`（复核架构红线） |
| 修复旋转后导航丢失 | `chrisbanes-skills/compose-state-and-effects` → `analyze-code`（对照 AR-01） |
| 新增一个 screen + 导航目的地 | `compose-expert` → `compose-state-and-effects` → `compose-ui-testing-patterns` |
| 审阅一个 PR 的 Compose 设计 | `compose-expert`（pr-review 参考）→ `compose-component-design` / `compose-state-and-effects` |

---

## 4. 项目架构红线（新代码必须遵守）

依赖方向：

```
model ← contract ← data / source-* ← ui-* ← ui ← application
```

- **禁止依赖环**。
- 新代码包名统一 `app.miyomu.*`；移植代码用 `app.miyomu.ported.*` 过渡，不新增 `tachiyomi` / `eu.kanade` 包根。
- `domain`、`model`、`contract` 不得依赖 Compose。当前 `domain` 的 Compose 依赖是待偿还债务（AR-04），不得新增。
- `ui-*` feature 不得直接依赖具体数据实现（`data`、`local`、`source-runtime` 等），应依赖端口/graph 接口。
- feature 模块之间不得为了共享小工具而互相依赖；共享工具放 `presentation-core` 或 `ui-core`。
- 新导航目的地：新增 `@Serializable MiyomuKey` + 在 `MiyomuNavHost` 的 `entryProvider` 中注册；key 只携带身份（id/title/index），不得携带 graph、对象或 lambda。
- 长生命周期服务（DB、HTTP client、下载队列、源运行时）由 application/平台层持有并实现 `Closeable`；不得在 composition 中创建后无人关闭。
- screen 状态：immutable state + callbacks；副作用使用生命周期匹配的 effect；禁止在主线程 `runBlocking`。
- 新逻辑必须有最小验证：`domain`/`data`/`source-*` 加 `jvmTest` 或 `commonTest`；UI 流程加 Compose UI 测试。

---

## 5. 标准工作流

### 5.1 任务开始（30 秒）

1. 读 `.qoder/skills/INDEX.md` 的 Routing shortcuts。
2. 判断任务类型；命中第 3 节路由表则加载 1–2 个最相关 skill。
3. 涉及构建/Gradle 时额外加载 `gradle-run`。
4. 超过 3 个文件或跨模块时，先建立任务列表（TaskCreate/TaskUpdate），记录依赖关系。

### 5.2 实施

- 按 `SKILL.md` 的 Procedure 执行；在计划中写明“将应用哪条规则”。
- 对照第 4 节架构红线；若任务本身就是偿还债务，先写清“当前违反哪条红线、目标状态是什么”。
- 不引入任务不需要的抽象、默认值、兼容层或新框架。
- 移植代码（`tachiyomi` / `eu.kanade` / `aniyomi` / `mihon` 包根）修改时，保留原有 “CMP port” 注释与来源说明。

### 5.3 验证

- 最小验证：`./gradlew :shared:<module>:jvmTest`；跨模块改动运行 `./gradlew jvmTest`。
- UI 改动：优先运行对应 Compose UI 测试或桌面应用；无法运行时必须在回复中明确说明“未做 UI 验证”。
- 构建改动：至少运行 `./gradlew projects` + 受影响模块的编译/测试。
- 记录命令、退出码、失败原因；不要把“未运行”写成“通过”。

### 5.4 交付

最终回复必须包含一个简短段落：

```
Skills applied:
- <skill-name> — <应用的关键规则>
- <skill-name> — <应用的关键规则>
Verification:
- <command> — <result>
```

PR 描述同样包含该段落。若未加载 skill，写明原因（未命中强制路由/纯查询/单行修复）。

---

## 6. 反模式（禁止）

- 一上来加载全部 skills，或为了“显得专业”堆砌 skill 名称。
- 只引用 skill 名称、不读 `SKILL.md` 就按其名义给建议。
- 把 skill 的通用建议凌驾于项目的 AGP 9、KMP、许可或架构红线之上。
- 用 skill 生成未经验证的 API、版本号或依赖坐标。
- 在 `domain` 新增 Compose import；在 feature 模块新增对 `data` 实现的直接依赖；新增第二个服务定位器。
- 新代码继续使用 Voyager `ScreenModel`（运行时导航是 Navigation 3）。
- 复制 GPL 代码、删除 NOTICE/LICENSE、把 `reference/` 下的上游代码提交进仓库。
- 在未运行验证的情况下宣称任务完成。

---

## 7. 许可与上游代码

- 项目许可：Apache-2.0；不得复制 GPL 代码。
- 保留根目录 `LICENSE`、`NOTICE` 及移植文件头部的来源说明。
- `reference/` 是本地参考资料，已在 `.gitignore` 中忽略，**不得提交**。
- 新增/修改移植代码时：
  - 保留 “CMP port: …” 注释；
  - 在 `NOTICE` 中记录上游项目与版本/commit；
  - 不把 Miyomu 自有代码混入上游包根；新代码放在 `app.miyomu.*`。

---

## 8. Skill 的维护

- 更新 skills 时按 `.qoder/skills/README.md` 的 shallow clone + rsync 流程执行，不要删除本地 `SKILL.md` 路由器。
- 更新后同步：
  1. `.qoder/skills/INDEX.md`；
  2. `.qoder/skills/README.md` 的版本/commit 表；
  3. 本文件的第 2、3 节（如有新增/删除/改名）。
- 每次更新记录上游 commit 与许可证复核结果；发现 GPL/冲突许可证的 skill 不得安装。
- 本文件的修改需要通过 PR，并在 PR 描述中说明对现有 agent 工作流的影响。

---

## 9. 快速检查清单

- [ ] 已读 `.qoder/skills/INDEX.md` 的 Routing shortcuts
- [ ] 命中强制路由时已加载并读取对应 `SKILL.md`
- [ ] 构建改动已读 `kotlin-tooling-agp9-migration` 与 `gradle-run`
- [ ] 未违反第 4 节架构红线
- [ ] 已运行最小验证，并记录命令与结果
- [ ] 最终回复包含 “Skills applied” 与 “Verification”
- [ ] 未提交 `reference/`、未复制 GPL 代码、未删除 NOTICE/LICENSE

---

## 10. 相关文档

- `.qoder/skills/INDEX.md` — 61 个 SKILL.md 的完整路由
- `.qoder/skills/README.md` — 安装来源、版本、许可与更新流程
- `docs/product-goals.md` — 产品定位、P0/P1/P2 功能范围与决策记录
- `docs/target-architecture.md` — 目标模块/架构设计（设计参考，未批准执行）
- `docs/architecture-review-2026-10-30.md` — 当前架构债务与修复路线图
- `gradle/libs.versions.toml` — 工具链与依赖版本
- `build-logic/src/main/kotlin/` — 约定插件与构建常量
