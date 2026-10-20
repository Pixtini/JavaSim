---
name: javasim-create-game
description: Implement or extend a JavaSim slot game from reviewed rules, integrating config, Basegame, randomly triggered Feature Games, results, registration, and tests. Use for game implementation; source review alone does not invoke this workflow.
---

# Implement a JavaSim game

Work from the repository root, read applicable `AGENTS.md` files, and inspect the target game's sources and review decisions. Apply the [root validation policy](../../../AGENTS.md) to the work actually performed: the implementation checks below apply when game/config/runtime behaviour changes or validation is explicitly requested. Editing this skill, its non-executable templates, or game documentation alone does not require a game run. Read [implementation references](references/implementation.md) for current contracts and tools.

1. Establish scope and source readiness. Use `docs/game-creation-form.md` as the canonical checklist. Resolve blocking ambiguities before implementing affected rules; continue independent work and record explicitly accepted assumptions. Unprovided balance targets are not a universal prerequisite.
2. For a new workspace, reuse `toolkit.wizard.GameProjectWizard.create` through the New game GUI or a temporary caller. It allocates the code and writes source templates, package locations, and a blank PAR; it does not generate game logic. Preserve populated workspaces and source documents.
3. Explain significant architecture changes first. Implement game-owned config, focused Basegame/Feature Game components, typed results, and a complete-round `GameSession`. Reuse appropriate GMF calculations; assess helpers for a clear reusable algorithm without adding speculative rules to common components.
4. Use the supplied session RNG for deterministic behavior. Separate purchase cost from payout/cap units. Implement approved triggers, state transitions, retriggers, and cap timing explicitly. Generic simulation, statistics, and reporting remain outside games.
5. Register the stable ID in `GameFactory`; inspect actual GUI selection mechanisms. Add metadata and optional player/viewer, exhaustive, or replay support only as requested or needed. Optional tooling is not automatically required for every game.
6. Test rules with controlled outcomes, invalid config, round accounting, caps, and repeatability. Check existing consumers when common code changes. Compile, run relevant suites, and run a small seeded simulation before larger validation. Use the validation skill's [controlled run guide](../javasim-validate-math/references/execution.md) without persistent config edits.
7. Update the creation form and handoff with actual evidence using [the handoff template](assets/implementation-handoff.md). Update `docs/architecture.md`. Identify incomplete checks and unexplained anomalies.

Do not change mathematical configuration without authorization. A blank wizard workbook is not valid math. Record proposed framework improvements separately unless necessary and within scope; follow applicable instructions before implementing them.
