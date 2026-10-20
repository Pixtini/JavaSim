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

Choose validation from the behaviour affected by the current task. Existing unrelated changes in the working tree do not justify additional testing.

- **Documentation, instructions, research, and non-executable skill/template changes:** check the changed content, local links, and relevant formats or skill metadata. Do not compile Java, run game/engine test suites, or launch simulations for these changes alone. This also applies to documentation inside a game workspace.
- **Game, mathematical configuration/PAR, parser, shared game math, result contract, or simulation engine changes:** compile the project, run the affected deterministic and integration tests, and run a small seeded simulation of the affected game/mode. Include existing consumers when shared behaviour changes; do not default to G001 for unrelated work.
- **Other executable changes, including toolkit, reporting, build scripts, and runnable skill helpers:** compile or check the affected code as appropriate and run focused tests for that behaviour. Include game/engine tests or a simulation only when that behaviour is affected or executing the changed helper requires it.
- **Explicit validation or debugging requests:** run the requested relevant checks even when no implementation changes are needed. A request to research the slot market or edit a skill is not a request to validate game maths.

Report the checks performed and any applicable checks that could not run. Checks outside the task's scope are not required and should not be reported as failures or blockers. Do not add tests that merely repeat documentation wording.

## Important

Do not redesign existing architecture unless the task requires it.

Do not modify unrelated files.

Do not make lasting edits to any game config unless specifically asked to do so. You may make changes to test but must revert.

Before implementing a significant architectural change, explain the proposed approach first.

Any task is not finished until Architecture.md has been updated.

Remember at all time, that this is meant to act as the simulator for a slot game. Any creation within the simulator itself must respect that new games will be added with a Basegame and Randomly triggered FG modes.
