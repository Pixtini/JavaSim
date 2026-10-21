# Game Creation Form — G002 Ember Forge

Completed adaptation of the [canonical form](../../../docs/game-creation-form.md). [Creation workflow](../../../docs/game-creation-flow.md) · [Math validation workflow](../../../docs/game-math-validation-flow.md).

## 1. Game identity and source material

- Name / stable ID: Ember Forge / `G002-ember-forge`.
- Rule source: agent-authored [GDD v1](GDD.md), 20 October 2026, under the user's end-to-end prototype request. Human review pending.
- Workspace / package: `game/G002-ember-forge/`; `game.ember_forge`.
- Research: corrected six-month revision 2, linked in [rationale](research-rationale.md); broad family: [lines](../../broadGDDs/lines.md).
- Reference implementations inspected: G001 session/parser/results and Expanding Wild player/GMF integration. No math copied.
- Review: [AGENT-REVIEW.md](../AGENT-REVIEW.md); no unresolved implementation-rule question. Production balance approval remains open.

## 2. Rules and math definition

Base stake determines all payouts and 8,100× cap; paid cost is 1× Normal, 2× EM1 or 47× BB1. No simulation currency rounding; prototype wallet rounds on debit/settlement. Fixed 5×3, six regulars and Wild/Scatter. Circular uniform stops; ten left-to-right lines, three or more matches, highest award per line, overlapping lines sum. Wild substitutes; specials have no own pay. Full IDs/paytable/line coordinates are in GDD and PAR.

BG-0 contains no Scatters; BG-1 always shows three Scatters and zero line pay. Normal selects entry 1/139, EM1 selects4/139, BB1 always entry. All entries award the same eight FG-0 spins. Heat starts 1, multiplies current awards, rises only after a positive free-spin win to maximum 5, and resets after the feature. No retriggers, extra feature mode, persistence, reel expansion or other evaluation family. Cap clips the reaching spin and ends the round. BB1 is the explicit non-random purchase exception; its entry screen is otherwise identical.

## 3. Configuration plan

[PAR.xlsx](PAR.xlsx) contains three isolated mode pairs. [JSON](balance-input.json) + [writer](build_par.py) reproduce its numeric inputs. Game-wide settings A1:B7; paytable A9:F18; lines A20:F31; BG selector A33:B36; FG selector A38:B40; Scatter map A42:B44.

| Set | Selection | Reels | Feature settings |
|---|---|---|---|
| BG-0 | Normal 138/139, EM1 135/139, BB1 0 | 5×12, ordinary | No trigger |
| BG-1 | Normal 1/139, EM1 4/139, BB1 1 | 5×3, entry | Three Scatters → eight free spins |
| FG-0 | Always when feature active | 5×12 | Same Heat 1–5 law across modes |

- [x] Dimensions, IDs, paytable, paylines, cap and all selectors defined.
- [x] Config validates legal symbols, strips, lines, costs and rule limits; parser rejects inconsistent cross-mode rules, missing mode pairs, bad weights, gaps and extra sets.
- [x] Config is immutable and inputs fail before simulation starts.

## 4. Reusable math and implementation boundaries

- [x] Reuse GMF `WeightedTable`, `ReelStrip`, `ReelGrid`, `Paytable`, `PaylineWinCalculator` and highest-payout policy.
- [x] Unique Heat progression, round orchestration and typed results remain in game code; no new generic algorithm required.
- [x] Supplied RNG only; no scheduling/statistics/reporting inside game implementation.
- [x] Worker-count repeatability verified with fixed seed and eight logical partitions.
- [x] Shared factory/player/viewer/replay consumers checked; GMF/engine/stats unchanged.

## 5. Game contract and results

