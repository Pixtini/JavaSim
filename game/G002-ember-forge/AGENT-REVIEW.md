# G002 Ember Forge — source and implementation review

20 October 2026. Status: **internally consistent and implemented as a rough prototype; ready for human review**. Author review only. The user's end-to-end request authorized an original concept, new PAR selection, implementation and rough balance; it does not imply human approval of these resulting rules.

## Source inventory and precedence

Reviewed [GDD v1](source/GDD.md), [shared line rules](../broadGDDs/lines.md), [research rationale](source/research-rationale.md), [flow](source/flowchart.md), [edge cases](source/edge-cases.md), [math design](source/math-design.md), [scope review](source/scope-review.md), [creation form](source/game-creation-form.md), numerical JSON and actual workbook cells. Corrected six-month research revision 2 is inspiration; it does not supply proprietary math or establish feature popularity.

GDD defines behavior; PAR supplies selected numerical inputs. The workbook contains Normal/EM1/BB1 pairs. All six sheets were inspected through the reader/parser and generated XML; no external workbook/PDF remains uninspected. Selected-mode report exports intentionally contain only their two sheets and remain valid inputs.

## Rule/config/test trace

The following ranges apply to each `<Mode>-Config` sheet unless otherwise specified.

| Rule | Configuration/source evidence | Deterministic verification |
|---|---|---|
| G-01 symbols/screen | A1:B7 settings; A9:F18 paytable | Parser validates 5×3, eight IDs, six paying regulars and two nonpaying specials |
| G-02 ten lines and units | A20:F31; GDD payout/selection rules | Every BG-0 and FG-0 stop combination agrees with an independent integer oracle; all-wild, scaling and detail totals |
| BG-01 two sets | A33:B36 weights; Reels A:E BG-0, G:K BG-1 | All 243 entry screens have exactly three Scatters and zero pay; every selector ticket tested |
| FG-01 single eight-spin Heat feature | A5:B6, A38:B40, A42:B44; Reels M:Q FG-0 | Current/next Heat, losses, saturation, resets, eight spins, exact finite-state expectation |
| M-01 modes | B3 costs 1/2/47; B4 denominator139, B35:B36 selectors | Fourfold probability exactly; cross-mode invariants; selected-mode report replay |
| CAP-01 | B7 = 8100; GDD termination order | Generated and replayed attainable maximum, injected 0.13 cap and typed line clipping |
| Replay/player | Versioned stop payloads; same session math | RNG-free recalculation, malformed/truncated events, mode charge and player totals |

## Decisions and resolved findings

| ID | Decision / resolution | Status |
|---|---|---|
| D-01 | Use a supported ten-line 5×3 design, one Heat Freegame; reject collection boards and additional bonus tiers | Resolved in GDD/rationale |
| D-02 | Six regular + two special symbols; two BG sets and one FG set shared across modes | Within skill limits |
| D-03 | Choose new rough PAR near 97%, D = 139 and BB1 cost = 47; no production target was supplied | Agent-selected prototype parameters |
| D-04 | BB1 uses a legal non-paying entry screen; no cost-scaled payouts | Resolved and tested |
| I-01 | Parser originally required all three mode pairs, conflicting with the generic selected-mode report exporter | Fixed: validate every present pair, allow a single selected report pair; report replay tests pass |
| I-02 | Reject extra reel columns/sets instead of silently ignoring them | Fixed and tested |

No unresolved rule/config blocker or unexplained validation discrepancy remains for this prototype. Human review should decide theme/appeal, whether the very short entry strips feel natural, Heat comprehension, production RTP and volatility targets. These are remaining product/balance approvals, not hidden numerical inputs.

## Implementation boundary

Existing GMF reels, weighted selection, payline evaluation and highest-payout selection are reused. Heat state/round orchestration and result adaptation stay game-owned. No GMF, simulation-engine or statistics change was needed. Factory, GUI, player adapter and screen printer integration were added. [Implementation handoff](IMPLEMENTATION.md) and [validation evidence](validation/validation-report.md) record the completed checks.

## 21 October 2026 — recorded-book balance layer

The user supplied three `<Mode>-Books` sheets and matching `source/Books/saved_gameplays_<mode>.csv` harvests, and requested a shared opt-in simulation mechanism. [Book validation](validation/books-2026-10-21/validation-report.md) records input hashes, full active-round reconstruction, deterministic/integration tests and real Simulation-window coverage. Inputs were preserved; duplicate ID contributions are additive and zero weights disable rows. The finite books' return/trigger expectations differ from the original reel game's math, including a high Normal expectation and no fourfold Normal/EM1 trigger relation. These remain human weight-balancing decisions, not unresolved implementation rules or agent approval of the supplied balance. Source/creation records above describe the original prototype revision.
