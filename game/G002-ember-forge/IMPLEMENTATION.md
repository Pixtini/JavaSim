# G002 implementation handoff

20 October 2026. Research → original source package → rough PAR → working game → seeded reports → playable desktop prototype. **Ready for human review, not production approval.**

## Scope and source authority

Stable ID `G002-ember-forge`, package `game.ember_forge`, workspace allocated by `GameProjectWizard.create`. The user explicitly authorized a new idea, numerical selection, implementation, validation and prototype. [GDD v1](source/GDD.md) and [review](AGENT-REVIEW.md) resolve the implemented rules; [rationale](source/research-rationale.md) traces the corrected six-month report and rejected alternatives. Research supplies design hypotheses rather than copied math or proof of player preference.

The fixed 5×3, ten-line game has six regulars, Wild and Scatter; two BG sets; one eight-spin FG set with win-driven next-spin Heat 1–5. Normal/EM1/BB1 reuse identical Freegame rules. EM1 changes entry weights to four times Normal's trigger probability at 2× cost. BB1 buys the same nonpaying entry and feature for 47×. No retriggers or extra modes were introduced.

## Implementation

- Immutable `EmberForgeConfig` and `EmberForgeParParser` validate game/mode inputs, table shapes, IDs, costs, selectors, reel bounds and cross-mode invariants. Source workbook has all three mode pairs; selected two-sheet report exports also parse.
- Separate Basegame and Freegame components use supplied RNG, GMF weighted selection, circular reels and highest-payout line evaluation. `EmberForgeEvaluator` adapts that calculation to game-specific Heat/results.
- `EmberForgeSession` generates complete rounds, owns Heat lifetime, applies combined cap and reconstructs replay without RNG. Typed `EmberForgeSpin` preserves lines/grid/Heat when capped. Costs affect exposure; payouts/cap stay in base-stake units.
- Registered GameFactory, ToolkitGUI Simulation/Win viewer and Game launcher; adapter factory, direct `EmberForgePlayerMain`, and CLI screen printing support G002.
- No generic engine, statistics, result contract or GMF component was changed. No new generic calculation was needed; exact auditing/report verification lives in validation tooling, outside runtime game logic.

## Evidence

| Check | Result | Evidence |
|---|---|---|
| `./build.sh` | Passed, Java 21 | [Manifest](validation/manifest.json) records runtime and hashes |
| Game/parser/session/player/repeatability tests | 5,319 assertions pass | [Log](validation/game-tests.txt) |
| Independent integer oracle | All 248,832 ordinary and 248,832 FG screens agree with actual evaluator; 243 entry combinations checked | [Audit](validation/exact-audit.txt), [math design](source/math-design.md) |
| Shared integration regressions | Simulation 88, Player 12, Viewer 10, Replay 16 assertions pass | [Log](validation/regressions.txt) |
| Small seeded simulations | 2,000 rounds/mode pass with features observed | `validation/runs/*-smoke/` |
| Larger simulations | Normal 2m, EM1 2m, BB1 200k; consistent with exact expectations | [Summary](validation/run-summary.csv) |
| Reports and replay | Selected PAR + HTML, distributions, set stats;60/60 saved-report replays pass | [Replay log](validation/report-replays.txt), [verifier](validation/verify_reports.py) |
| Actual Swing prototype | All three mode debits and settlement, eight BB1 spins, Heat and replay control verified | [Desktop log](validation/prototype-check.txt), [preview](validation/prototype.png) |

All simulations use stake1, seeds440055/440056/440057 by mode, two workers, sixteen logical partitions, ten saved rounds per report. [Validation report](validation/validation-report.md) gives theoretical versus observed values and uncertainty. Repeatability tests fix seed/partitions and change one to four workers. No unexplained discrepancy remains.

## Completion and limits

[Creation form](source/game-creation-form.md) and `docs/architecture.md` are updated. Lasting numeric changes are confined to the newly authorized G002 PAR/JSON. Existing game configs and research artifacts were preserved. Narrow ignore exceptions retain the new PAR and report workbooks/HTML so the evidence survives version control.

The first UI capture exposed long symbol names overlapping; G002 now uses compact labels rather than changing shared viewport geometry. Desktop execution required display access outside the sandbox. The automated check exercised actual windows and accounting; human playtesting of appeal/feel remains outstanding. Shared autoplay remains available, but this run did not perform a separate end-to-end autoplay-stop test. Replay Last Play deliberately repeats an outcome and charges again. The prototype uses placeholder colors/text, without commissioned art or sound.

Suggested future framework improvement: a single game catalog could supply factory IDs, default PAR paths and supported player adapters to the three existing selector lists as the number of games grows. This is a proposal only; no registry redesign was made. The next product decision is whether this entry-set presentation and Heat progression deserve a human-led balance/art pass.
