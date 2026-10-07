# Game Creation Form

Complete this form before implementing a game, then update it as decisions are confirmed. It captures the inputs, implementation work, and evidence needed to add a game to the current simulator.

For the workflow and code reference points, see [game-creation-flow.md](game-creation-flow.md). For mathematical review and simulation checks, see [game-math-validation-flow.md](game-math-validation-flow.md).

## 1. Game identity and source material

- **Game name:** [ ]
- **Stable game ID** (lowercase, used by `GameFactory`): [ ]
- **Owner / source of rules:** [ ]
- **Rules or math specification link/file/version:** [ ]
- **Target package:** `game/[game-name]/`
- **Reference game reviewed:** [ ]
- **Open questions requiring math approval:**
  - [ ]

## 2. Rules and math definition

### Shared game settings

- **Currency / win units:** [ ]
- **Stake definition and scaling basis:** [ ]
- **Win cap:** [ ] (include units and whether the cap truncates a spin or stops the feature)
- **Rounding rules:** [ ]
- **Other game-wide rules:** [ ]

### Symbols and screen

- **Symbols and stable IDs:** [ ]
- **Grid/reel dimensions:** [ ]
- **Reel strips or stop weights:** [ ]
- **Symbols with special behavior:** [ ]
- **Symbol substitution / exclusion rules:** [ ]

### Win evaluation

- **Evaluation type:** [ ] paylines / ways / clusters / scatter / other: [ ]
- **Paylines, ways rules, or cluster rules:** [ ]
- **Paytable and paytable units:** [ ]
- **Win selection / combination rules:** [ ]
- **Multiplier rules and order of application:** [ ]
- **Overlapping wins and aggregation rules:** [ ]

### Basegame and randomly triggered feature games

- **Basegame outcome generation:** [ ]
- **Feature trigger condition and probability mechanism:** [ ]
- **Feature award/count:** [ ]
- **Retrigger rules:** [ ]
- **Freegame stake basis:** [ ]
- **Feature modes and their outcome generation:** [ ]
- **Feature completion and return-to-round behavior:** [ ]
- **Other randomly triggered modes:** [ ]
- **Any non-random trigger exception and specification reference:** [ ]

## 3. Configuration plan

Configuration belongs to the game module. List game-wide values and mode-specific settings. If there are selectable reel/config sets, document the set selector weights and each set's contents separately.

| Mode | Set ID | Selection weight | Reels / weights | Feature settings | Notes |
|---|---|---:|---|---|---|
| Basegame | [ ] | [ ] | [ ] | [ ] | [ ] |
| Freegame / feature | [ ] | [ ] | [ ] | [ ] | [ ] |

- [ ] Symbols and IDs defined.
- [ ] Paytable, paylines/evaluation settings, and win cap defined.
- [ ] Basegame set selector and per-set spin config defined, where applicable.
- [ ] Feature set selector and per-set spin config defined, where applicable.
- [ ] Feature weights, awards, retriggers, and multipliers defined.
- [ ] Config validation covers dimensions, IDs/indexes, positive/valid weights, and mode-specific constraints.
- [ ] Invalid configuration fails before simulation starts with a useful error.

## 4. Reusable math and implementation boundaries

- **Existing GMF components that fit:** [ ]
- **New generic calculation candidates:** [ ]
- **Game-specific rules/helpers:** [ ]
- **Existing common components affected (if any):** [ ]
- **Existing games checked for regression impact:** [ ]
- **Reason a common-library change is needed, if proposed:** [ ]
- [ ] Keep game math and round orchestration in this game module.
- [ ] Keep simulation scheduling, threads, global statistics, and reporting in the framework.
- [ ] Use the RNG supplied through the game/session contract; no hidden RNG or `Math.random()`.
- [ ] Same seed and config reproduce the same game outcomes under the framework's partitioning rules.

## 5. Game contract and results

- **Game implementation:** [ ] implements `game.Game`.
- **Session implementation:** [ ] implements `game.GameSession` and owns the supplied random stream.
- **One-round behavior:** [ ] one basegame spin, then any triggered feature spins, returned as `GameRoundResult`.
- **Base result type/details:** [ ]
- **Feature result details:** [ ]
- **Named awards / paytable buckets:** [ ]
- **Basegame and feature set counts/metadata:** [ ]
- **Max-win multiplier and cap behavior:** [ ]
- **Round stake passed consistently to basegame and feature evaluation:** [ ]
- **Result fields needed by statistics/reporting:** [ ]
- [ ] Results remain game outcomes; generic aggregation stays in the statistics framework.

## 6. Integration

- [ ] Create the dedicated package and game-specific config.
- [ ] Keep Basegame and Feature Game logic clearly separated.
- [ ] Implement reusable GMF calculations where they have a clear generic contract; otherwise keep unique rules in the game module.
- [ ] Register the stable game ID and constructor in `game/GameFactory.java`.
- [ ] Confirm CLI and GUI game selection can create the game.
- [ ] Confirm the game runs through the existing `Game` / `GameSession` contract without game-specific engine or statistics changes.
- [ ] If a framework capability appears necessary, document the proposal; do not silently implement an unrelated framework redesign.

## 7. Deterministic tests

Record test file(s): [ ]

- [ ] Config validation: valid and invalid cases.
- [ ] Symbol/reel/grid evaluation and boundary cases.
- [ ] Win calculation, stake scaling, multipliers, and aggregation.
- [ ] Feature trigger conditions and awards using controlled random outcomes.
- [ ] Feature behavior, retriggers, and feature-to-round return.
- [ ] Win cap behavior, including stop/truncation semantics.
- [ ] Set selection and mode-specific settings, if applicable.
- [ ] Structured result fields and named awards.
- [ ] Same-seed repeatability.
- [ ] Seeded simulation accounting; worker-count reproducibility with a fixed partition count where applicable.
- [ ] Existing game/framework tests still pass after any shared-component change.

## 8. Replay and optional tooling

- **Saved replay required?** [ ] Yes / [ ] No
- **If yes, versioned payload and decisions to record:** [ ]
- [ ] Implement replay recalculation and compare basegame, feature, and total outcomes.
- [ ] Add a viewer/player adapter only if the game needs visual inspection or play support.
- [ ] Add exhaustive reelset support only if the game implements the required interface and its assumptions fit.

## 9. Validation evidence

Record commands, settings, and outcomes. A successful run confirms execution; mathematical correctness also requires checking expected rules and distributions.

- **Compile command/result:** [ ]
- **Relevant test command/result:** [ ]
- **Small deterministic run settings/result:** [ ]
- **Feature-trigger path observed:** [ ]
- **Seeded simulation settings (rounds, stake, seed, threads, partitions):** [ ]
- **Expected vs observed RTP / trigger frequency / win frequency:** [ ]
- **Distribution or cap checks:** [ ]
- **Report export checked:** [ ]
- **Unexplained differences or remaining limitations:** [ ]
- **Large simulation command/result, when completing a game:** [ ]

## 10. Documentation and completion

- **Game-specific rules/config document:** [ ]
- [ ] Document rules, config structure, assumptions, known limits, and test/validation evidence.
- [ ] Document any suggested framework/GMF changes without implementing them automatically.
- [ ] Update [`docs/architecture.md`](architecture.md) if the architecture or supported game behavior changed.
- [ ] Replace the previous entry in [`nextsteps.md`](../nextsteps.md) with one clear suggestion under 100 words.
- [ ] Review unrelated config and files to ensure they were not changed.

**Status:** [ ] Not started / [ ] In progress / [ ] Ready for review / [ ] Complete

**Reviewer / date:** [ ]
