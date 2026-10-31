# Installed Skills

Installed in the workspace on **2026-10-28** so every agent in this project can use them.
Tracked in the Miyomu repository since **2026-10-31**; licenses are listed below and in the root `NOTICE`.

| Collection | Upstream | Commit | License | Skills |
|---|---|---|---|---|
| `compose-skill` | https://github.com/aldefy/compose-skill | `954ef54` | MIT | 1 (`compose-expert`) |
| `material-3-skill` | https://github.com/hamen/material-3-skill | `14385f2` | MIT | 1 (`material-3`) |
| `md3e-skill` | https://github.com/mfskys/md3e-skill | `640264b` | Apache-2.0 | 1 (`md3e`) |
| `chrisbanes-skills` | https://github.com/chrisbanes/skills | `8fc8836` | Apache-2.0 | 19 |
| `compose-performance-skills` | https://github.com/skydoves/compose-performance-skills | `1b32f81` | Apache-2.0 | 26 |
| `kotlin-agent-skills` | https://github.com/Kotlin/kotlin-agent-skills | `c2f9069` | Apache-2.0 | 10 |

Total: **58 upstream skills** in 6 collections, plus **3 collection routers** (`chrisbanes-skills`, `compose-performance-skills`, `kotlin-agent-skills`) for Qoder discovery — 61 `SKILL.md` files. Each collection keeps its full upstream tree so relative `references/`, `assets/`, and `scripts/` paths keep working.

## Layout

```
.qoder/skills/
  INDEX.md                       # full catalog and routing
  compose-skill/                 # full upstream repo
  material-3-skill/
  md3e-skill/
  chrisbanes-skills/             # + root router SKILL.md
  compose-performance-skills/    # + root router SKILL.md
  kotlin-agent-skills/           # + root router SKILL.md
```

Qoder exposes six entry points (three single skills + three collection routers). Nested skills are reached through the collection root `SKILL.md` and `INDEX.md`. If a running session does not show the new skills yet, reload the workspace/session.

## Update

Use a shallow clone and copy without deleting local router files:

```bash
git clone --depth 1 <upstream-url> /tmp/skill-update
rsync -a --exclude .git /tmp/skill-update/ .qoder/skills/<collection>/
```

Re-check licenses and commits after updating. Do not add skills that conflict with the Miyomu license policy (Apache-2.0, no GPL code copying).
