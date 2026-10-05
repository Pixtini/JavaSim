# Architecture

## Overview

The project is a small Java Monte Carlo game simulation. `Main` reads the selected game ID from `SimConfig`, asks `GameFactory` to create that game with its own config, and passes it to `SimulationRunner`. It then passes the merged statistics to the reporting class.

The source is grouped into separate game and simulation layers. `Main.java` stays in the default package at the repository root as the launch point; it wires game configuration and an implementation into the simulation layer. The simulation engine depends on the game contract, while game rules stay inside game implementations.

## Source layout

- `game/`: game contracts (`Game`, `GameSession`), selection factory, shared game result types, and implementations.
- `game/model/`: spin and base-round results passed from game sessions to the simulation layer.
- `game/proxy/`: current probability-based example game and its `BasicProxyConfig`. Each future game should keep its own configuration with its implementation.
- `game/expandingwild/`: first reel-based game, with separate basegame, freegame, config, config adapter, and detailed spin results. Reel strips are configured as integer symbol-ID lists and translated to GMF `Symbol` values by a game-side adapter.
- `GameModuleFramework/`: reusable symbols, reel strips and grids, payline evaluation and win selection, paytable award metadata, scatter counting and trigger evaluation, wild expansion, additive multiplier helpers, and generic weighted-table draws.
- `simulation/`: generic Monte Carlo execution and output.
- `simulation/config/`: simulation-wide settings.
- `simulation/engine/`: orchestration, worker threads, seeded partitions, and aggregation.
- `simulation/result/`: completed simulation output returned by the engine.
- `simulation/stats/`: incremental statistics and win-distribution calculations.
- `simulation/reporting/`: console formatting and report file creation.
- `toolkit/viewer/`: command-line win finder and screen printer for interactive game inspection.
- `toolkit/reelset/`: exact Cartesian reel-stop enumerator and full-cycle aggregate results.
- `Main.java`: root-level application entry point.

## Configuration

### `game.proxy.BasicProxyConfig`

Holds settings used only by the probability-based proxy:

- Win amounts and their cumulative probability thresholds.
- The paytable buckets used by `StandardStats`.
- The number of free spins awarded by a freegame trigger.

### `simulation.config.SimConfig`

Holds simulation-wide values:

- `gameId`: selects which game `Main` asks `game.GameFactory` to construct. It defaults to `expanding-wild`; set it to `basic-proxy` to run the probability proxy. Each factory entry creates that game's own configuration and implementation.
- `rounds`: number of basegame rounds to simulate.
- `stake`: stake per basegame round.
- `seed`: stored seed used when `usePreviousSeed` is enabled; `simulation.engine.SimulationRunner` replaces it with a randomly generated seed otherwise.
- `usePreviousSeed`: selects between replaying the configured seed and generating a new seed for this run.
- `exportReport`: selects console-only detailed output or file-based detailed reporting.
- `showAwards`: controls whether basegame and freegame award counts appear in the regular console and text report.
- `threads`: maximum worker threads used to process partitions.
- `partitions`: fixed logical work partitions used for deterministic seeding and result merging.

## Simulation flow

`simulation.engine.SimulationRunner` resolves the run's effective seed from `SimConfig.usePreviousSeed`. It divides the configured rounds into a fixed number of logical partitions and derives one seed from the run seed and each partition ID. Each task owns its `Random`, `GameSession`, and three statistics collectors. The game implementation creates sessions from the supplied random stream, keeping random sequences deterministic per partition. Tasks do not update shared statistics.

After all tasks finish, the runner merges each partition's statistics in partition-ID order. Fixed partitions and ordered merging keep results reproducible when the same seed, settings, and partition count are used, even if the worker-thread count changes. The effective seed and logical partition count are printed at the end of the console output and `simulation_stats.txt`.

`Main` creates `SimConfig`, asks `GameFactory` for the selected game, invokes the runner, and passes the merged results to `Print`. The factory creates the selected implementation together with its game-specific config. Each partition runs its assigned basegame rounds through `GameSession.playRound(totalRoundStake)`. Games that use stake-scaled paytables receive the configured stake; the default interface implementation preserves games whose payouts do not depend on stake. The session returns a `GameRoundResult` containing the basegame result and all freegame spin results triggered by that basegame spin. The engine then:

