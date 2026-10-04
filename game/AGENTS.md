# Game Development Instructions

## Purpose

This directory contains the implementation of individual slot games and their game features.

The simulator framework, statistics framework, and common mathematical libraries are separate from individual game implementations.

A game should contain only the logic required to define and execute that specific game's mathematical behaviour.

---

## Core Architecture

Every game should be built from three layers:

1. **Game Configuration**
   - Defines mathematical parameters and game-specific values.
   - Must not contain executable game logic.

2. **Common Game Features**
   - Provides reusable slot mechanics.
   - Examples include reel evaluation, win calculation, symbol evaluation, multipliers, feature triggers, and result aggregation.

3. **Game-Specific Logic**
   - Implements behaviour unique to the individual game.
   - Uses configuration and common mathematical components rather than duplicating generic functionality.

The simulator itself MUST remain independent of individual games.

A new game MUST NOT require modifications to the multithreaded simulation engine or generic statistics framework unless a genuinely new framework capability is required.

---

## Creating a New Game

When creating a new game:

1. Create a dedicated game directory/package as exampled by proxy.
2. Create the game implementation.
3. Create the game's configuration.
4. Define the game's symbols and mathematical parameters.
5. Define the Basegame.
6. Define any randomly triggered Feature Games.
7. Reuse existing common mathematical components wherever possible from the Game Module Framework.
8. Create game-specific components only where the behaviour is unique and highly unlikely to be reused.
9. If a new component is created, check if its addition into GMF is welcomed.
10. Define the game result structure.
11. Add tests for the game's mathematical behaviour.
12. Run a small simulation before running large simulations.
13. Create a .md file with the summary of creation and if any changes are suggested to the simulation framework (DO NOT AUTOMATICALLY IMPLEMENT THEM TO THE SIM FRAMEWORK).

A new game should be independently executable through the existing simulation framework.

---

## Game Structure

A typical game should follow this structure:

    game/
        GameName/
            GameName.java
            GameResult.java

            basegame/
                BaseGame.java
                BaseGameResult.java

            features/
                FeatureName.java
                FeatureResult.java

            config/
                game-config.java

            tests/

The exact structure may be simplified for very small games, but Basegame and Feature Game logic should remain clearly separated.

---

## Basegame

Every slot game must have a clearly defined Basegame.

The Basegame is responsible for:

- Generating the basegame outcome.
- Evaluating symbols.
- Calculating wins.
- Applying relevant multipliers.
- Determining feature triggers.
- Producing the appropriate game result.

The Basegame should not contain logic belonging exclusively to a Feature Game.

---

## Feature Games

Feature Games are randomly triggered from the Basegame unless the game specification explicitly requires another trigger mechanism.

A Feature Game should:

- Have its own clearly defined implementation.
- Have its own configuration where appropriate.
- Reuse common mathematics where possible.
- Produce a well-defined feature result.
- Return control to the Basegame/simulation flow after completion.

Feature logic should not be embedded directly into the Basegame when it can be represented as a separate feature component.

For example:

    BaseGame
        |
        +-- Feature Trigger
                |
                +-- FreeGame
                +-- BonusFeature
                +-- OtherFeature

---

## Common Mathematical Behaviour

Before implementing a mathematical mechanic, check whether the common game library already provides the required behaviour.

Common reusable behaviour includes:

- Reel evaluation
- Symbol matching
- Payline calculation
- Ways calculation
- Win aggregation
- Multipliers
- Wild behaviour
- Scatter evaluation
- Feature triggering
- Free-spin counting
- Cascade evaluation
- Reel transformations
- Result aggregation

Do not duplicate common mathematical behaviour inside individual games.

Prefer:

    Game
       -> Common Calculator
       -> Game Configuration

over:

    Game
       -> Duplicate Game-Specific Calculator

If an existing common component does not support the required behaviour, determine whether the behaviour is genuinely generic before modifying the common library.

DO NOT BLINDLY MODIFY FEATURES IN THE COMMON LIBRARY - THIS MAY BREAK PREVIOUSLY IMPLEMENTED GAMES.

Prefer:
    Adding a new feature, or keeping logic game specific

over:
    Changing an existing common function.

---

## Game-Specific Behaviour

Game-specific behaviour belongs inside the game implementation when it cannot reasonably be reused by other games.

Examples:

- A unique symbol transformation.
- A unique bonus mechanic.
- A game-specific reel interaction.
- A unique feature progression system.
- A special win-resolution rule.

Do not generalise a mechanic merely because it exists in one game.

