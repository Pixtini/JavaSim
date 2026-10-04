# Expanding Wild Game

## Summary

This is the first reel-based game module and a test of the `GameModuleFramework` (GMF) foundation. The module is registered as `expanding-wild` and is now the default selected game; set `simulation.config.SimConfig.gameId` to `"basic-proxy"` to run the probability proxy.

The implementation uses a 5-reel by 5-row window, ten regular symbols (`T1`–`T5` and `L1`–`L5`), 15 left-to-right paylines, an expanding banner wild, and a basegame scatter trigger. `ExpandingWildConfig` contains the symbol definitions and mapping as well as all game settings. Reel strips are integer ID lists, so they can be pasted from spreadsheet output: 0–4 map to `T1`–`T5`, 5–9 map to `L1`–`L5`, 10 is `BANNER`, 11 is `WILD`, and 12 is `SCATTER`. The configuration is intentionally small and is not balanced.

## Rules implemented

- A spin independently stops one configured circular strip for each of the five reels. The five symbols beginning at each stop form the visible reel.
- A banner anywhere in a stopped reel expands that reel to five wilds after all reels stop. Each default reel strip has one banner.
- Paylines are read left to right. A win must start on reel one and continue without a mismatch. Only 3, 4, and 5 matching reels pay. Each line awards its best paying symbol once, including wild substitutions.
- The 15 line paths below are zero-based visible row indexes, one row per reel:

  ```text
  2-2-2-2-2   0-0-0-0-0   4-4-4-4-4
  1-1-1-1-1   3-3-3-3-3   0-1-2-3-4
  4-3-2-1-0   1-0-1-2-1   3-4-3-2-3
  2-1-0-1-2   2-3-4-3-2   0-0-1-2-3
  4-4-3-2-1   1-2-3-2-1   3-2-1-2-3
  ```

- Scatters are present only on reels 1, 3, and 5. GMF's `ScatterTrigger` counts the configured symbol on those reels; meeting the configured threshold awards eight free spins. Scatter count is evaluated on the stopped grid before expansion. The default strips place the scatter five stops away from the banner, so a visible banner cannot cover a visible scatter.
- Free spins use the same reel strips with scatters removed, so they cannot retrigger. Each visible banner expands and independently draws a multiplier from `ExpandingWildConfig.bannerMultiplierWeights` using GMF's `WeightedTable`. Each row defines a multiplier and its relative weight; the default table assigns weight 1 to each value from 2x through 10x, preserving uniform odds.
- Multipliers on a winning line add together. For example, 2x and 3x produce a 5x line multiplier. A banner only contributes when it is part of that line's consecutive winning prefix. Basegame banner wilds use 1x.

The chosen free-spin award and line paths are example values because the request did not specify them. Both live in `ExpandingWildConfig` and can be changed there.

## Example paytable

Values are credits per winning line, before any freegame banner multiplier. They are deliberately simple and are not scaled to the simulator's total round stake.

| Symbol | 3 of a kind | 4 of a kind | 5 of a kind |
| --- | ---: | ---: | ---: |
| A | 2 | 5 | 12 |
| B | 2 | 6 | 15 |
| C | 2.5 | 7 | 18 |
| D | 3 | 8 | 20 |
| E | 3 | 9 | 24 |
| F | 3.5 | 10 | 28 |
| G | 4 | 12 | 32 |
| H | 4 | 14 | 36 |
| I | 5 | 16 | 40 |
| J | 5 | 18 | 45 |

## Results

`ExpandingWildSpinResult` extends the shared `SpinResult` with the stopped and expanded grids, scatter count, expanded banner reels, freegame mode, and selected payline wins. Each line win retains its symbol, match length, base paytable value, applied multiplier, and final win. `GameRoundResult` returns the base result and each free spin result through the existing game contract.

The shared simulator currently accepts one paytable index per spin. This module maps it to loss or win so existing hit counts and win distributions work. That does not provide the 30 symbol-by-match-length award counts, and the runner currently discards the detailed game result after accumulating its generic statistics. Those are simulator-framework gaps; no simulation engine or generic statistics changes were made for this game.

## Verification and observed behavior

The game tests cover reel-window wrapping, unbroken paylines, paytable lengths, banner expansion, additive multipliers, scatter eligibility, no freegame retriggers, configured free-spin count, game-factory selection, and seeded agreement across worker-thread counts.

A seeded 1,000,000-round simulation used seed `4042026`, 32 partitions, and four threads. A separate one-thread run with the same seed and partition count produced matching total and freegame distributions:

- Basegame scatter triggers: 30,046 (3.0046%).
- Free spins: 240,368 (eight per trigger).
- Basegame winnings: 38,430,848.50.
- Freegame winnings: 195,640,722.00.
- Total winnings: 234,071,570.50; reported overall RTP against a stake of 1 per base round is 23,407.16%.

The very high RTP is expected from the deliberately unbalanced example: line payouts are applied to all 15 paylines without splitting the round stake across lines, and freegame banner multipliers can add to as much as 50x on a line with five banners. It is a math/configuration result, not a simulator crash or aggregation mismatch.

The run exposed reporting issues in existing generic simulation code:

- Freegame RTP divides freegame winnings by the number of free spins times the configured stake. Since these free spins are not separately staked, that value is not the freegame contribution against basegame stake.
- The total-game standard deviation printed by `Print` is currently read from the freegame distribution instead of the total-game distribution.
- The Awards line can only show the module's loss/win buckets, not its per-symbol 3/4/5-of-a-kind awards.

These are documented follow-ups for the simulation framework and were not changed as part of this game task.

## Suggested simulator work

Before using reports for symbol-level analysis, the simulator needs a game-result aggregation hook that can collect multiple line awards from one spin and preserve game-specific detail for optional report output. It should also define how total round stake maps to line stake and calculate freegame contribution against basegame stake. These changes are recommendations only and have not been made here.
