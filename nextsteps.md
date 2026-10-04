# Next steps

## 1. Include complete run configuration in reports

The report records the selected game ID, rounds, stake, seed, partitions, and elapsed time, but not the worker count or the selected game's settings. Add the worker count and a game-provided configuration summary so reports capture the inputs needed to reproduce and interpret a run. Keep each game's configuration beside its implementation.

## 2. Add a symbol-based game implementation

Implement a small slot game with reel or symbol outcomes, basegame wins, and randomly triggered freegames behind `game.Game` and `game.GameSession`. Keep the probability proxy as a separate implementation and use the new game to exercise the contract end to end.

## 3. Add contract-focused tests

Use a small test game implementation to verify the engine handles basegame outcomes, triggered freegame outcomes, and seeded sessions without depending on proxy-specific classes.