Only move functionality into the common library when there is a clear reusable abstraction.

---

## Results

Games should return structured results rather than loosely related primitive values.

A game result should contain the information required by the statistics framework and any downstream processing.

For example:

    GameResult
        - totalWin
        - baseGameWin
        - featureWin
        - multiplier
        - featureTriggered
        - featureResult

Feature results should similarly expose the information required to understand the feature outcome.

Avoid adding statistics-specific logic to the game result.

The game produces the result.

The statistics framework interprets and aggregates the result.

---

## Randomness

Game randomness must use the random-generation mechanism provided by the simulator framework.

Do not create independent random generators inside individual game components unless explicitly required by the framework.

A game must be deterministic when supplied with the same simulation seed and configuration.

Do not use:

    Math.random()

for game mathematics.

Random events must be generated through the simulator's controlled RNG.

---

## Configuration

Game mathematical values belong in the game's configuration rather than being hard-coded.

Avoid:

    double multiplier = 5.0;

Prefer configuration-driven behaviour where appropriate.

Configuration should define values such as:

- Reel strips
- Paytables
- Symbol weights
- Multiplier values
- Trigger probabilities
- Feature parameters
- Free-spin counts
- Feature-specific probabilities

Game code should define how those values are used.

---

## Separation From Simulation

Game code must not be responsible for:

- Thread management.
- Simulation partitioning.
- Simulation scheduling.
- Global statistics aggregation.
- Report generation.
- Simulation orchestration.

Those responsibilities belong to the simulator/statistics framework.

The game should effectively expose a deterministic mathematical interface similar to:

    GameResult spin(GameContext context)

The simulator should be able to execute the game without knowing its internal implementation.

---

## Testing

Every new game should have tests covering at minimum:

- Symbol evaluation.
- Reel evaluation.
- Win calculation.
- Feature triggering.
- Feature behaviour.
- Multiplier behaviour.
- Edge cases.
- Result aggregation.

Where probabilities are involved, tests should verify the mathematical rules rather than relying solely on Monte Carlo results.

Monte Carlo simulations should be used as a secondary validation method.

---

## Mathematical Validation

After implementing a game:

1. Run a small deterministic simulation.
2. Confirm the game executes without errors.
3. Confirm Basegame results.
4. Confirm Feature Game triggering.
5. Confirm feature results.
6. Confirm win aggregation.
7. Confirm multiplier behaviour.
8. Run a sufficiently large simulation.
9. Investigate unexpected results before considering the game complete.

A simulation that completes successfully does not mean the game mathematics are correct.

---

## Changes To Common Libraries

Before modifying a common game component:

1. Determine whether the requirement is genuinely generic.
2. Check existing games that use the component.
3. Ensure the change will not alter existing game behaviour unintentionally.
4. Add or update tests.
5. Keep game-specific exceptions out of generic components where possible.

Do not modify common mathematics solely to make one game implementation easier.

---

## Coding Principles

Prefer:

- Small focused classes.
- Explicit mathematical behaviour.
- Composition over large game classes.
- Configuration-driven parameters.
- Reusable mathematical components.
- Strongly typed results.
- Deterministic randomness.
- Clear separation between Basegame and Features.

Avoid:

- Giant `Game.java` classes.
- Static global game state.
- Duplicated mathematical functions.
- Magic numbers.
- Game logic inside configuration files.
- Simulation logic inside games.
- Statistics aggregation inside games.
- Hidden random-number generation.
- Special cases added to common libraries for a single game.

---

## Adding Features

When adding a new feature:

1. Determine whether the mechanic already exists in the common library.
2. If it does, reuse it.
3. If it does not, determine whether the mechanic is generic enough to add to the common library.
4. If it is unique to the game, implement it within the game.
5. Add feature configuration.
6. Add feature result types where required.
7. Add deterministic tests.
8. Validate the feature independently.
9. Validate the interaction between the feature and Basegame.
10. Run a full simulation after integration.

A feature should be treated as an independent mathematical component rather than a collection of conditions added to the Basegame.

---

## Completion Criteria

A game is not considered complete merely because it compiles.

Before considering a game complete:

- The game compiles.
- Configuration loads correctly.
- Basegame executes correctly.
- Features execute correctly.
- Results are correctly structured.
- Common mathematics are reused appropriately.
- Tests pass.
- RNG behaviour is deterministic.
- A large simulation completes.
- No unexplained mathematical anomalies remain.
- Documentation is created regarding game logic , changes to Game Module Framework and other important details.