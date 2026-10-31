---
name: compose-performance-skills
description: "Jetpack Compose performance skill collection: recomposition, stability, lazy lists, prefetch, baseline profiles, release-mode testing, R8, modifier chains, side effects, and HotSwan hot reload. Read INDEX.md and the matching nested SKILL.md."
---

# Compose Performance Skills Collection

This directory contains 26 upstream skills from `skydoves/compose-performance-skills` (Apache-2.0).

## Routing

1. Read `INDEX.md` to see the full catalog.
2. Pick the smallest relevant set of nested `SKILL.md` files.
3. Read each selected `SKILL.md` fully before applying it.

Common routes:

- Full audit: `audit/auditing-compose-performance/SKILL.md`
- Recompose debugging: `recomposition/debugging-recompositions/SKILL.md`
- Deferred state reads: `recomposition/deferring-state-reads/SKILL.md`
- derivedStateOf choice: `recomposition/choosing-derivedstateof/SKILL.md`
- Stability diagnosis: `stability/diagnosing-compose-stability/SKILL.md`
- Stabilizing types: `stability/stabilizing-compose-types/SKILL.md`
- Lazy list jank / prefetch: `lists/optimizing-lazy-layouts/SKILL.md`, `lists/configuring-lazy-prefetch/SKILL.md`
- Baseline profiles / release mode: `measurement/generating-baseline-profiles/SKILL.md`, `measurement/testing-compose-in-release-mode/SKILL.md`
- Collecting flows safely: `side-effects/collecting-flows-safely/SKILL.md`
- Modifier ordering/migration: `modifiers/ordering-modifier-chains/SKILL.md`, `modifiers/migrating-to-modifier-node/SKILL.md`

## Source and license

Upstream: https://github.com/skydoves/compose-performance-skills — Apache-2.0. Keep this notice when redistributing.
