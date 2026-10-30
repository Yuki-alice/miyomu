# Miyomu — Agent Guide

This is the shared instruction file for **every agent** in this workspace: Explore, Plan, general-purpose, and any custom subagent. It is not optional reading.

## Project state

- Product: **Miyomu** — cross-platform video / novel / comic reader.
- **2026-10-30 (afternoon): the novel line is merged and pushed; see `git log -1` for the baseline.** Start from `docs/15-handoff-2026-10-30-afternoon.md`. Android + desktop are the production targets (both builds green); iOS is not started. All three media (comic / video / novel) now live on one branch. The Phase-D handoff is `docs/11-handoff-2026-10-30.md`; the anime/player plan is `docs/10-anime-source-and-player-plan.md`; the novel plan is `docs/12-novel-plan.md`; decisions are ADR-0011/0012/0013/0014.
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
| `reference/NovelDokusha` | https://github.com/nanihadesuka/NovelDokusha | GPL-3.0 | Novel-side information architecture blueprint (Compose); design reference only, **do not copy code** |
| `reference/legado-with-MD3` | https://github.com/HapeLee/legado-with-MD3 | GPL-3.0 | Chinese web-novel typography conventions and chapter-regex behavior; design/behavior reference only, **do not copy code** |
| `reference/book-story` | https://github.com/Acclorite/book-story | GPL-3.0 | Material You reader typography/settings design reference; **do not copy code** |
| `reference/Myne` | https://github.com/Pool-Of-Tears/Myne | Apache-2.0 | EPUB reading implementation reference; code reuse allowed with NOTICE |
| `reference/material-3-expressive-catalog` | https://github.com/meticha/material-3-expressive-catalog | Apache-2.0 | MD3E component catalog; code reuse allowed with NOTICE |

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
- **Known gaps**: Windows packaging/MPV verification, external subtitles (blocked on mediamp 0.5.3), player lock/PiP/MediaSession, local 7z/PDF loaders, source coverage/canary automation, iOS (no `iosMain` target exists at all), legado discovery/category pages, no release artifacts, CI green since 2026-10-30. Venera source compatibility is measured by `tools/venera_compat.py` + `VeneraSourceCompatTest` — see `docs/14-venera-compat.md`; all 13 reference sources construct and the static API surface has no gaps; real-site round-trip is still unverified.
- **Novel wiring**: `HomeScreen` has six tabs with `NovelLibraryTab` at index 0 (manga/updates/history/browse/more shifted by one). Novel bookmarks, reader prefs and legado sources all live in the JSON `PreferenceStore`, so the novel side still has **zero Room schema impact** — keep it that way unless a migration is planned.
- **CI gaps**: CI is green as of 2026-10-30 (canonical repositories on runners; `download`/`danmaku` tests included); no Windows job; no packaging smoke.

## Canonical files

- Product: `docs/00-product-brief.md`
- Architecture: `docs/01-architecture.md` (note: module list predates Phase D; see README/11)
- Current handoff: `docs/15-handoff-2026-10-30-afternoon.md` (Phase-D era: `docs/11-handoff-2026-10-30.md`)
- Anime/player plan: `docs/10-anime-source-and-player-plan.md`
- Novel plan: `docs/12-novel-plan.md` (benchmark research: `docs/13-novel-benchmark.md`)
- Brand: `docs/06-brand.md`
- Historical plans: `docs/05-phase-1-plan.md`, `docs/08-m1-task-plan.md`, `docs/09-handoff-2026-10-29.md`
