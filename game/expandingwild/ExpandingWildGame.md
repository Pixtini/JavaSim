# Expanding Wild Game

## Summary

This is the first reel-based game module and a test of the `GameModuleFramework` (GMF) foundation. The module is registered as `expanding-wild` and is now the default selected game; set `simulation.config.SimConfig.gameId` to `"basic-proxy"` to run the probability proxy.

The implementation uses a 5-reel by 5-row window, ten regular symbols (`T1`–`T5` and `L1`–`L5`), configurable left-to-right paylines, expanding banner wilds, and a scatter freegame trigger. `ExpandingWildConfig` separates shared game rules from `baseGame` and `freeGame` modes. The selected PAR workbook is parsed by `ExpandingWildParParser`; the `Reels` and `Config` worksheets supply reel strips, game-wide values, paylines, paytable, set selectors, insertion count tables, placement heat maps, multiplier tables, and the scatter award map. `ExcelWorkbookReader` handles the workbook's Open XML data without a third-party dependency. Reel strips use symbol IDs: 0–9 are the regular symbols, 10 is `BANNER`, 11 is `WILD`, and 12 is `SCATTER`. `ExpandingWildConfigAdapter` translates the parsed strips into GMF reel strips.

The selected workbook currently defines four 206/166/172/134/124-stop reel sets (the stop count may differ by reel). Special symbols are inserted after reel stops according to separate per-mode, per-set count tables and heat maps. `wildExpand` tables configure banner insertion; `scatter` tables configure scatter insertion. The heat map's `reelMax` is enforced per column, and protected symbols are not replaced. The current freegame scatter tables give positive weight only to zero inserted scatters; the game does not support freegame retriggers, and the parser rejects a PAR that enables them. No static `WILD` symbol is used because banners expand after the reels stop.

| Strip symbols | Count per reel |
| --- | ---: |
| T1–T5 | 6 each |
| L1–L5 | 6 each |
| **Stops** | **60** |

## Rules implemented

- A spin independently stops one configured circular strip for each of the five reels. The five symbols beginning at each stop form the visible reel.
- After a normal reel stop, the selected set's symbol insertion rules draw a count and place symbols on eligible visible cells using the configured heat map. Zero-weight positions are excluded, forbidden replacement symbols are preserved, and per-reel limits are enforced. Scatter insertion is limited to reels 1, 3, and 5; banner insertion can use any reel. The inserted grid then continues through feature checks and win evaluation.
- A banner anywhere in the inserted grid expands that reel to five wilds. Expansion occurs after the reel stop and insertion stages.
- Paylines are read left to right. A win must start on reel one and continue without a mismatch. Only configured match lengths pay. GMF's `PaylineWinCalculator` applies total-round stake and candidate multipliers, then its configured selection policy awards the highest final payout once per line, including wild substitutions. The game adapts selected GMF wins into `ExpandingWildLineWin` values.
- The 15 line paths below are zero-based visible row indexes, one row per reel:

  ```text
  2-2-2-2-2   0-0-0-0-0   4-4-4-4-4
  1-1-1-1-1   3-3-3-3-3   0-1-2-3-4
  4-3-2-1-0   1-0-1-2-1   3-4-3-2-3
  2-1-0-1-2   2-3-4-3-2   0-0-1-2-3
  4-4-3-2-1   1-2-3-2-1   3-2-1-2-3
  ```

- Scatters can be configured independently in each set. The basegame trigger counts scatters on any reel enabled by its placement heat maps. The current PAR maps three scatters to five free spins. Freegame scatter insertion is currently weighted to zero; freegame retriggers are unsupported.
- Every basegame and freegame spin independently draws a set index from that mode's weighted selector (currently set 0:set 1 is 3:1), then selects stops from that set's strips. Each visible banner draws from the selected set's `bannerMultiplierWeights` when configured; basegame banners without a multiplier table use 1×.
- Multipliers on a winning line add together. For example, 2x and 3x produce a 5x line multiplier. A banner only contributes when it is part of that line's consecutive winning prefix. Basegame banner wilds use 1x.
- A complete basegame round is capped at `maxWinMultiplier` times total round stake (currently 100x). Basegame win consumes the cap first; each free spin can win only the amount remaining. The spin that reaches the cap is clipped to the remaining amount, and later free spins are not generated. TotalGame statistics report how many rounds reached the cap.

