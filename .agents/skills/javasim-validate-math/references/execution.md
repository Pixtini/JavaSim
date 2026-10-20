# Controlled execution and evidence

## Maintained sources

- [Validation workflow](../../../../docs/game-math-validation-flow.md): staged rule and statistical checks.
- [Architecture](../../../../docs/architecture.md): tests, denominators, reports and determinism.
- [Main](../../../../Main.java), [SimConfig](../../../../simulation/config/SimConfig.java): currently Main accepts workbook path and mode; game ID, rounds and seed behavior use SimConfig. It is not a general simulation argument parser.
- [Runner](../../../../simulation/engine/SimulationRunner.java), [stats](../../../../simulation/stats/StandardStats.java), [reporter](../../../../simulation/reporting/Print.java): actual accounting and runtime behavior.
- [Full reelset CLI](../../../../toolkit/reelset/FullReelsetMain.java): inspect arguments, game support and workload before use.
- [G001 lessons](../../../../docs/lessons/G001-LESSONS-LEARNED.md): unit separation and accounting regressions.

## Compile and relevant tests

Run `./build.sh` after changes, then `java -cp build/classes` with applicable classes:

| Scope | Main class |
|---|---|
| Engine/stats/reports/repeatability | `SimulationTests` |
| G001 rules/parser/report/replay | `game.walkingwild.tests.WalkingWildGameTests` |
| Expanding Wild math / PAR | `game.expandingwild.tests.ExpandingWildGameTests` / `game.expandingwild.tests.ExpandingWildParParserTests` |
| Generic workbook reader | `toolkit.par.ExcelWorkbookReaderTests` |
| Replay / exhaustive | `ReplayTests` / `ReelsetTests` |
| Workspace generation | `GameProjectWizardTests` |
| Presentation | `PlayerTests` / `ViewerTests` |

Inspect current suites to choose relevant coverage; every task does not need every suite.

## Temporary seeded run

The sibling asset `assets/SeededSimulation.java.template` is deliberately not a Java source extension because the build scans all repository Java files. Copy and compile it in a temporary directory. From the root after compiling the project:

```sh
javasim_run_dir=$(mktemp -d "${TMPDIR:-/tmp}/javasim-skill-run.XXXXXX")
cp .agents/skills/javasim-validate-math/assets/SeededSimulation.java.template "$javasim_run_dir/SeededSimulation.java"
javac -cp build/classes -d "$javasim_run_dir" "$javasim_run_dir/SeededSimulation.java"
java -cp "build/classes:$javasim_run_dir" SeededSimulation \
  G001-walkingwild game/G001-walkingwild/source/G001-walkingWildsPAR.xlsx \
  Normal 2000 1.0 440055 2 16 "$javasim_run_dir/reports" 10
```

Arguments: game ID, workbook (`-` for non-PAR), mode, rounds (long), base stake, seed, threads, partitions, report root (`-` disables export), maximum saved gameplays. Capture stdout and retain reports until evidence is recorded. The template prints the absolute report root. Two thousand rounds is a smoke check, not balance validation. Non-PAR template runs support Normal mode only.

Record code revision, workbook SHA-256 (e.g. `shasum -a 256 path/to/PAR.xlsx`), selected pair, mode cost and units, effective seed, partitions, threads, rounds and commands. Retain the exported workbook alongside replay data. No developer-specific Python path is required for these Java checks.

Inspect total winnings, contribution RTPs, triggers/hits, awards, distributions and caps. Freegame spin statistics and per-trigger feature distributions have different observation counts. Separate base-unit payouts from paid mode cost in RTP interpretation. Avoid exact floating-point equality in statistical comparisons.
