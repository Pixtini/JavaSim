# Next steps

## 1. Include complete run configuration in reports

The report records rounds, stake, seed, partitions, and elapsed time, but not all `game.config.GameConfig` values or worker count. Include those settings so a report captures the inputs needed to reproduce and interpret a run. Keep game-specific settings with each game's implementation and simulation-wide settings with the simulation layer.

## 2. Add a symbol-based game implementation

Implement a small slot game with reel or symbol outcomes, basegame wins, and randomly triggered freegames behind `game.Game` and `game.GameSession`. Keep the probability proxy as a separate implementation and use the new game to exercise the contract end to end.

## 3. Add contract-focused tests

Use a small test game implementation to verify the engine handles basegame outcomes, triggered freegame outcomes, and seeded sessions without depending on proxy-specific classes.
