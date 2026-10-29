# Miyomu — Agent Guide

This is the shared instruction file for **every agent** in this workspace: Explore, Plan, general-purpose, and any custom subagent. It is not optional reading.

## Project state

- Product: **Miyomu** — cross-platform video / novel / comic reader.
- Phase 0 complete; decision **Go (reduced scope)**: Android + desktop first, iOS later.
- Read `docs/05-phase-1-plan.md` for milestones and `docs/checklists/phase-0.md` for evidence.
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

## Phase 1 focus

- M0: unify the Android toolchain conflict (AGP 9.4 / Kotlin 2.2 vs quickjs-kt Kotlin 2.4), adopt Ktor, persistent cookies, Miyomu identifiers.
- Player: add audio-track / embedded-subtitle selection behind a backend abstraction.
- UI: fork + strangler migration to CMP; follow Material 3 / MD3E guidance and the S3 evidence.
- iOS: validate AVKit in a real app shell before expanding scope.

## Canonical files

- Product: `docs/00-product-brief.md`
- Architecture: `docs/01-architecture.md`
- Phase 1: `docs/05-phase-1-plan.md`
- Brand: `docs/06-brand.md`
