# Expanding Wild Game

## Summary

This is the first reel-based game module and a test of the `GameModuleFramework` (GMF) foundation. The module is registered as `expanding-wild` and is now the default selected game; set `simulation.config.SimConfig.gameId` to `"basic-proxy"` to run the probability proxy.

The implementation uses a 5-reel by 5-row window, ten regular symbols (`T1`–`T5` and `L1`–`L5`), 15 left-to-right paylines, an expanding banner wild, and a basegame scatter trigger. `ExpandingWildConfig` separates shared game rules from `baseGame` and `freeGame` modes. Each mode has two independent spin sets and a weighted selector; the default selector is 3:1 for set 0 to set 1. Reel strips use integer IDs for spreadsheet editing: 0–4 map to `T1`–`T5`, 5–9 map to `L1`–`L5`, 10 is `BANNER`, 11 is `WILD`, and 12 is `SCATTER`. `ExpandingWildConfigAdapter` translates each set's strips into GMF reel strips. The configuration is intentionally small and is not balanced.

Basegame set 0 uses five 60-stop reels with a scatter only on reels 1, 3, and 5, and has no banners. Basegame set 1 has one banner per reel and no scatters. Freegame set 0 has no wilds; set 1 has one banner per reel. The no-scatter freegame strips explicitly retain 60 stops per reel, with the removed scatter stop represented by an additional T1. No static `WILD` symbol is used because banners expand after the reels stop.

| Symbol | Reels 1, 3, 5 | Reels 2, 4 |
| --- | ---: | ---: |
| T1 | 6 | 5 |
| T2–T5 | 6 each | 6 each |
| L1 | 5 | 6 |
| L2–L5 | 6 each | 6 each |
| BANNER | 0 (set 0), 1 (set 1) | 0 (set 0), 1 (set 1) |
| SCATTER | 1 (set 0), 0 (set 1) | 0 |
| **Stops** | **60** | **60** |

## Rules implemented

- A spin independently stops one configured circular strip for each of the five reels. The five symbols beginning at each stop form the visible reel.
- A banner anywhere in a stopped reel expands that reel to five wilds after all reels stop. Each reel in set 1 has one banner; set 0 has none.
- Paylines are read left to right. A win must start on reel one and continue without a mismatch. Only configured match lengths pay. GMF's `PaylineWinCalculator` applies total-round stake and candidate multipliers, then its configured selection policy awards the highest final payout once per line, including wild substitutions. The game adapts selected GMF wins into `ExpandingWildLineWin` values.
- The 15 line paths below are zero-based visible row indexes, one row per reel:

  ```text
  2-2-2-2-2   0-0-0-0-0   4-4-4-4-4
  1-1-1-1-1   3-3-3-3-3   0-1-2-3-4
  4-3-2-1-0   1-0-1-2-1   3-4-3-2-3
  2-1-0-1-2   2-3-4-3-2   0-0-1-2-3
  4-4-3-2-1   1-2-3-2-1   3-2-1-2-3
  ```

- Scatters are present only on reels 1, 3, and 5. GMF's `ScatterTrigger` counts the configured symbol on those reels; meeting the configured threshold awards the configured number of free spins (five by default). Scatter count is evaluated on the stopped grid before expansion. The scatter and banner stops are far enough apart on the circular reel that a visible banner cannot cover a visible scatter.
- Every basegame and freegame spin independently draws a set index from that mode's weighted selector (default set 0:set 1 is 3:1), then selects stops from that set's strips. Basegame set 0 can trigger freegames; set 1 cannot. Freegame sets contain no scatters and cannot retrigger. Each visible banner in freegame set 1 independently draws a multiplier from that set's `bannerMultiplierWeights` using GMF's `WeightedTable`. Each row defines a multiplier and its relative weight; the current config gives 2x and 3x a weight of 1000 each and 4x–10x a weight of 1 each. Basegame set multipliers are stored separately.
- Multipliers on a winning line add together. For example, 2x and 3x produce a 5x line multiplier. A banner only contributes when it is part of that line's consecutive winning prefix. Basegame banner wilds use 1x.
- A complete basegame round is capped at `maxWinMultiplier` times total round stake (currently 100x). Basegame win consumes the cap first; each free spin can win only the amount remaining. The spin that reaches the cap is clipped to the remaining amount, and later free spins are not generated. TotalGame statistics report how many rounds reached the cap.

