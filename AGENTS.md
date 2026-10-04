# Project Instructions

## Project

This is a Java-based Monte Carlo simulation framework for iGaming mathematics.

The framework should eventually support:

- Game-specific spin logic

- Monte Carlo simulation

- RTP and volatility analysis

- Win-frequency analysis

- Distribution analysis

- Bonus-buy simulations

- Parallel/multithreaded simulation

- Configurable simulation parameters

- Reusable statistics collection

## Architecture

Keep these concerns separate:

- Game logic

- Simulation engine

- Result types

- Statistics

- Configuration

- Reporting

Game implementations should not contain generic simulation or statistics logic.

Simulation implementations should not contain generic Game logic.

## Java

- Use modern Java conventions.

- Prefer clear, explicit code over clever abstractions.

- Use descriptive variable names.

- Avoid unnecessary dependencies.

- Keep classes small and focused.

- Use packages consistently.

- Optimise for time complexity where possible. 

- Comment code in a professional manner while maintaining readability.

## Simulation

- Use `long` for spin counts.

- Avoid storing every spin result unless explicitly required.

- Statistics should be accumulated incrementally.

- Never use floating-point equality for statistical comparisons.

- Make random number generation explicit and controllable where practical.

## Validation

After making changes:

1. Compile the project.

2. Run relevant tests.

3. Run a small simulation to verify behaviour.

4. Report any tests or validation that could not be run.

## Important

Do not redesign existing architecture unless the task requires it.

Do not modify unrelated files.

Before implementing a significant architectural change, explain the proposed approach first.

Any task is not finished until Architecture.md has been updated.

Any task is not finished until nextsteps.md has been updated with a suggestion of the most effective next change within less than 100 words. 

Any change that was previously listed in nextsteps.md must be cleared for clarity and adding another suggestion.

Remember at all time, that this is meant to act as the simulator for a slot game. Any creation within the simulator itself must respect that new games will be added with a Basegame and Randomly triggered FG modes.