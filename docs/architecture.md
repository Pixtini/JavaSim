# Architecture

## Overview

The project is a small Java Monte Carlo game simulation. `Main` wires configuration, invokes `SimulationRunner` to execute configured rounds, then passes merged basegame, freegame, and total-game statistics to the reporting class.

The current implementation separates game configuration, simulation configuration, game logic, spin results, statistics, and reporting. The game and statistics classes are in the default package; configuration classes are in `config`.

## Configuration

### `config.GameConfig`

Holds game-specific values used by the example game:

- Win amounts and their cumulative probability thresholds.
- The paytable buckets used by `StandardStats`.
- The number of free spins awarded by a freegame trigger.

### `config.SimConfig`

Holds simulation-wide values:

- `rounds`: number of basegame rounds to simulate.
- `stake`: stake per basegame round.
- `seed`: stored seed used when `usePreviousSeed` is enabled; `SimulationRunner` replaces it with a randomly generated seed otherwise.
- `usePreviousSeed`: selects between replaying the configured seed and generating a new seed for this run.
- `exportReport`: selects console-only detailed output or file-based detailed reporting.
- `threads`: maximum worker threads used to process partitions.
- `partitions`: fixed logical work partitions used for deterministic seeding and result merging.

## Simulation flow

`SimulationRunner` resolves the run's effective seed from `SimConfig.usePreviousSeed`. It divides the configured rounds into a fixed number of logical partitions and derives one seed from the run seed and each partition ID. Each task owns its `Random`, game instances, and three statistics collectors. Tasks share the read-only `GameConfig` but do not update shared statistics.

After all tasks finish, the runner merges each partition's statistics in partition-ID order. Fixed partitions and ordered merging keep results reproducible when the same seed, settings, and partition count are used, even if the worker-thread count changes. The effective seed and logical partition count are printed at the end of the console output and `simulation_stats.txt`.

`Main` creates configuration, invokes the runner, and passes the merged results to `Print`. Each partition runs its assigned basegame rounds and:

1. Runs `GameBasicBase.spin()` and records that base spin in basegame statistics.
2. If the result triggers free spins, runs the configured number of free spins and records each individual free spin in freegame totals and paytable statistics.
3. Records one freegame distribution result equal to the sum of the free spins awarded by that trigger.
4. Records one total-game distribution result equal to the basegame win plus any freegame wins for the round.

This means the three win distributions have different observation units: basegame entries are base rounds, freegame entries are triggered freegames, and total-game entries are base rounds. A round without a freegame has no freegame distribution entry.

### `SimulationRunner` and `SimulationResult`

`SimulationRunner` validates the worker settings, resolves the seed, submits fixed partitions to an executor, and merges partition-local statistics in partition order. `SimulationResult` carries the three merged statistics collectors back to `Main` for reporting.

## Game and result classes

### `GameBasic`

Generates a spin win using `GameConfig` thresholds and amounts. It returns an immutable `SpinResult`.

### `GameBasicBase`

Uses `GameBasic` for the base spin and sets the free-spin flag based on the basegame roll.

### `GameBasicFree`

Uses the common spin logic in `GameBasic` for each free spin.

### `SpinResult`

An immutable value for one spin. It contains the win amount, the paytable win-size index, and whether the spin triggers free spins.

## Statistics

### `StandardStats`

Accumulates spin counts, total winnings, paytable awards, freegame trigger counts, and a win distribution. It receives the stake and paytable when constructed rather than creating configuration objects itself.

`addResult` records one individual spin in the regular totals and paytable. `recordWinInDistribution` separately records the value that represents one observation in a distribution. This separation lets freegame totals count each free spin while its distribution counts one sum per triggered freegame.

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

### `Print`

Formats the regular summary and detailed distributions. It receives the same `SimConfig` instance used by `Main`, so the report uses the simulation's rounds, stake, output mode, and timing from `SimulationResult`.

When `exportReport` is `false`, the console receives only the regular summary statistics and run settings. No report files are created.

When `exportReport` is `true`, the console receives the regular summary statistics, a path to the generated report folder, and the run settings at the bottom. Detailed files are written under `reports/` in a timestamped `simulation-YYYY-MM-DD_HH-mm-ss` folder. A numeric suffix is added if another report already uses that timestamp. Each folder contains:

- `simulation_stats.txt`: the regular summary printed to the console, followed by logical partition count, effective seed, and total simulation time.
- `win_distributions.csv`: raw win amounts and hit counts.
- `win_distribution_aggregated.csv`: aggregated win-band values.

Both CSVs group their sections in this order, with a blank row between sections: **Total Game, Basegame, Freegame**. The generated `reports/` directory and compiled `.class` files are ignored by Git.

## Tests

`tests/SimulationTests.java` is a dependency-free test harness for statistics, win-band boundaries, deterministic merging across worker counts, seed reporting, console output, and generated report files. Run it with:

```sh
javac $(rg --files -g '*.java')
java -cp .:tests SimulationTests
```

## Current scope

The code is an example simulation framework rather than a general-purpose engine. `SimulationRunner` handles orchestration and parallel execution, while the game logic, configuration, statistics, and output remain separate. The current `GameBasic` classes are a simple probability proxy; future game implementations can add more detailed slot rules. See [nextsteps.md](../nextsteps.md) for the proposed order of improvements.