The chosen free-spin award and line paths are example values because the request did not specify them. Both live in `ExpandingWildConfig` and can be changed there.

## Current PAR paytable

Payouts are multipliers of the total round stake, applied per winning line before banner multipliers. The workbook is the source of truth for the values used in simulation.

| Symbol | 3 of a kind | 4 of a kind | 5 of a kind |
| --- | ---: | ---: | ---: |
| T1 | 5 | 10 | 20 |
| T2 | 2 | 3 | 5 |
| T3 | 1 | 2 | 3 |
| T4 | 0.5 | 1 | 2 |
| T5 | 0.2 | 0.5 | 1 |
| L1–L5 | 0.1 | 0.2 | 0.5 |

## Results

`ExpandingWildSpinResult` extends the shared `SpinResult` with the stopped and expanded grids, scatter count, expanded banner reels, freegame mode, and selected payline wins. Each line win retains its symbol, match length, all visible symbols on the payline, stake-scaled win before banner multipliers, applied multiplier, and final win. It also exposes one named award label per winning line for generic statistics. `GameRoundResult` returns the base result, free spin results, and whether the round reached its configured win cap. The runner passes the same total basegame round stake to the base spin and all triggered free spins.

Each spin result carries one named award label per winning payline, such as `T1 3oak`. The shared statistics collector aggregates these categories independently for basegame and freegame spins; each hit is a paid line, so one spin can add multiple award hits. The generic loss/win bucket remains available for hit-rate calculations. The runner still discards other detailed game data after aggregating statistics.

## Verification and observed behavior

The game tests cover reel-window wrapping, unbroken paylines, paytable lengths, banner expansion, additive multipliers, scatter eligibility, no freegame retriggers, configured free-spin count, game-factory selection, and seeded agreement across worker-thread counts.

For replay, each expanding-wild spin records its mode, selected set, zero-based reel stops, inserted symbol locations, and resolved banner multiplier assignments in a versioned JSON payload. Replaying injects these decisions and recalculates the result; insertion heat-map and multiplier weights are not redrawn. The replay tool compares basegame, each freegame spin, and total wins with the values saved in the CSV.

The full reelset tool evaluates every configured basegame reel-stop combination using the visible regular symbols and paytable only. It deliberately skips symbol insertions, scatter triggers, banner expansion, and multipliers so it measures the underlying reel/paytable cycle without random feature mechanics. It still reports stop-combination counts and symbol award hits; its results are not the full game's feature-inclusive RTP.

A seeded 1,000,000-round simulation using `expandingWildPAR.xlsx` used seed `4042026`, 32 partitions, and four threads. A separate one-thread run with the same seed and partition count produced matching total and freegame distributions:

- Basegame scatter triggers: 119,926 (11.9926%).
- Free spins played: 519,790 (some rounds stop early at the win cap).
- Basegame winnings: 3,823,369.70.
- Freegame winnings: 5,474,676.00.
- Total winnings: 9,298,045.70; reported overall RTP against a stake of 1 per base round is 929.80%.

This run is a parser and execution check, not a target PAR validation. The observed RTP is 929.80%; each winning line applies the workbook payout multiplier to the total round stake, which is the intended pay-per-line convention. The round win is capped at 100×. Compare the result with the target RTP before treating the current workbook as a validated production PAR.

The run exposed reporting issues in existing generic simulation code:

The console and report now calculate freegame RTP against the total basegame stake exposure, and total-game standard deviation uses the total-game distribution. The Awards lines show the 30 per-symbol 3/4/5-of-a-kind categories and count each paid line.

## Suggested simulator work

The simulator aggregates named awards but does not retain full game-specific outcomes for export. An optional detailed report could include stopped grids, expanded reels, banner multipliers, scatter counts, and selected winning paylines.
