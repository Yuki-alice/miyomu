# Miyomu — Agent Guide

This is the shared instruction file for **every agent** in this workspace: Explore, Plan, general-purpose, and any custom subagent. It is not optional reading.

## Project state

- Product: **Miyomu** — cross-platform video / novel / comic reader.
- **2026-10-30: Phase A/B/C/D complete.** Android + desktop are the production targets (both builds green); iOS is not started. The current handoff is `docs/11-handoff-2026-10-30.md`; the anime/player plan is `docs/10-anime-source-and-player-plan.md`; decisions are ADR-0011/0012/0013/0014.
- `docs/05-phase-1-plan.md`, `docs/08-m1-task-plan.md`, and `docs/09-handoff-2026-10-29.md` are **historical** (pre-Phase-D) and are kept for implementation records only.
- Production build lives at the repository root (`androidApp/`, `desktopApp/`, `shared/`); Phase 0/1 probes and spikes live in `experiments/` as a separate Gradle build (`./gradlew -p experiments ...`).
- Do not re-run Phase 0 spikes unless explicitly asked; build on the reports.

## Skill policy (temporarily disabled)

**Disabled 2026-10-29**: the `.qoder/skills/` library is not present on this machine, so the previously mandatory skill policy (read `INDEX.md` before Compose / Kotlin / Gradle tasks; select 1–3 skills; route by task) is suspended.

- Do not attempt to read `.qoder/skills/**`; rely on project docs, ADRs, and standard best practice instead.
- To restore: re-sync the `.qoder/` directory, then recover the original "Skill policy", "Skill entry points", and "Routing by task" sections from git history (`git log -p -- AGENTS.md`, before the 2026-10-29 removal; uncommitted working tree: `git diff -- AGENTS.md`).

## Reference repositories

Local shallow clones (not committed; `reference/` is gitignored):

| Path | Upstream | License | Usage |
|---|---|---|---|
| `reference/aniyomi` | https://github.com/aniyomiorg/aniyomi | Apache-2.0 | UI, reader loaders, domain, tracker patterns; code reuse allowed with NOTICE |
| `reference/venera-next` | https://github.com/CyrilPeng/Venera-Next | GPL-3.0 | Behavior/design reference only; **do not copy code** unless the project relicenses to GPL |
| `reference/mediamp` | https://github.com/open-ani/mediamp | Apache-2.0 | Player API and backend reference; dependency allowed |

Rules:

- Read reference code to understand behavior; do not paste GPL Venera code into Miyomu.
- Aniyomi/mediamp reuse must keep copyright/NOTICE notices.
- Do not commit `reference/`; it is a local checkout.

## Agent behavior

- **Explore**: read-only search; must not edit.
- **Plan**: produce implementation plans with concrete verification steps.
- **general-purpose / implementer**: read `AGENTS.md` before editing; run build/test after changes.
- **All agents**: follow project docs and explicit user decisions; they override general guidance.

## Current focus (2026-10-30)

- **Parallel work**: two machines, one short branch + PR per goal; nightly goal window 23:00–09:00 (08:45 stop/commit/push). See `docs/11-handoff-2026-10-30.md` §5.
- **Schema discipline**: Room is at version 4 (`entry_episode`, `entry_chapter`, `video_resolve_cache`, `category`, `manga_category`, `library_updates`). Only one branch may change `shared/data/schemas`, `shared/model`, or the `MiyomuGraph`/`MiyomuApp`/`HomeScreen` wiring at a time.
- **Known gaps**: Windows packaging/MPV verification, external subtitles (blocked on mediamp 0.5.3), player lock/PiP/MediaSession, novel reader + local 7z/PDF/EPUB/TXT, source coverage/canary automation, iOS shell.
- **CI gaps**: `download`/`danmaku` tests are not in CI; no Windows job; no packaging smoke.

## Canonical files

- Product: `docs/00-product-brief.md`
- Architecture: `docs/01-architecture.md` (note: module list predates Phase D; see README/11)
- Current handoff: `docs/11-handoff-2026-10-30.md`
- Anime/player plan: `docs/10-anime-source-and-player-plan.md`
- Brand: `docs/06-brand.md`
- Historical plans: `docs/05-phase-1-plan.md`, `docs/08-m1-task-plan.md`, `docs/09-handoff-2026-10-29.md`
