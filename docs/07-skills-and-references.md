# Skills 与参考仓库

> **历史记录（2026-10-29 起）**：`.qoder/skills` 不在本机，技能策略已停用（见 AGENTS.md）。下表为历史安装记录；参考仓库部分仍有效。

- 安装日期：2026-10-28
- 使用策略：`AGENTS.md` / `agent.md`
- 技能索引：`.qoder/skills/INDEX.md`

## 已安装 Skills

全部位于 `.qoder/skills/`，保留上游完整目录（相对 `references/`、`assets/`、`scripts/` 可用）。

| 集合 | 上游 | Commit | 许可 | 上游技能数 |
|---|---|---|---|---|
| `compose-skill` | https://github.com/aldefy/compose-skill | `954ef54` | MIT | 1 |
| `material-3-skill` | https://github.com/hamen/material-3-skill | `14385f2` | MIT | 1 |
| `md3e-skill` | https://github.com/mfskys/md3e-skill | `640264b` | Apache-2.0 | 1 |
| `chrisbanes-skills` | https://github.com/chrisbanes/skills | `8fc8836` | Apache-2.0 | 19 |
| `compose-performance-skills` | https://github.com/skydoves/compose-performance-skills | `1b32f81` | Apache-2.0 | 26 |
| `kotlin-agent-skills` | https://github.com/Kotlin/kotlin-agent-skills | `c2f9069` | Apache-2.0 | 10 |

共 58 个上游 skill + 3 个集合路由 `SKILL.md`。Qoder 当前识别 6 个入口：

- 单技能：`compose-expert`、`material-3`、`md3e-skill`
- 集合：`chrisbanes-skills`、`compose-performance-skills`、`kotlin-agent-skills`

使用规则：所有 agent 先读 `.qoder/skills/INDEX.md`，选 1–3 个匹配技能，再读完整 `SKILL.md`；不要一次加载全部。

### 更新

```bash
git clone --depth 1 <upstream-url> /tmp/skill-update
rsync -a --exclude .git /tmp/skill-update/ .qoder/skills/<collection>/
```

更新后核对许可与 commit，并保留三个集合路由文件。

## 参考仓库

位于 `reference/`，均为 `--depth 1` 浅克隆；`reference/` 已加入 `.gitignore`。

| 路径 | 上游 | Commit | 许可 | 用途与限制 |
|---|---|---|---|---|
| `reference/aniyomi` | https://github.com/aniyomiorg/aniyomi | `9741444` | Apache-2.0 | UI、阅读器 loader、领域模型、追踪参考；可复用代码但保留版权/NOTICE |
| `reference/venera-next` | https://github.com/CyrilPeng/Venera-Next | `8bf22f0` | GPL-3.0 | 只参考行为与设计；**不得复制代码**到 Apache-2.0 的 Miyomu |
| `reference/mediamp` | https://github.com/open-ani/mediamp | `a813e63` | Apache-2.0 | 播放器 API/后端参考；可作为依赖使用 |

参考仓库保留各自 `.git`，便于更新；不纳入 Miyomu 的版本控制，也不随项目分发。

## 通用 agent 配置

- `AGENTS.md` 是所有 agent 的共享指令（Explore / Plan / general-purpose / 自定义子 agent）。
- `agent.md` 是兼容入口，指向 `AGENTS.md`。
- 技能路由按任务匹配；子 agent 的任务描述必须带上要读的 skill 路径。
- 项目文档与用户决策优先于 skill 建议；若冲突，记录冲突。
