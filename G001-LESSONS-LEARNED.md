# G001 end-to-end lessons and improvements for G002

## What the G001 workflow covered

G001 started as a wizard-created intake folder. The source package gathered the GDD, edge cases, flowchart, game creation form, shared line-game rules, and a PAR workbook with separate Normal and BB1 Config/Reels pairs. Reviewing these together surfaced decisions that had to be resolved before implementation: the mode's cost versus payout unit, per-line win selection, scatter awards, the freegame wild relocation sequence, temporary exclusion of occupied cells, and the five-wild maximum.

The implementation kept the workbook as the configuration source. `WalkingWildParParser` reads one selected mode and rejects unsupported or inconsistent input. The game module separates config parsing, Basegame, Freegame, result evaluation, and round/session flow. It uses the existing GMF reel, weighted table, insertion, and payline components. Tests cover mode parsing, payline selection, wild exclusions, persistence, determinism, and generated report contents. Seeded simulations were then run for both modes and their reports retained the selected PAR pair.

## Lessons from implementing G001

- **Resolve units before writing engine code.** BB1 costs 100 base stakes, while awards and the maximum-win cap are defined in base-stake units. Keeping those separate avoids incorrectly multiplying payouts or the cap by the purchase cost.
- **Describe state transitions concretely.** “Wilds persist” was not enough: the order of reel stops, new wild placement, old wild relocation, and collision exclusions determined the implementation.
- **Make exclusion behavior non-mutating.** A per-placement copy of the PAR heat map preserves its source values and makes temporary zero-weight exclusions auditable.
- **Use the PAR as configuration, but validate its shape.** Parsing should reject absent tables, invalid symbols, mismatched mode sheets, and impossible freegame quantities before the simulation starts.
- **Add reporting checks alongside game-rule tests.** The initial integration test found that TotalGame RTP was `NaN` because the simulation runner recorded its distribution but did not accumulate a total-game result and stake basis. The runner now records one round-level total result per base round.
- **Treat report artifacts as an implementation deliverable.** A successful run alone does not demonstrate which PAR mode was used. The report needs the selected Config/Reels sheets, mode identity, cost, seed, and summary statistics.
- **Keep game-owned files together while using valid Java packages.** The `G001-walkingwild` workspace contains source material, PAR, implementation, and tests. Java sources stay under `src/main/java/game/walkingwild/` in package `game.walkingwild`; the project folder can retain its human-facing G-code name.

## Improvements to make for G002

1. **Strengthen the intake checklist before coding.** Require source owner/version/date, authoritative-source ranking, screen coordinate conventions, symbol IDs and roles, stake and payout units, rounding, cap timing, and feature state transitions.
2. **Add a PAR schema/preview validator to the wizard.** It should list discovered modes and tables, show missing/extra fields, check row/column dimensions and referential integrity, and generate a review summary before engine implementation.
3. **Separate generic PAR infrastructure from game-specific schema.** Keep workbook reading, sheet-mode selection, validation errors, selected-sheet export, and HTML rendering reusable; give each game a small typed parser with explicit required table definitions.
4. **Standardize deterministic math test fixtures.** Add reusable GMF test helpers for fixed reel stops, weighted selections, wild placement, and exact expected result structures. Each rule from the GDD should map to a test before running Monte Carlo.
5. **Add a round-accounting invariant suite.** Check total winnings equal Basegame plus Freegame winnings, each trigger produces the expected feature events, stake exposure uses mode cost, and caps never exceed configured units. This would have caught the TotalGame stats gap earlier.
6. **Create reports from a named run profile.** Store the selected mode, source workbook hash, game version, seed, partition count, stake basis, and run size with report artifacts so a result can be reproduced even after the PAR changes.
7. **Make validation staged and reproducible.** Run parser and deterministic tests, then a small seed-locked simulation, then a larger mode-specific report. Record exact commands and report paths in the game's handover.
8. **Finish intake metadata during review.** G001's source-template fields and some broad edge cases remained incomplete. G002 should not enter implementation until source status and any game-specific exceptions are approved or explicitly tracked as assumptions.

## Framework changes prompted by G001

- `Game.getCostMultiplier()` defaults to 1 and allows a game mode to expose purchase cost separately from payout units.
- `SimConfig` and `SimulationRunner` carry that multiplier into stake exposure and report metadata.
- Total-game statistics now accumulate one round-level result and stake contribution per simulated round, as well as the round win distribution.
- The player adapter contract now exposes mode/cost metadata and adapts a recorded `GameRoundResult` for Win Viewer handoff. G001 provides detailed spin results with its grid, line awards, scatter count, and set ID.
- G001 replay events store each selected set, six reel stops, scatter insertion positions, and Freegame new/moved wild destinations. Replay reconstructs each 6×4 screen from those choices and recalculates wins without drawing fresh random outcomes.
- The prototype player tab is now a game launcher for Expanding Wild and G001. The viewer selects a workbook/mode and can open either game's found result in its corresponding player adapter.
- G001 exposes the existing exhaustive reel-stop capability for Basegame strips and paylines. That report omits randomly inserted scatters and triggered Freegames, so it is a base-reel evaluation rather than a complete game RTP result.

These are shared capabilities justified by G001's BB1 cost, report validation gap, and need to reuse player/viewer presentation. The G001 rules and replay payload remain game-specific.