1. Records the base spin in basegame statistics.
2. If the base result triggers free spins, records the trigger and each returned free spin in freegame totals, paytable buckets, and any named award categories returned with the spin.
3. Records one freegame distribution result equal to the sum of the free spins returned for that trigger.
4. Records one total-game distribution result equal to the basegame win plus any freegame wins for the round.

This means the three win distributions have different observation units: basegame entries are base rounds, freegame entries are triggered freegames, and total-game entries are base rounds. A round without a freegame has no freegame distribution entry. Freegame RTP uses one basegame stake exposure per base round, not one stake per free spin.

### `simulation.engine.SimulationRunner` and `simulation.result.SimulationResult`

`SimulationRunner` validates the worker settings, resolves the seed, submits fixed partitions to an executor, and merges partition-local statistics in partition order. `SimulationResult` carries the three merged statistics collectors back to `Main` for reporting.

### `game.GameFactory`

Maps the `SimConfig.gameId` selection to a game implementation and constructs it with its game-specific configuration. `Main` passes the resulting `Game` to the runner and does not import a game's config class. Register future game implementations here with their own configuration classes.

## Game and result classes

### `game.Game` and `game.GameSession`

`Game` provides the paytable bucket shape, optional named award labels, and creates a `GameSession` for a supplied random stream. Each `SpinResult` can carry zero or more award labels; statistics aggregate every occurrence, including multiple same-category line awards in one spin. Sessions can receive the total round stake so game-specific paytables can scale line awards from it. Each session plays one basegame round, including any randomly triggered freegame spins, and returns a `GameRoundResult`. The engine knows the shared round/result contract but does not construct specific game classes.

### `game.proxy.BasicProxyGame`

Implements `Game` using the simple probability-based proxy rules. Its session owns the proxy's basegame and freegame logic and returns their outcomes through the game contract.

### `game.proxy.GameBasic`, `GameBasicBase`, and `GameBasicFree`

These classes implement the proxy's individual spin logic. They are an example game implementation, not dependencies of the simulation engine.

### `game.expandingwild.ExpandingWildGame`

Implements the existing game contract with a 5x5 reel game. It keeps basegame and freegame logic separate, uses GMF for reel/grid and payline math, expands visible banners into full-height wild reels, and draws each freegame banner multiplier from a game-configured weighted table before adding participating multipliers on paylines. Its example config uses one shared 60-stop sequence across the five reels, with scatter stops only on reels 1, 3, and 5. See [ExpandingWildGame.md](../game/expandingwild/ExpandingWildGame.md) for strip symbol counts, paytable, paylines, tests, and known limits.

### `GameModuleFramework`

Provides small reusable components used by the reel game: `Symbol`, `ReelStrip`, `ReelGrid`, `WildExpansion`, `Payline`, `Paytable`, `PaytableAward`, `PaylineEvaluator`, and `PaylineWinCalculator`. The generic calculator stake-scales line candidates, accepts a candidate-specific multiplier function, and resolves candidate wins using a `LineWinSelectionPolicy` (Expanding Wild selects the highest final payout). GMF also provides `ScatterCounter`, `ScatterTrigger`, `AdditiveMultipliers`, and `probability.WeightedTable`. The game config owns its symbols, symbol-ID mapping, integer reel strips, paylines, paytable, and feature settings; a game adapter converts integer strips into GMF reels. Multiplier assignment and result adaptation remain in the game module.

### `toolkit.viewer`

`ViewerMain` accepts a registered game ID and optional spin limit and stake. `WinFinder` creates a random game session and searches basegame and triggered freegame results until it finds the first positive-win screen or reaches the limit. `WinScreenPrinter` renders Expanding Wild's expanded grid and line details; games that provide only shared `SpinResult` data display their win and named awards. Run it from the project root with `java toolkit.viewer.ViewerMain expanding-wild`.

`FullReelsetMain` runs every unique stop-index combination for a game implementing `ExhaustiveReelGame`. `FullReelsetSimulator` partitions the Cartesian index range across workers, evaluates only the basegame outcome, totals win-award hits and RTP, and counts feature triggers without executing those features. `ReelsetReportPrinter` presents award hits as a matrix with symbols in rows and match lengths in columns. `ReelStrip.windowAt` and `ReelGrid.atStops` provide the deterministic GMF stop-to-screen path. Both exhaustive and Monte Carlo runners use `toolkit.progress.ProgressBar` for large workloads; progress is written to standard error and small runs stay quiet. Run the exhaustive tool with `java toolkit.reelset.FullReelsetMain expanding-wild 1.0 8`.

