# Miyomu — Agent Guide

This is the shared instruction file for **every agent** in this workspace: Explore, Plan, general-purpose, and any custom subagent. It is not optional reading.

## Project state

- Product: **Miyomu** — cross-platform video / novel / comic reader.
- **2026-10-30 (evening): the code compiles, but the project is NOT usable.** The user's real-device test found it "基本不可用"; do not claim feature completeness from fixture/construction/static tests. Start from `docs/00-current-status.md`. Android + desktop compile; iOS is not started; 210 module-level/fixture tests pass; all 13 reference Venera sources construct and hitomi's single real-site path works, but none of these prove a user can import data/sources and actually read or play. Feature work is paused; prioritize reproducing real-use failures and hardening import → use → restart flows. The old ADR/plan/handoff docs were deleted on 2026-10-30; the decision digest is `docs/02-decisions.md`. **Packaging/release is deferred.**
- Old ADRs, plans, handoffs and checklists were deleted on 2026-10-30. If their content is needed, recover it from git history `14c495e`; the current minimal docs are `docs/00-current-status.md` through `docs/03-verification.md`.
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

- **Parallel work**: two machines, one short branch + PR per goal; nightly goal window 23:00–09:00 (08:45 stop/commit/push). The old handoff with those rules was deleted; recover it from git `14c495e` if needed.
- **Schema discipline**: Room is at version 4 (`entry_episode`, `entry_chapter`, `video_resolve_cache`, `category`, `manga_category`, `library_updates`). Only one branch may change `shared/data/schemas`, `shared/model`, or the `MiyomuGraph`/`MiyomuApp`/`HomeScreen` wiring at a time.
- **Known gaps**: **real-usage validation failed (user, 2026-10-30); detailed failure list pending**; Windows packaging/MPV verification, external subtitles (blocked on mediamp 0.5.3), player lock/PiP/MediaSession, local 7z/PDF loaders, source coverage/canary automation, iOS (no `iosMain` target exists at all), online novel body search, no release artifacts. Venera compatibility is measured by `tools/venera_compat.py` + `VeneraSourceCompatTest` — see `docs/03-verification.md`; the 13/13 numbers cover construction + static API only, and hitomi covers one search → details → pages path; **they are not usability evidence**, and the remaining sources are unverified.
- **Validation rule**: only claim a feature works after a fresh-data UI walkthrough (import → use → restart → persistence). Module/fixture tests, source construction, and static audits are not evidence of usability.
- **Novel wiring**: `HomeScreen` has six tabs with `NovelLibraryTab` at index 0 (manga/updates/history/browse/more shifted by one). Novel bookmarks, reader prefs and legado sources all live in the JSON `PreferenceStore`, so the novel side still has **zero Room schema impact** — keep it that way unless a migration is planned.
- **CI gaps**: CI is runnable as of 2026-10-30 (canonical repositories on runners; `download`/`danmaku` tests included), but it only validates compilation and module tests, not real usability; no Windows job; no packaging smoke.

## Canonical files

- Current status: `docs/00-current-status.md` (single source of truth)
- Architecture: `docs/01-architecture.md`
- Decisions: `docs/02-decisions.md`
- Verification: `docs/03-verification.md`
- Docs index: `docs/README.md`
- Old docs: deleted 2026-10-30; recover from git `14c495e` if needed
