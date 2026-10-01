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

## Java

- Use modern Java conventions.

- Prefer clear, explicit code over clever abstractions.

- Use descriptive variable names.

- Avoid unnecessary dependencies.

- Keep classes small and focused.

- Use packages consistently.

- Optimise for time complexity where possible. 

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