# Miyomu — Agent Guide

This is the shared instruction file for **every agent** in this workspace: Explore, Plan, general-purpose, and any custom subagent. It is not optional reading.

## Project state

- Product: **Miyomu** — cross-platform video / novel / comic reader.
- Phase 0 complete; decision **Go (reduced scope)**: Android + desktop first, iOS later.
- Read `docs/05-phase-1-plan.md` for milestones and `docs/checklists/phase-0.md` for evidence.
- Production build lives at the repository root (`androidApp/`, `desktopApp/`, `shared/`); Phase 0/1 probes and spikes live in `experiments/` as a separate Gradle build (`./gradlew -p experiments ...`).
- Do not re-run Phase 0 spikes unless explicitly asked; build on the reports.

## Skill policy (mandatory)

1. **Before any Compose / Compose Multiplatform / Material 3 / MD3E / Kotlin / Gradle / performance task**, read `.qoder/skills/INDEX.md`.
2. Select **1–3 relevant skills**. Open the collection router if needed, then read the selected nested `SKILL.md` **in full**.
3. Do not load every skill. Do not load unrelated skills. Prefer the smallest set that covers the task.
4. Use skills **proactively** — do not wait for the user to name a skill. If a task matches a trigger below, apply it.
5. For code changes, after applying a skill, run the relevant build/tests. For UI changes, also apply testing/performance guidance where relevant.
6. Project docs and explicit user decisions override a skill. If they conflict, follow the project and note the conflict.
7. When delegating to a subagent, include the exact skill paths it should read; subagents are still bound by this file.
8. If a subagent does not have the Skill tool, it must open the listed `SKILL.md` path with Read directly. Skills are plain files; tool availability is not an excuse to skip them.

## Skill entry points

| Skill | Type | Use when | Location |
|---|---|---|---|
| `compose-expert` | direct | Compose / CMP UI, state, effects, navigation, paging, modifiers, design-to-code | `.qoder/skills/compose-skill/skills/compose-expert/SKILL.md` |
| `material-3` | direct | Material 3 tokens, color, typography, shape, components, theming | `.qoder/skills/material-3-skill/skills/material-3/SKILL.md` |
| `md3e-skill` | direct | Material 3 Expressive components, motion, theming | `.qoder/skills/md3e-skill/SKILL.md` |
| `chrisbanes-skills` | collection | Compose state/effects, animations, component design, performance, UI tests, Kotlin API/concurrency, Gradle, planning | `.qoder/skills/chrisbanes-skills/SKILL.md` |
| `compose-performance-skills` | collection | Recomposition, stability, jank, lazy lists/prefetch, baseline profiles, R8, hot reload | `.qoder/skills/compose-performance-skills/SKILL.md` |
| `kotlin-agent-skills` | collection | AGP 9 migration, Kotlin/Gradle toolchain, CocoaPods→SPM, Java→Kotlin, native build performance | `.qoder/skills/kotlin-agent-skills/SKILL.md` |

The full catalog (58 upstream skills + 3 collection routers) with descriptions and paths is in `.qoder/skills/INDEX.md`.

## Routing by task

- **Writing/reviewing Compose UI** → `compose-expert` + the relevant `chrisbanes-skills` nested skill (`compose-component-design`, `compose-state-and-effects`, `compose-animations`, `compose-focus-navigation`).
- **Material 3 / MD3E** → `material-3` or `md3e-skill`; for expressive components use `md3e-skill` first.
- **Performance / jank / stability** → `compose-performance-skills`; use `compose-performance` or the matching nested skill. Measure before changing.
- **Lazy lists / 1,000-item grids** → `optimizing-lazy-layouts`, `configuring-lazy-prefetch`, `deferring-state-reads`.
- **Compose tests / screenshots** → `compose-ui-testing-patterns`; for benchmarks use `android-benchmark-comparison`.
- **Kotlin API / concurrency** → `kotlin-api-design`, `kotlin-concurrency-and-flow`.
- **Gradle / AGP / KMP toolchain** → `kotlin-agent-skills`; Miyomu has a known AGP 9.4 vs quickjs-kt 2.4 conflict, read `kotlin-tooling-agp9-migration` before touching the build.
- **Planning / multi-step implementation** → `to-plan`, `implement-with-subagents`, `shepherd`.
- **Public docs / release notes** → `grounded-writing`.
- **Kotlin library release** → `release-kotlin-library`.

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

- **Explore**: read-only search; may consult skills for vocabulary/patterns but must not edit.
- **Plan**: read `to-plan` when producing implementation plans; include skill paths and verification steps.
- **general-purpose / implementer**: read `AGENTS.md`, the skill index, and the selected skills before editing; run build/test after changes.
- **All agents**: cite the skill files used in your working notes or final summary when they materially influenced the solution.

## Phase 1 focus

- M0: unify the Android toolchain conflict (AGP 9.4 / Kotlin 2.2 vs quickjs-kt Kotlin 2.4), adopt Ktor, persistent cookies, Miyomu identifiers.
- Player: add audio-track / embedded-subtitle selection behind a backend abstraction.
- UI: fork + strangler migration to CMP; use Material 3 / MD3E skills and the S3 evidence.
- iOS: validate AVKit in a real app shell before expanding scope.

## Canonical files

- Product: `docs/00-product-brief.md`
- Architecture: `docs/01-architecture.md`
- Phase 1: `docs/05-phase-1-plan.md`
- Skills: `.qoder/skills/INDEX.md`, `.qoder/skills/README.md`
- Brand: `docs/06-brand.md`
