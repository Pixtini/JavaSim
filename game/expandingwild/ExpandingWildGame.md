# Expanding Wild Game

## Summary

This is the first reel-based game module and a test of the `GameModuleFramework` (GMF) foundation. The module is registered as `expanding-wild` and is now the default selected game; set `simulation.config.SimConfig.gameId` to `"basic-proxy"` to run the probability proxy.

The implementation uses a 5-reel by 5-row window, ten regular symbols (`T1`–`T5` and `L1`–`L5`), 15 left-to-right paylines, an expanding banner wild, and a basegame scatter trigger. `ExpandingWildConfig` separates shared game rules from `baseGame` and `freeGame` modes. Each mode has two independent spin sets and a weighted selector; the default selector is 3:1 for set 0 to set 1. Reel strips use integer IDs for spreadsheet editing: 0–4 map to `T1`–`T5`, 5–9 map to `L1`–`L5`, 10 is `BANNER`, 11 is `WILD`, and 12 is `SCATTER`. `ExpandingWildConfigAdapter` translates each set's strips into GMF reel strips. The configuration is intentionally small and is not balanced.

Every set uses five 60-stop reels containing only regular paying symbols (six copies of each symbol on every reel). Special symbols are inserted into the visible grid after reel stops: basegame set 0 inserts scatters, basegame set 1 inserts banners, freegame set 0 inserts none, and freegame set 1 inserts banners. Each insertion rule has a 0–3 count table, a reel/row heat map, a one-per-reel limit, and forbidden replacement symbols. The default count weights are 90:6:3:1 for 0, 1, 2, and 3 inserted symbols. No static `WILD` symbol is used because banners expand after the reels stop.

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

- Scatters are inserted only on reels 1, 3, and 5 in basegame set 0, at most one per reel. GMF's `ScatterTrigger` counts them after insertion; three visible scatters award five free spins by default. Freegame sets have no scatter insertion rule and cannot retrigger.
- Every basegame and freegame spin independently draws a set index from that mode's weighted selector (default set 0:set 1 is 3:1), then selects stops from that set's strips. Each visible banner in set 1 independently draws a multiplier from that set's `bannerMultiplierWeights`; each row defines a multiplier and its relative weight. Basegame and freegame multiplier tables are configured separately.
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

For replay, each expanding-wild spin records its mode, selected set, zero-based reel stops, inserted symbol locations, and resolved banner multiplier assignments in a versioned JSON payload. Replaying injects these decisions and recalculates the result; insertion heat-map and multiplier weights are not redrawn. The replay tool compares basegame, each freegame spin, and total wins with the values saved in the CSV.

The full reelset tool evaluates every configured basegame reel-stop combination using the visible regular symbols and paytable only. It deliberately skips symbol insertions, scatter triggers, banner expansion, and multipliers so it measures the underlying reel/paytable cycle without random feature mechanics. It still reports stop-combination counts and symbol award hits; its results are not the full game's feature-inclusive RTP.

A seeded 1,000,000-round simulation used seed `4042026`, 32 partitions, and four threads. A separate one-thread run with the same seed and partition count produced matching total and freegame distributions:

- Basegame scatter triggers: 7,375 (0.7375%).
- Free spins played: 36,679 (some rounds stop early at the win cap).
- Basegame winnings: 267,277.80.
- Freegame winnings: 20,259.10.
- Total winnings: 287,536.90; reported overall RTP against a stake of 1 per base round is 28.75%.

The measured RTP reflects the intentionally simple example configuration. Each winning line applies its paytable multiplier to the full round stake rather than splitting stake across the 15 paylines; up to three inserted banners can contribute additive multipliers on a line. These defaults are for exercising the framework, not for balance.

The run exposed reporting issues in existing generic simulation code:

The console and report now calculate freegame RTP against the total basegame stake exposure, and total-game standard deviation uses the total-game distribution. The Awards lines show the 30 per-symbol 3/4/5-of-a-kind categories and count each paid line.

## Suggested simulator work

The simulator aggregates named awards but does not retain full game-specific outcomes for export. An optional detailed report could include stopped grids, expanded reels, banner multipliers, scatter counts, and selected winning paylines.