### `game.model.SpinResult` and `game.model.GameRoundResult`

Immutable results for an individual spin and a complete basegame round. A round result contains one basegame spin and the list of freegame spins it triggered.

## Statistics

### `simulation.stats.StandardStats`

Accumulates spin counts, total winnings, generic paytable buckets, named award counts, freegame trigger counts, stake exposure, and a win distribution. It receives the stake, paytable shape, and award labels when constructed rather than creating game configuration objects itself.

`addResult` records one individual spin in regular totals, paytable buckets, and named award counts. `recordWinInDistribution` separately records the value that represents one observation in a distribution. This separation lets freegame totals count each free spin while its distribution counts one sum per triggered freegame. Freegame RTP divides its winnings by basegame stake exposure recorded by the runner.

The win distributions map each win amount to a `long` frequency. Standard deviation is computed from the distribution. Aggregated win-band rows are also calculated from the distribution using the configured ranges: zero, then `(0, 1]`, `(1, 2]`, `(2, 3]`, `(3, 5]`, and successive bands through `(25,000, 10,000,000]`.

Each aggregate band reports:

- **Hits:** observations in that band.
- **% Of Winnings:** that band's total win divided by the distribution's total win.
- **% Of Hits:** band hits divided by observations in that distribution.
- **Frequency:** observations divided by band hits; zero when the band has no hits.
- **RTP:** band total win divided by total simulation stake.
- **Total Win:** sum of all wins in the band.

The percentage of hits and frequency use each distribution's own observation count. RTP uses total basegame stake for all three distributions so the rows show each category's contribution to overall simulation RTP.

## Reporting

### `simulation.reporting.Print`

Formats the regular summary and detailed distributions. It prints named award counts when a game supplies award labels, falling back to generic paytable buckets for games such as the probability proxy. The `showAwards` setting controls whether these award lines appear in console and text output. Total-game standard deviation is calculated from the total-game distribution. It receives the same `SimConfig` instance used by `Main`, so the report uses the simulation's rounds, stake, output mode, and timing from `SimulationResult`.

When `exportReport` is `false`, the console receives only the regular summary statistics and run settings, including the selected game ID. No report files are created.

When `exportReport` is `true`, the console receives the regular summary statistics, a path to the generated report folder, and the run settings at the bottom, including the selected game ID. Detailed files are written under `reports/` in a timestamped `simulation-YYYY-MM-DD_HH-mm-ss` folder. A numeric suffix is added if another report already uses that timestamp. Each folder contains:

- `simulation_stats.txt`: the regular summary printed to the console, followed by selected game ID, logical partition count, effective seed, and total simulation time.
- `win_distributions.csv`: raw win amounts and hit counts.
- `win_distribution_aggregated.csv`: aggregated win-band values.

Both CSVs group their sections in this order, with a blank row between sections: **Total Game, Basegame, Freegame**. The generated `reports/` directory and compiled `.class` files are ignored by Git.

## Tests

`tests/SimulationTests.java` covers generic statistics, deterministic merging, game selection, and reports. `game/expandingwild/tests/ExpandingWildGameTests.java` covers GMF mechanics, game rules, and a seeded simulator run. Run both suites with:

```sh
javac *.java tests/*.java game/expandingwild/tests/*.java
java -cp .:tests SimulationTests
java game.expandingwild.tests.ExpandingWildGameTests
```

From the repository root, the normal launch flow is:

```sh
javac *.java
java Main
```

`javac *.java` compiles `Main.java` and discovers the source dependencies in both layers. To compile every source explicitly, including tests, use `javac $(find . -name '*.java')`.

## Current scope

The code is an example simulation framework rather than a general-purpose engine. `simulation.engine.SimulationRunner` handles orchestration and parallel execution through the `game.Game` / `game.GameSession` contract. Both `game.proxy` and `game.expandingwild` implement that contract. The initial `GameModuleFramework` package contains reusable reel and line-win math; new generic mechanics should be added only when another game can use them. See [nextsteps.md](../nextsteps.md) for the proposed order of improvements.
