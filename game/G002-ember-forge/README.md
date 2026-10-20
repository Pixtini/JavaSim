# G002 — Ember Forge

An original research-led, small line-game prototype. Five reels, three rows, ten lines; three Scatters award eight Forge Freegames. Winning free spins raise **Heat for the next spin**, from 1× to 5×. Exactly one Freegame, six regular symbols, two specials, two Basegame sets and one Freegame set.

## Play

From the JavaSim repository root:

```sh
./run.sh toolkit.player.EmberForgePlayerMain Normal
./run.sh toolkit.player.EmberForgePlayerMain EM1
./run.sh toolkit.player.EmberForgePlayerMain BB1
```

Launch one mode at a time. An optional second argument seeds the game, e.g. `BB1 86`. Or run `./run.sh` and choose G002 in the Game launcher, Simulation or Win viewer. Mode is selected before opening a player window.

The desktop prototype has manual Spin, counted Auto Play/Stop, line highlighting, current/next Heat, feature playback and a local pretend balance. Normal costs 1× base stake, EM1 costs 2× with exactly four times the Normal feature chance, BB1 costs 47× for the same feature. Free spins cost nothing extra. **Replay Last Play charges again and repeats the prior display outcome**, as in the existing shared player. It is a debugging/balancing control, not a fresh random spin.

Prototype labels: **C** Coal, **Cu** Copper, **Fe** Iron, **Ag** Silver, **Au** Gold, **Em** Ember, **W** Wild, **S** Scatter. Colors and short labels are placeholders for art. [Actual UI capture](validation/prototype.png).

## Inspect or edit

- [GDD](source/GDD.md), [flow](source/flowchart.md), [edge cases](source/edge-cases.md), [research rationale](source/research-rationale.md).
- [Numerical design](source/math-design.md), [PAR workbook](source/PAR.xlsx), [editable input JSON](source/balance-input.json), [stdlib-only PAR writer](source/build_par.py).
- [Source review](AGENT-REVIEW.md), [completed creation form](source/game-creation-form.md), [implementation handoff](IMPLEMENTATION.md).
- [Validation report](validation/validation-report.md), [exact math](validation/exact-math.csv), [simulation summary](validation/run-summary.csv), [code/input hashes](validation/manifest.json).

Current theoretical RTP: **97.1069% Normal, 97.1453% EM1, 97.1821% BB1**. Normal entry chance 1/139; EM1 4/139. Maximum 8,100× base stake. These are new agent-selected rough parameters, not a supplied production target or approved commercial balance.

## Verify

```sh
./build.sh
java -Xmx512m -cp build/classes game.ember_forge.tests.EmberForgeGameTests
java -Xmx512m -cp build/classes game.ember_forge.tests.EmberForgeMathAudit
sh game/G002-ember-forge/validation/run-validation.sh smoke
```

Use `balance` or `all` for the larger retained-run settings. New reports get timestamped folders. The script compiles the validation skill's temporary runner outside the source tree; no persistent simulator config edits. Optional desktop check, requiring a macOS display:

```sh
java -Xmx512m -cp build/classes game.ember_forge.tests.EmberForgePrototypeCheck
```

Source code is under `src/main/java/game/ember_forge/`, focused checks under `src/test/java/game/ember_forge/tests/`. The complete game uses the existing engine, statistics and GMF math unchanged. Prototype implementation and validation are complete; human appeal/comprehension review and production math approval remain pending.
