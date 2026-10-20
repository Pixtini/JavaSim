# G002 Ember Forge validation — 20 October 2026

**Outcome:** implemented rough prototype agrees with its specified rules and independently calculated expectations. Human product approval and production mathematical sign-off are pending. No unexplained discrepancy remains in the performed checks.

## Inputs and scope

[GDD v1](../source/GDD.md), [source review](../AGENT-REVIEW.md) and [math design](../source/math-design.md) define authority and units. The user authorized new G002 configuration/rough balancing; no existing game configuration changed. [Manifest](manifest.json) records the Git base, Java 21 runtime and SHA-256 hashes of actual numerical inputs and code. This is an uncommitted implementation, so hashes identify the tested files rather than implying HEAD contains them.

All six source workbook sheets were inspected. Selected report workbooks contain only the selected mode's Config/Reels pair. Costs 1/2/47 affect the paid denominator; line awards and cap use base stake. There are two BG sets, one FG set, no retriggers or hidden extra bonus states. The theoretical results below apply only to the retained PAR, full 8,100× configured cap and specified Heat law.

## Exact and deterministic evidence

```sh
./build.sh
java -Xmx512m -cp build/classes game.ember_forge.tests.EmberForgeGameTests
java -Xmx512m -cp build/classes game.ember_forge.tests.EmberForgeMathAudit game/G002-ember-forge/validation/exact-math.csv
```

Build passed. [5,319 game assertions](game-tests.txt) cover parser/mode rejection, immutable data, all 243 entry stop combinations, every selector ticket, wild/highest-pay selection, stake/Heat scaling, next-spin ordering, losses/saturation/reset, maximum and lower caps, generated and replayed outcomes, bad/truncated replay, RNG-free replay, actual-session display totals, simulation accounting and one-versus-four-worker repeatability with fixed logical partitions.

The independent integer oracle checked **248,832 BG-0 and 248,832 FG-0 stop combinations** against the actual game evaluator. The eight-spin finite-state recurrence then includes all Heat paths, yielding feature mean **45.675585266473** base stakes and second moment 9760.97242426922. Entry has no line pay. The 8,100× bound is attainable in a controlled fixture; it cannot truncate excess from any allowed full-cap outcome. This establishes full-game expectation for this deliberately small model, rather than extrapolating feature math from a Basegame-only enumeration. [Audit log](exact-audit.txt) · [machine-readable moments](exact-math.csv).

The shared integration checks passed: SimulationTests 88, PlayerTests 12, ViewerTests 10 and ReplayTests 16 assertions. [Commands/results](regressions.txt). No shared GMF/engine/stats algorithm was changed; unrelated exhaustive, wizard and other game-specific math suites were not needed for this scope.

## Controlled runs

Before larger execution, all three modes ran 2,000 rounds and produced feature results. Normal had 9 triggers, EM1 69, BB1 2,000; small-sample RTP values are smoke evidence only. The larger runs then used:

| Mode | Rounds | Seed | Base stake | Workers | Logical partitions | Saved rounds |
|---|---:|---:|---:|---:|---:|---:|
| Normal | 2,000,000 | 440055 | 1 | 2 | 16 | 10 |
| EM1 | 2,000,000 | 440056 | 1 | 2 | 16 | 10 |
| BB1 | 200,000 | 440057 | 1 | 2 | 16 | 10 |

The validation skill's temporary `SeededSimulation` runner was copied to `/private/tmp` and compiled there. Its actual invocation pattern, from the repository root:

```sh
java -Xmx512m -cp build/classes:/private/tmp SeededSimulation \
  G002-ember-forge game/G002-ember-forge/source/PAR.xlsx Normal \
  2000000 1.0 440055 2 16 game/G002-ember-forge/validation/runs/Normal-balance 10
```

Equivalent EM1/BB1 commands use the settings and corresponding mode output roots above. Smoke calls use 2,000 rounds. No persistent `SimConfig` edits were made. [Reproduction script](run-validation.sh) recreates the temporary runner and uses the same settings: `sh game/G002-ember-forge/validation/run-validation.sh all`.

## Expected versus observed

