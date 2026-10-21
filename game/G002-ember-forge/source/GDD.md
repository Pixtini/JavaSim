# G002 Ember Forge — Game Design Document

Version 1, 20 October 2026. Agent-authored prototype under the user's end-to-end design authorization; human product sign-off and production math approval remain pending.

## Concept and rule authority

A small forge-themed line game: eight Freegames build Heat after wins, making the next win worth more. Shared rules: [broad line GDD](../../broadGDDs/lines.md). This document defines behavior; [PAR.xlsx](PAR.xlsx) supplies the selected rough numerical configuration, reproduced by [balance-input.json](balance-input.json) and [build_par.py](build_par.py). Conflicts must be resolved, not silently inferred from code.

## G-01 — Screen and symbols

Fixed five reels, three visible rows; reel-major coordinates and zero-based row indexes. Circular strips select one uniformly random top stop independently per reel.

Six regular symbols: IDs 1 COAL, 2 COPPER, 3 IRON, 4 SILVER, 5 GOLD, 6 EMBER. Special ID 7 WILD substitutes for every regular and does not self-pay; ID 8 SCATTER triggers only, without a scatter payout. No other symbols, expanded boards, cascades, persistent paid-round meters or secondary bonuses.

## G-02 — Lines and payment

Ten fixed paylines, left to right starting on reel one, three or more consecutive matching symbols. Each line pays separately, overlapping lines allowed; select the highest monetary candidate per line, including a leading-wild substitution candidate. All-wild lines substitute for the best-paying regular. Payouts are multiples of base stake, never multiplied by purchase cost.

Lines: `11111, 00000, 22222, 01210, 21012, 00122, 22100, 10001, 12221, 01110`. Payouts for 3/4/5 matches respectively:

| Symbol | 3 | 4 | 5 |
|---|---:|---:|---:|
| COAL | 0.15 | 0.35 | 1.00 |
| COPPER | 0.15 | 0.45 | 1.35 |
| IRON | 0.20 | 0.70 | 2.05 |
| SILVER | 0.35 | 1.35 | 4.05 |
| GOLD | 0.70 | 2.70 | 10.80 |
| EMBER | 1.35 | 6.75 | 27.00 |

All awards have 0.05-base-unit granularity before stake scaling. Simulation retains numerical precision; prototype wallet presentation rounds currency to two decimals.

## BG-01 — Two Basegame sets

Set selection is the first random operation. BG-0 has ordinary regular/wild strips with no scatter. BG-1 is the entry set: reel i contains only regular ID i+1, plus one Scatter on reels 1, 3 and 5. Its strips each have length three, so exactly three Scatters are visible regardless of stops. Distinct regular types on consecutive reels guarantee no line award. Scatters can appear in any visible row, one per qualifying reel.

Select BG-0 or BG-1 using mode-specific integer weights. Then draw five reel stops. Evaluate line awards, determine the scatter award and apply the round cap. No replacement, rejection loop or hidden trigger draw is used.

## FG-01 — Exactly one Freegame

Three Scatters award exactly eight spins of FG-0, initialized at Heat 1. Every free spin independently uses the same FG-0 strips. There are no Scatters in FG-0, retriggers, additional entry tiers or extra Freegame sets.

For each free spin: draw five stops, evaluate all lines with current Heat multiplying the selected line awards, sum the win, apply the remaining round cap, then increase Heat by one after a positive win for the following spin, up to Heat 5. A loss leaves Heat unchanged. Heat never changes midway through a spin and resets to one at the next feature; it never carries into another paid round.

## M-01 — Purchase modes

Only Normal, EM1 or BB1 can be selected before the paid round; selection stays fixed until settlement.

- Normal costs 1× base stake; BG-1 weight 1, BG-0 weight D−1.
- EM1 costs 2× base stake; BG-1 weight 4, BG-0 weight D−4.
- BB1 costs the explicit multiplier in its PAR; BG-1 has all weight and BG-0 is unreachable. It starts with the same non-paying entry Basegame and buys the identical eight-spin FG-0.
- D is the shared trigger denominator in the numerical configuration. Therefore pEM1 = 4/D = 4pNormal exactly. No mode alters Freegame initialization, strips, awards or Heat law. Free spins incur no additional charge.

See [math-design.md](math-design.md) for the selected D, buy price and balance evidence. None are copied from another game's PAR.

## CAP-01 — Settlement and state

A complete round is one Basegame result followed by its awarded Freegame results. The combined maximum is 8,100× base stake. Clip the spin reaching a supplied lower validation cap and stop further feature generation. The configured cap bounds all possible outcomes: eight winning spins use Heat sum 30 and at most 10×27 per spin before Heat, giving 8,100×.

No Basegame on the entry set can win, so BB1 cannot consume the cap before its purchased feature. No state survives settlement. Replay records selected set and five stops per spin; Heat is recalculated from previous wins.

## Package

[Research rationale](research-rationale.md) · [Flow](flowchart.md) · [Edge cases](edge-cases.md) · [Math/config](math-design.md) · [Scope review](scope-review.md) · [Creation form](game-creation-form.md).

## SIM-BOOKS-01 — Recorded-outcome simulation, added 21 October 2026

The Simulation tool can select a separate recorded-book outcome source. It samples complete recorded rounds using `<Mode>-Books` weights and the matching mode CSV, instead of executing fresh BG/FG random selection. Recorded outcomes are reconstructed under the deterministic rules above; base stake still scales payouts and the cap, while purchase cost affects exposure. The original M-01 trigger probabilities describe fresh reel simulation. Independently reweighted books have their own return/trigger expectations and do not automatically retain the fourfold EM1 relationship. [Book workflow](../../../docs/gameplay-books.md) and [book validation](../validation/books-2026-10-21/validation-report.md) specify this simulation/config layer.