- [x] `EmberForgeGame` implements Game; `EmberForgeSession` implements GameSession.
- [x] Round returns one Basegame + zero/eight Freegame results, shortened only by cap.
- [x] `EmberForgeSpin` preserves screen, lines, current/next Heat, trigger, set and optional replay event; capped line details reconcile.
- [x] Metadata: two BG sets, one FG set, selected cost, 8,100× cap. No named award bucket is needed; detailed lines and generic distributions report wins.
- [x] Shared statistics retain total/base/free/set accounting and mode-paid exposure.

## 6. Integration

- [x] Wizard allocated workspace; code and tests live inside it.
- [x] Separate config, Basegame, Freegame, session, evaluator and results.
- [x] Registered in GameFactory, simulation GUI, win viewer and game launcher.
- [x] Actual-session player adapter and direct `EmberForgePlayerMain` added.
- [x] Existing Game/GameSession/GMF/engine/statistics contracts reused.

## 7. Deterministic tests

[EmberForgeGameTests.java](../src/test/java/game/ember_forge/tests/EmberForgeGameTests.java) and [EmberForgeMathAudit.java](../src/test/java/game/ember_forge/tests/EmberForgeMathAudit.java).

- [x] Valid/invalid PAR, isolated report mode, immutable config, extra sets, mismatched FG and invalid costs.
- [x] Every ordinary/free stop combination against independent oracle; every entry stop combination and selector ticket.
- [x] Wild selection, scaling, Heat order/loss/saturation/reset, eight-spin accounting.
- [x] Maximum and lower caps, generated and replayed; typed win detail totals.
- [x] Bad replay sequences/version/stops; recorded money ignored during recalculation; replay consumes no RNG.
- [x] Player totals and costs, saved simulation outcomes, worker-count repeatability.
- [x] Focused shared integration regression suites pass; see evidence.

## 8. Replay and optional tooling

- [x] Version `v1;set;stop0,stop1,stop2,stop3,stop4` per event; Heat recalculated.
- [x] Generic report replayer validates every saved round against the exported selected-mode workbook.
- [x] Animated playable adapter with wins, current/next Heat, mode charging and launcher integration.
- [x] Game-specific exact audit supports these small reels and Heat law. Generic exhaustive-tool support is not claimed.

## 9. Validation evidence

[Validation report](../validation/validation-report.md), [exact math](../validation/exact-math.csv), [run summary](../validation/run-summary.csv), retained report folders and [manifest](../validation/manifest.json) supply commands, hashes, settings and uncertainty.

- [x] Build and 5,319 game assertions; 497,664 exhaustive ordinary/free screens and 243 entry combinations.
- [x] Small seeded runs: 2,000 rounds per mode, stake1, seeds440055/56/57, two workers, sixteen partitions.
- [x] Larger runs: Normal 2m, EM1 2m, BB1 200k with same explicit settings.
- [x] No unexplained math discrepancy; theory and estimates agree within sampling uncertainty. Zero sampled cap hits does not establish cap probability.
- [x] Reports include selected PAR/HTML, per-set statistics, distributions and saved events; all 60 saved exported-report replays pass.
- Desktop UI evidence and limitations are recorded separately in the validation report.

## 10. Documentation and completion

- [x] GDD, rationale, math design, flow, edge cases, scope review and review log populated.
- [x] [Handoff](../IMPLEMENTATION.md), [README](../README.md) and architecture updated.
- [x] Only new G002 numerical config selected; existing game configs preserved.
- [ ] Human product and production math approval — outside this agent prototype run.

Status: **implemented rough prototype, ready for review**, 20 October 2026.

## 21 October 2026 extension — recorded books

- [x] Human-supplied Normal/EM1/BB1 Books sheets and mode CSVs preserved.
- [x] Shared Simulation toggle selects only weighted recorded complete rounds; missing or invalid inputs block execution.
- [x] Complete statistics, cost/stake scaling, replay capture, fixed-partition repeatability and portable report bundles checked.
- [x] New finite-book return/trigger expectations recorded separately in [book validation](../validation/books-2026-10-21/validation-report.md); original reel balance above is historical.
- [ ] Human review/rebalance of book weights and desired Normal/EM1 relationship.
