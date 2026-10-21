# Weighted gameplay books — validation, 21 October 2026

Implemented the shared weighted-book Simulation path and GUI toggle, using the human-provided G002 PAR and harvested CSVs unchanged. The book weights are inputs, not agent-approved target math. Original reel-based validation remains in [the earlier report](../validation-report.md); its expectations are a different distribution.

## Contract and inputs

The runner loads `<Mode>-Books`, reads active referenced IDs from the adjacent mode CSV in one pass, reconstructs/validates all those rounds through game-owned replay without RNG, and caches typed outcomes at the requested base stake. Each simulated paid round makes only a weighted ID draw, with replacement. It then follows the original statistics/capture path. No generic statistics or game-rule implementation was changed.

The supplied folder is `source/Books/`, supported by case-insensitive directory/filename matching. All three supplied tables have **1,064 rows, 1,000 active distinct IDs and total weight 10,640**. Repeated IDs add their weights; zeros disable rows. Each of the 3,000 active mode-specific recordings successfully reconstructs and matches its saved base/free/total/event wins against the current PAR.

Missing selected-mode table, CSV or active ID blocks execution before worker creation. Invalid/noninteger/negative weights, zero total, overflow, wrong identity, incomplete/stale replay, fresh RNG during reconstruction and invalid result metadata are rejected. The same shared mechanism works through any Game/GameSession implementation with complete replay support; it is not a G002 dispatch branch.

## Observed finite-book math

| Mode | Expected finite-book RTP | Expected feature probability | 2,000-round observed RTP | Triggers observed |
|---|---:|---:|---:|---:|
| Normal | 364.219925% | 6.766917% | 331.835000% | 138 |
| EM1 | 93.070959% | 2.067669% | 101.920000% | 44 |
| BB1 | 99.336206% | 100% | 94.758936% | 2,000 |

[Summary CSV](summary.csv) records unrounded return ratios and approximate RTP z-scores of −0.6202, 0.4023, −1.1212. Standard error uses the finite book's exact weighted second moment, divided by paid cost and √rounds; the normal approximation is a sampling check, not approval. These short runs verify integration. Deterministic selector-ticket checks establish weight behavior separately from noisy samples.

Normal's high expected return is explained by its repeated bonus IDs: each of eight feature rounds appears nine times in the table, receiving combined weight 90 instead of 10. Those user-provided contributions were preserved. Independent books also do **not** retain the original EM1 fourfold trigger relation. The human balance should review both return and trigger expectations before treating these as final weights. No inference is made about the unrestricted reel game's return from these sampled/reweighted books.

## Commands and checks

```sh
./build.sh
java -Xmx512m -cp build/classes BookSimulationTests
java -Xmx512m -cp build/classes SimulationTests
java -Xmx512m -cp build/classes ReplayTests
java -Xmx512m -cp build/classes toolkit.par.ExcelWorkbookReaderTests
java -Xmx512m -cp build/classes game.ember_forge.tests.EmberForgeGameTests
java -Xmx512m -cp build/classes PlayerTests
java -Xmx512m -cp build/classes ViewerTests
java -cp build/classes GameProjectWizardTests
```

Passed: **127 shared book assertions**, 88 simulation, 16 replay, 5,319 G002, 12 player and 10 viewer assertions, workbook-reader checks and wizard workspace checks. [Regression output](regressions.txt). Book tests use a separate replay-only fixture whose fresh-spin method fails, enumerate every weight ticket, verify duplicate/zero-weight semantics, and check all normal statistics: hits, caps, triggers, individual feature spins, named awards, base/free/total accounting, set breakdowns and session distributions. They check one-versus-four-worker reproducibility with fixed partitions, requested-stake scaling, purchase-cost exposure, capture limits, bulk CSV errors and missing/invalid inputs. Real G002 tests export all three modes, replay all 30 saved report rounds and reload each exported book bundle.

The smoke harness is retained as [EmberForgeBookSmoke.java](../../src/test/java/game/ember_forge/tests/EmberForgeBookSmoke.java), reproducible with:

```sh
java -Xmx512m -cp build/classes game.ember_forge.tests.EmberForgeBookSmoke
```

Each mode used 2,000 rounds, stake 1, seed 440055, two workers, sixteen partitions and ten saved rounds. Each also ran a paired 2,000-round fresh control with Books disabled. Mode logs: [Normal](Normal-smoke.txt), [EM1](EM1-smoke.txt), [BB1](BB1-smoke.txt). Full report bundles are under `validation/runs/<Mode>-book-smoke/`; each retains selected Config/Reels/Books, copied input book CSV, HTML, regular/set statistics, distributions and sampled replays. Input hashes and tested-code hashes are in [manifest.json](manifest.json).

An actual Swing Simulation window selected G002/EM1, enabled **Use weighted gameplay books**, disabled report export and completed 2,000 rounds. Output identifies `EM1-Books`, the mode CSV, expected return and observed statistics. [GUI check output](gui-check.txt). Desktop access outside the sandbox was used. This check exercised the requested toggle in the real UI; it did not change the prototype player's separate fresh-spin controls.

The PAR generation helper was checked independently: attempting its default overwrite now refuses a workbook containing Books sheets, its `--output` option generates a separate numerical baseline, and SHA-256 confirms the human PAR was unchanged. G002 malformed-workbook tests now locate worksheets by name/relationship so inserting Books sheets cannot make them mutate the wrong mode's XML. New wizard workspaces include empty Normal-Books headers and a harvest directory; they do not fabricate recorded gameplays.

## Remaining balance work

The engine feature is complete for the tested contract. Human weight balancing, choosing a sufficiently representative harvest and deciding whether to enforce the original Normal/EM1 relationship remain product/math decisions. The current high Normal return was not corrected automatically. Edited PAR/CSV inputs must be revalidated; report export rejects a source input that changed after preflight. Larger simulations are unnecessary to establish the finite book expectation, which is directly computable from its weighted cached outcomes.
