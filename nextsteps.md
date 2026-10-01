# Next steps

## 1. Make game logic pluggable

`SimulationRunner` currently constructs `GameBasicBase` and `GameBasicFree` directly. Define a small game contract for running a base spin and its triggered freegame spins, then have the runner depend on that contract. This will let future slot games provide their own reel, payline, and bonus rules without changing parallel execution or statistics collection. Keep `GameBasic` as the simple probability proxy.

## 2. Include complete run configuration in reports

The report records rounds, stake, seed, partitions, and elapsed time, but not all `GameConfig` values or worker count. Add those settings so a report contains the inputs needed to reproduce and interpret the run.

## 3. Add a larger game example

Once the game contract is in place, implement a small symbol-based game with basegame wins and randomly triggered freegames. Use it to exercise the simulator beyond the current payout proxy and test its stats and report output end to end.