| Mode | Exact RTP | Observed RTP | Approximate 95% interval for estimate | Triggers observed / expected |
|---|---:|---:|---:|---:|
| Normal | 97.1069% | 96.5833% | 95.3889–97.7777% | 14,299 / 14,388.49 |
| EM1 | 97.1453% | 97.8477% | 96.6840–99.0114% | 57,416 / 57,553.96 |
| BB1 | 97.1821% | 97.1972% | 96.3803–98.0141% | 200,000 / 200,000 |

Intervals use `estimate ± 1.96 × exact round SD / (cost × √rounds)` under independent, identically distributed rounds and the usual normal approximation. They describe sampling uncertainty around the Monte Carlo estimate; the exact expectation has no Monte Carlo interval. No universal RTP tolerance or production approval is inferred. Observed values are rounded from report output. RTP z-scores are −0.859, 1.183, 0.036; trigger z-scores are −0.749, −0.584, 0 respectively. These do not suggest a systematic discrepancy. EM1's exact fourfold trigger relation is verified from selector tickets, not inferred from the ratio of two noisy samples.

Mean feature pay observed in larger runs: Normal 45.16×, EM1 46.27×, BB1 45.68×, versus exact 45.6756×. All three runs had zero sampled cap hits; this is compatible with the rare maximum and is **not** proof that it is unreachable. Controlled maximum and low-cap fixtures verify termination.

[All run metrics and paths](run-summary.csv) include smoke and larger runs. Larger report folders:

- [Normal](runs/Normal-balance/simulation-2026-10-20_16-49-40/simulation_stats.txt).
- [EM1](runs/EM1-balance/simulation-2026-10-20_16-49-40/simulation_stats.txt).
- [BB1](runs/BB1-balance/simulation-2026-10-20_16-49-40/simulation_stats.txt).

Each folder retains selected PAR, its HTML rendering, summary, raw/aggregated distributions and saved events. Aggregate winnings reconcile across base/free/total within printed rounding; paid exposure and distribution counts reconcile. Raw distribution win labels use the existing report's one-decimal presentation, so those rounded CSV amounts are not an exact replacement for unrounded simulation moments.

## Export and replay

```sh
python3 game/G002-ember-forge/validation/verify_reports.py
```

[Verifier](verify_reports.py) passed six report bundles and **all 60 saved rounds**, using `toolkit.replay.ReplayerMain` against each report's selected workbook. It checks mode, costs, component accounting, eight spins per trigger, distribution observations, workbook/HTML presence and recalculated wins. [Full replay output](report-replays.txt). Tests separately cover altered/truncated input and show that recorded money is not accepted as authoritative payout.

An integration defect found during this run was fixed: the initial G002 parser required three mode pairs, while the generic report exporter retains only one. It now validates all present pairs and accepts a single selected report pair. No generic exporter/replayer change was needed.

## Playable prototype

`./run.sh toolkit.player.EmberForgePlayerMain Normal` launches the actual session; replace Normal with EM1 or BB1. The Game launcher also supports G002. [Desktop check source](../src/test/java/game/ember_forge/tests/EmberForgePrototypeCheck.java) opens all three actual Swing windows, verifies immediate debits, waits for settlement, checks all eight purchased Freegames, balances and replay-button charging. [Desktop log](prototype-check.txt) · [UI capture](prototype.png).

The first sandboxed desktop attempt exited 134 before window creation; running the same check with permitted desktop access passed. Visual inspection exposed long label overlap; the G002 adapter now uses compact symbol labels, documented in the README, and the desktop check was repeated. The screenshot shows the Freegame Heat state in the real window. Gameplay math was unaffected by that presentation change.

Shared manual play, highlights, counted autoplay/stop and charged replay controls are available. This run verified manual-spin/feature settlement and replay initiation; a separate end-to-end autoplay-stop sequence was not exercised. Replay Last Play repeats a saved outcome and charges again, following the shared prototype's existing behavior. Human testing of enjoyment, pacing, short entry-strip presentation and Heat comprehension remains outstanding. Graphics are placeholder symbols/colors; no production art, audio, certification, deployment or approval is claimed.