The chosen free-spin award and line paths are example values because the request did not specify them. Both live in `ExpandingWildConfig` and can be changed there.

## Example paytable

Each value is a multiplier of the total basegame round stake, paid for each winning line before any freegame banner multiplier. For example, T1 paying 2.0 for three of a kind awards `2.0 × total round stake` on that line. The values are deliberately simple and are not balanced.

| Symbol | 3 of a kind | 4 of a kind | 5 of a kind |
| --- | ---: | ---: | ---: |
| T1 | 2 | 5 | 12 |
| T2 | 2 | 6 | 15 |
| T3 | 2.5 | 7 | 18 |
| T4 | 3 | 8 | 20 |
| T5 | 3 | 9 | 24 |
| L1 | 3.5 | 10 | 28 |
| L2 | 4 | 12 | 32 |
| L3 | 4 | 14 | 36 |
| L4 | 5 | 16 | 40 |
| L5 | 5 | 18 | 45 |

## Results

`ExpandingWildSpinResult` extends the shared `SpinResult` with the stopped and expanded grids, scatter count, expanded banner reels, freegame mode, and selected payline wins. Each line win retains its symbol, match length, all visible symbols on the payline, stake-scaled win before banner multipliers, applied multiplier, and final win. It also exposes one named award label per winning line for generic statistics. `GameRoundResult` returns the base result, free spin results, and whether the round reached its configured win cap. The runner passes the same total basegame round stake to the base spin and all triggered free spins.

Each spin result carries one named award label per winning payline, such as `T1 3oak`. The shared statistics collector aggregates these categories independently for basegame and freegame spins; each hit is a paid line, so one spin can add multiple award hits. The generic loss/win bucket remains available for hit-rate calculations. The runner still discards other detailed game data after aggregating statistics.

## Verification and observed behavior

The game tests cover reel-window wrapping, unbroken paylines, paytable lengths, banner expansion, additive multipliers, scatter eligibility, no freegame retriggers, configured free-spin count, game-factory selection, and seeded agreement across worker-thread counts.

For replay, each expanding-wild spin records its mode, selected set, zero-based reel stops, and resolved banner multiplier assignments in a versioned JSON payload. Replaying injects these decisions and recalculates the result; multiplier weights are not redrawn. The replay tool compares basegame, each freegame spin, and total wins with the values saved in the CSV.

The full reelset tool can enumerate every basegame stop combination with `java toolkit.reelset.FullReelsetMain expanding-wild 1.0 8`. The two five-reel sets have 60 stops on every reel, so the tool evaluates 1,555,200,000 unique set-and-stop outcomes. Its weighted totals apply the 3:1 selector; it evaluates banner expansion and basegame line wins and counts scatter triggers without playing freegames. Weighted full-reelset evaluation requires selectable sets to have the same total number of stop combinations. Award hits print as a table with symbols down the rows and matching lengths across the columns.

A seeded 1,000,000-round simulation used seed `4042026`, 32 partitions, and four threads. A separate one-thread run with the same seed and partition count produced matching total and freegame distributions:

- Basegame scatter triggers: 30,046 (3.0046%).
- Free spins: 240,368 (eight per trigger).
- Basegame winnings: 38,430,848.50.
- Freegame winnings: 195,640,722.00.
- Total winnings: 234,071,570.50; reported overall RTP against a stake of 1 per base round is 23,407.16%.

The very high RTP is expected from the deliberately unbalanced example: each winning line applies its paytable multiplier to the full round stake, without splitting the stake across the 15 paylines, and freegame banner multipliers can add to as much as 50x on a line with five banners. It is a math/configuration result, not a simulator crash or aggregation mismatch.

The run exposed reporting issues in existing generic simulation code:

The console and report now calculate freegame RTP against the total basegame stake exposure, and total-game standard deviation uses the total-game distribution. The Awards lines show the 30 per-symbol 3/4/5-of-a-kind categories and count each paid line.

## Suggested simulator work

The simulator aggregates named awards but does not retain full game-specific outcomes for export. An optional detailed report could include stopped grids, expanded reels, banner multipliers, scatter counts, and selected winning paylines.
