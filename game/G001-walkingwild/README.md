# New Game Workspace: walkingWild

**Game code:** `G001-walkingwild`
**Workspace folder:** `G001-walkingwild/`  
**Suggested Java package:** `game.walkingwild`

This wizard creates an intake workspace only. It does not create or register game code.

## Step 0: prepare and review the source material

1. Put the approved GDD, edge cases, flowchart, and PAR values in the matching files under `source/`. Keep source versions and dates in each document.
2. Complete `source/game-creation-form.md` alongside the GDD. Confirm stake units, evaluation rules, feature triggers/retriggers, win cap, and set behavior.
3. Review `source/G001-walkingWildsPAR.xlsx` as this game's PAR input. It contains separate Normal and BB1 Config/Reels pairs. Keep each mode's tables and reel strips isolated; a simulation selects one mode for the whole run. A mode-prefixed sheet pair follows `<Mode>-Config` and `<Mode>-Reels`; an unprefixed `Config`/`Reels` pair is interpreted as Normal.
4. Ask the agent to review the source package before writing game logic. The agent records conflicts, missing config, open questions, and proposed questions in `AGENT-REVIEW.md`.
5. Resolve the review items with the game owner. Do not invent balance values or silently choose between conflicting sources.

## After the review is agreed

Implement game-specific config and logic in this workspace, reuse `GameModuleFramework` where its behavior matches the rules, and keep Basegame and randomly triggered Feature Game logic separate. Add deterministic rule tests, register `G001-walkingwild` in `game/GameFactory.java`, and run a seeded simulation through the existing engine.

Check RTP by mode and set, triggers, hit rate, distributions, caps, and report assets. Document assumptions and validation evidence in the game handover. Update `docs/architecture.md` and `nextsteps.md` when the implementation changes the framework or completes a task.

## Workspace files

- `source/GDD.md`: game rules and approved math.
- `source/edge-cases.md`: required boundary behavior.
- `source/flowchart.md`: round and feature flow.
- `source/G001-walkingWildsPAR.xlsx`: mode-specific PAR input with Normal and BB1 Config/Reels sheets.
- `source/game-creation-form.md`: implementation intake checklist.
- `AGENT-REVIEW.md`: agent-authored questions, gaps, and proposals.
