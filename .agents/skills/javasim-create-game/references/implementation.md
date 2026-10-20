# Implementation references

Verify API details in current code before generating implementation or commands.

| Resource | Use |
|---|---|
| [Creation workflow](../../../../docs/game-creation-flow.md) | End-to-end workflow and optional integrations |
| [Creation form](../../../../docs/game-creation-form.md) | Canonical intake/completion checklist, copied by wizard |
| [Architecture](../../../../docs/architecture.md) | Layers, tools and test entry points |
| [Wizard](../../../../toolkit/wizard/GameProjectWizard.java) | `create(Path gamesRoot, String gameName)`; no CLI main |
| [Game](../../../../game/Game.java), [GameSession](../../../../game/GameSession.java), [results](../../../../game/model/) | Metadata, RNG, round, cap and replay contracts |
| [GameFactory](../../../../game/GameFactory.java), [ToolkitGUI](../../../../ToolkitGUI.java) | Registration and selected PAR/mode integration |
| [GMF](../../../../GameModuleFramework/) | Existing reusable mathematics |
| [G001 implementation](../../../../game/G001-walkingwild/src/main/java/game/walkingwild/), [tests](../../../../game/G001-walkingwild/src/test/java/game/walkingwild/tests/WalkingWildGameTests.java) | Current workspace and PAR-backed example |
| [Expanding Wild](../../../../game/expandingwild/), [proxy](../../../../game/proxy/) | Other reel and probability references |
| [G001 lessons](../../../../docs/lessons/G001-LESSONS-LEARNED.md) | Units, state transitions, config and accounting regressions |

Keep sources/PAR under `game/GXXX-game-name/source/`, Java under `src/main/java/game/<package>/`, tests under `src/test/java/game/<package>/tests/`. Workspace codes are not Java package names. Preserve actual stable IDs and case; G001 uses `G001-walkingwild`.

Mode-specific Config/Reels pairs remain isolated. Default to Normal and reject missing, duplicate or incomplete pairs. Workbook reading is shared; required table schemas and typed config are game-owned.

Run `./build.sh` from the root. It discovers all Java files, including nested workspaces, into `build/classes`. Keep temporary Java callers outside the repo.

A round contains one Basegame result plus randomly triggered Feature Game results. The session uses the runner's RNG, handles feature state, and may stop generation when capped. Results describe outcomes; statistics interpret them incrementally. Purchase cost affects stake exposure, not implicit rescaling of paytable or cap units.

Test deterministic behavior before Monte Carlo. Identify current callers and regression suites before changing common math; preserve behavior where adding a focused component is more appropriate.
