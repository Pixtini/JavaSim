# Expanding Wild Game

## Summary

This is the first reel-based game module and a test of the `GameModuleFramework` (GMF) foundation. The module is registered as `expanding-wild` and is now the default selected game; set `simulation.config.SimConfig.gameId` to `"basic-proxy"` to run the probability proxy.

The implementation uses a 5-reel by 5-row window, ten regular symbols (`T1`–`T5` and `L1`–`L5`), 15 left-to-right paylines, an expanding banner wild, and a basegame scatter trigger. `ExpandingWildConfig` contains the symbol definitions and mapping as well as all game settings. Reel strips are integer ID lists, so they can be pasted from spreadsheet output: 0–4 map to `T1`–`T5`, 5–9 map to `L1`–`L5`, 10 is `BANNER`, 11 is `WILD`, and 12 is `SCATTER`. `ExpandingWildConfigAdapter` translates these compact strips into GMF reel strips. The configuration is intentionally small and is not balanced.

All five base reels use the same 60-stop sequence, with the scatter stop present only on reels 1, 3, and 5. The scatter-free reels replace that stop with `L1` to preserve the shared strip layout. Each reel has one banner; no static `WILD` symbol is used because banners expand into wilds after the reels stop.

| Symbol | Reels 1, 3, 5 | Reels 2, 4 |
| --- | ---: | ---: |
| T1 | 5 | 5 |
| T2–T5 | 6 each | 6 each |
| L1 | 5 | 6 |
| L2–L5 | 6 each | 6 each |
| BANNER | 1 | 1 |
| SCATTER | 1 | 0 |
| **Stops** | **60** | **60** |

## Rules implemented

- A spin independently stops one configured circular strip for each of the five reels. The five symbols beginning at each stop form the visible reel.
- A banner anywhere in a stopped reel expands that reel to five wilds after all reels stop. Each default reel strip has one banner.
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
- Free spins use the same reel strips with scatters removed, so they cannot retrigger. Each visible banner expands and independently draws a multiplier from `ExpandingWildConfig.bannerMultiplierWeights` using GMF's `WeightedTable`. Each row defines a multiplier and its relative weight; the current config gives 2x and 3x a weight of 1000 each and 4x–10x a weight of 1 each.
- Multipliers on a winning line add together. For example, 2x and 3x produce a 5x line multiplier. A banner only contributes when it is part of that line's consecutive winning prefix. Basegame banner wilds use 1x.

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

`ExpandingWildSpinResult` extends the shared `SpinResult` with the stopped and expanded grids, scatter count, expanded banner reels, freegame mode, and selected payline wins. Each line win retains its symbol, match length, all visible symbols on the payline, stake-scaled win before banner multipliers, applied multiplier, and final win. It also exposes one named award label per winning line for generic statistics. `GameRoundResult` returns the base result and each free spin result through the existing game contract. The runner passes the same total basegame round stake to the base spin and all triggered free spins.

Each spin result carries one named award label per winning payline, such as `T1 3oak`. The shared statistics collector aggregates these categories independently for basegame and freegame spins; each hit is a paid line, so one spin can add multiple award hits. The generic loss/win bucket remains available for hit-rate calculations. The runner still discards other detailed game data after aggregating statistics.

## Verification and observed behavior

The game tests cover reel-window wrapping, unbroken paylines, paytable lengths, banner expansion, additive multipliers, scatter eligibility, no freegame retriggers, configured free-spin count, game-factory selection, and seeded agreement across worker-thread counts.

The full reelset tool can enumerate every basegame stop combination with `java toolkit.reelset.FullReelsetMain expanding-wild 1.0 8`. With five 60-stop reels, that is 777,600,000 unique outcomes. The tool evaluates banner expansion and basegame line wins for each outcome, records line-award hit counts and total RTP, and counts basegame scatter triggers; it does not play the triggered freegames. Award hits print as a table with symbols down the rows and matching lengths across the columns.

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
