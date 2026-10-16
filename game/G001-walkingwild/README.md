# New Game Workspace: walkingWild

**Game code:** `G001-walkingwild`
**Workspace folder:** `G001-walkingwild/`  
**Java package:** `game.walkingwild` (`src/main/java/game/walkingwild/`)

This is the complete G001 game workspace. It keeps source material and PAR under `source/`, Java implementation under `src/main/java/`, and game-specific tests under `src/test/java/`. The package is `game.walkingwild`; the `G001-walkingwild` folder is the human-facing game ID, while the Java package uses a valid Java identifier.

From the repository root, launch the toolkit with `./run.sh`. It compiles all game workspaces into the shared `build/classes` output before starting the GUI, so nested G001 classes are available at runtime.

## Step 0: prepare and review the source material

1. Put the approved GDD, edge cases, flowchart, and PAR values in the matching files under `source/`. Keep source versions and dates in each document.
2. Complete `source/game-creation-form.md` alongside the GDD. Confirm stake units, evaluation rules, feature triggers/retriggers, win cap, and set behavior.
3. Review `source/G001-walkingWildsPAR.xlsx` as this game's PAR input. It contains separate Normal and BB1 Config/Reels pairs. Keep each mode's tables and reel strips isolated; a simulation selects one mode for the whole run. A mode-prefixed sheet pair follows `<Mode>-Config` and `<Mode>-Reels`; an unprefixed `Config`/`Reels` pair is interpreted as Normal.
4. Ask the agent to review the source package before writing game logic. The agent records conflicts, missing config, open questions, and proposed questions in `AGENT-REVIEW.md`.
5. Resolve the review items with the game owner. Do not invent balance values or silently choose between conflicting sources.

## After the review is agreed

The implementation reads its config from the selected mode in `source/G001-walkingWildsPAR.xlsx`; do not substitute hard-coded defaults for PAR values. Select `G001-walkingwild`, its workbook, and Normal or BB1 in the Simulation screen, Game launcher, or Win viewer. The lower-level factory API also accepts `(gameId, parWorkbookPath, mode)` for scripted runs.

The Game launcher opens a G001 player with animated 6×4 screens, line highlights, the scatter/Freegame sequence, auto play, mode-cost balance accounting, and last-round playback. The Win viewer prints the grid and line awards and can open the found round in the player. Reports save G001 reel stops, scatter insertions, and Freegame wild destinations; Replay reconstructs the screens and validates the recalculated wins. The Full reelset tool evaluates Basegame reel stops and paylines; it omits random scatter insertions and triggered Freegames. The implemented rules and integrations have deterministic coverage in `src/test/java/game/walkingwild/tests/WalkingWildGameTests.java`. Both modes were simulated with seeded runs; reports are under `reports/G001-walkingwild/`. Check RTP by mode and set, triggers, hit rate, distributions, caps, and report assets before balance approval. Framework architecture and G002 workflow lessons are summarized in root `G001-LESSONS-LEARNED.md`.

## Workspace files

- `source/GDD.md`: game rules and approved math.
- `source/edge-cases.md`: required boundary behavior.
- `source/flowchart.md`: round and feature flow.
- `source/G001-walkingWildsPAR.xlsx`: mode-specific PAR input with Normal and BB1 Config/Reels sheets.
- `source/game-creation-form.md`: implementation intake checklist.
- `AGENT-REVIEW.md`: agent-authored questions, gaps, and proposals.
- `src/main/java/game/walkingwild/`: G001 implementation and config parser.
- `src/test/java/game/walkingwild/tests/`: deterministic G001 tests.
