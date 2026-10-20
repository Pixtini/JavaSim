---
name: javasim-validate-math
description: Validate JavaSim game math with source/config checks, deterministic tests, controlled seeded simulations, accounting checks, and reproducible reports. Use to investigate RTP, trigger frequency, distributions, caps, or mode costs; Monte Carlo execution alone is not mathematical approval.
---

# Validate game mathematics

Work from the repository root and read applicable `AGENTS.md` files. This workflow performs explicitly requested mathematical validation or checks relevant game/config/engine changes. Under the [root validation policy](../../../AGENTS.md), editing this skill, its documentation, or non-executable templates alone does not invoke game tests or simulations. Identify game, mode, workbook/config revision, question, and expected values. Read [execution and evidence guidance](references/execution.md); consult `docs/game-math-validation-flow.md` for the maintained checklist.

1. Check intended rules against config before interpreting results. Confirm payout/stake/cost/cap units, mode isolation, weights, feature eligibility, dimensions and legal inputs. Preserve config unless changes were authorized.
2. Compile and run deterministic tests for affected rules, invalid inputs, triggers/retriggers, state, accounting and caps. Inspect coverage rather than treating a passing suite as proof of every rule.
3. Use exact component evaluation only for supported games and tractable workloads. Explicitly record omitted mechanics: exhaustive Basegame reel stops do not establish full-game RTP with random insertions or Feature Games.
4. Run a small seeded simulation with explicit settings. The [temporary runner template](assets/SeededSimulation.java.template) avoids config edits. Inspect Basegame, Freegame, total and per-set results; winnings must reconcile and mode cost must affect the intended denominator.
5. Investigate systematic discrepancies before increasing run size. For noisy estimates, choose larger runs appropriate to event rarity and scope. Record expected values and uncertainty methods/assumptions. Do not invent a universal RTP tolerance or treat an absent rare trigger as proof of a defect.
6. To check reproducibility, hold seed, config, mode, stake, rounds and logical partitions fixed while varying worker count. Compare mathematical results with floating-point tolerances, excluding timing and paths. Changing partitions changes RNG streams.
7. Record evidence using [the validation template](assets/validation-report.md), normally in the game's workspace. Distinguish verified behavior, statistical evidence, unexplained differences and missing coverage. Follow architecture documentation completion requirements.

Do not silently rebalance PARs to obtain plausible output. Do not claim approval or complete validation with unresolved rules, required checks, or anomalies. Choose long runs and exhaustive workloads to serve the actual question rather than launching them by default.
