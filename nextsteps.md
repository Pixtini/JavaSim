# Next steps

## 1. Include complete run configuration in reports

The report records rounds, stake, seed, partitions, and elapsed time, but not all game settings or worker count. Include the selected game's configuration and worker count so a report captures the inputs needed to reproduce and interpret a run. The game contract is now in place; keep game-specific settings with each game's implementation.

## 2. Add a symbol-based game implementation

Implement a small slot game with reel or symbol outcomes, basegame wins, and randomly triggered freegames behind `game.Game` and `game.GameSession`. Keep the probability proxy as a separate implementation and use the new game to exercise the contract end to end.

## 3. Add contract-focused tests

Use a small test game implementation to verify the engine handles basegame outcomes, triggered freegame outcomes, and seeded sessions without depending on proxy-specific classes.
