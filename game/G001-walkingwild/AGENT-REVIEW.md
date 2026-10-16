# Agent Review Log

This review reflects the current G001 sources and PAR, checked against the latest shared line-game rules. It separates resolved conventions from game-specific mechanics and implementation follow-ups.

## Review status

- Status: **G001 engine and PAR parser implemented; seeded Normal and BB1 reports generated; source metadata and balance approval remain outstanding**
- Sources reviewed: `source/GDD.md`, `source/edge-cases.md`, `source/flowchart.md`, `source/flowchart.pdf`, `source/game-creation-form.md`, `README.md`, `game/broadGDDs/lines.md`, and `source/G001-walkingWildsPAR.xlsx`
- PAR sheets: `Normal-Config`, `Normal-Reels`, `BB1-Config`, and `BB1-Reels`
- Review date: 2026-10-16

## Review summary

The GDD and PAR define a 6-reel × 4-row line game with eight regular symbols, a wild, a scatter, a basegame, a freegame, and a bonus-buy mode. The PAR provides 20 paylines, awards for 3–6 matches, reel strips for BG sets 0 and 1 and FG set 0, set weights, scatter tables, and a freegame wild quantity/location table. Both modes set `maxWin=100`; Normal costs 1 unit and BB1 costs 100 units.

The revised `lines.md` now resolves the general line evaluation rules, including wild substitution and wild-only wins. When multiple symbol runs can score on one line, the line pays the single highest available award. It also states that modes are selected before a spin, the default is Normal, each round begins in the basegame, the selected mode supplies its configuration, and a max-win cap clips the winning spin and ends remaining freegames. Earlier questions about payout basis, line selection, BB1 cost/mode selection, 6-symbol awards, and cap behavior are therefore closed.

The PDF and the owner's answers now define the Freegame wild sequence: use the 2D chance table to determine new wild count; new wilds land with the reels; persisted wilds stay in limbo while reels spin; after reel 6 stops, move persisted wilds using the same location distribution. For this placement sequence, occupied cells—including new wilds and persisted wilds already placed—have their weights set to zero in a temporary copy of the location table; source PAR weights remain unchanged. The configuration caps the total at 5 active wilds, preventing the quantity table from requesting more wilds than can be placed. Both prior wild placement questions are resolved.

The mode-prefixed parser and run-level mode behavior are shared infrastructure. G001's parser and engine are in this workspace under `src/main/java/game/walkingwild/`, with tests under `src/test/java/game/walkingwild/tests/`. G001 is registered in `GameFactory` and can be simulated through the GUI and CLI. Its parser loads only the selected mode's Config/Reels pair.

The Normal PAR gives BG-0 a set-selection weight of 3 and BG-1 a weight of 1, while scatter insertion has positive weights only for BG-1. This means scatter triggers are possible only when BG-1 is selected. The shared rules explicitly allow features to be enabled/disabled by set weights, so this is a valid configuration pattern; it is a balance choice to verify, not a conflict with the general rules.

## Resolved owner clarifications

| ID | Question | Why it matters | Source / location | Proposed answer or options | Owner answer | Status |
|---|---|---|---|---|---|---|
| Q-001 | How are destinations selected for persisted wilds that move after reel 6 stops? | The PDF establishes movement after the reels stop; owner confirms the same location distribution is used and wild positions cannot overlap. | `source/flowchart.pdf`, `source/flowchart.md`; PAR `FG-0-wild-location` | Use the same location distribution; set cells occupied by landed wilds and persisted wilds already placed to zero in a temporary weight table. | Same locations; do not place over a landed wild or another persisted wild. Set excluded cells' weights to 0 temporarily. | Resolved |
| Q-002 | What happens if there are not enough eligible positions to place all persisted and newly landed wilds? | The PDF forbids overlapping wilds but did not specify a limit or fallback. | `source/flowchart.pdf`; PAR `FG-0-wild-location` and `FG-0-wild-dependancyInsertQuantity` | Limit the total active wilds to 5 in configuration so the table cannot request an unplaceable total. | Prevented in config; maximum 5 at a time. | Resolved |

## Implementation follow-ups

| ID | Item | Finding | Follow-up | Status |
|---|---|---|---|---|
| I-001 | Mode-specific workbook sheets and simulation selection | Owner specifies matching `<Mode>-Config`/`<Mode>-Reels` pairs, no cross-mode merging, one selected mode for the whole simulation, and unprefixed sheets defaulting to Normal. | Generic mode selection and selected-pair reporting are implemented; G001 has its own validated parser and engine. Both modes have seeded 1,000,000-round report artifacts under `reports/G001-walkingwild/`. | Implemented and simulated; balance review pending |
| I-002 | Scatter/set balance | Normal mode can trigger scatters only from BG-1; BB1's set selector gives BG-0 weight zero and selects BG-1. Scatter awards are 4→5, 5→8, and 6→12 freegames; the scatter has zero paytable awards. | Preserve this mapping and confirm it in the eventual seeded balance review. No rule clarification is needed unless the owner intended scatters in BG-0. | Config observation |
| I-003 | Flowchart and edge-case source material | The PDF supplies the freegame wild sequence; owner has clarified destination exclusions and the maximum wild count. | Converted the PDF into `source/flowchart.md`; added confirmed wild rules to the GDD and wild-specific cases to `source/edge-cases.md`. Remaining general edge cases and game creation form content should be completed as source metadata is approved. | Core game rules implemented; intake documents incomplete |
| I-004 | README PAR instructions | Updated to describe the populated Normal/BB1 sheet pairs and mode isolation. | Keep the README aligned if the workbook mode layout changes. | Resolved |
| I-005 | GDD source metadata | Owner, status/version/date, authoritative source, and source reference remain blank. | Fill these fields before treating the GDD as the approved handover source. | Documentation incomplete |

## Resolved by the shared line-game rules and current PAR

- Reel stop selection, top-row display, and wraparound at the end of a reel.
- Winline coordinates are 0-based screen rows, with one position per reel and consecutive reels unless stated otherwise.
- All winlines pay independently and may overlap.
- Wilds substitute for regular symbols and can form their own wins. If multiple symbol runs qualify on one line, only the highest award on that line is paid.
- Payouts use the base unit even when a mode costs a multiple of the base stake; wins are in 0.05-unit increments.
- Mode is selected before the spin; Normal is default; a round begins in the Basegame and uses the selected mode's configuration.
- The PAR specifies 6-symbol awards, `maxWin=100` for both modes, and mode costs of 1 (Normal) and 100 (BB1). The common cap rule clips the winning spin and stops remaining freegames.
- Freegame wilds use the PAR's location distribution; exclude landed wilds and persisted wilds already placed. Configuration limits total active wilds to 5.
- Set selection occurs at the start of each spin; a set may disable features through zero weights.
- Math targets are not required in the GDD; balance approval is a separate activity.

## Mode selection decision

- A workbook can define each mode with a matching `<Mode>-Config` and `<Mode>-Reels` pair.
- A legacy unprefixed `Config`/`Reels` pair is the `Normal` mode.
- Prefixed mode pairs remain isolated; a simulation selects exactly one mode before it starts and every round uses only that mode's pair.
- If no mode is selected, use `Normal`. If the requested mode has no complete pair, fail validation before simulation.
- The simulation interface/config and report metadata include the selected mode. Generic mode selection is implemented for Expanding Wild; the G001 game-specific parser and engine remain pending.

## Proposed validation for implementation

- Test each symbol award and line-level highest-award selection, including wild substitution and wild-only lines.
- Test scatter counts 4, 5, and 6; verify counts below 4 do not trigger and scatters do not appear in freegames.
- Test freegame wild persistence, movement order, insertion, destination exclusions, and the configured five-wild maximum using deterministic random choices.
- Test mode selection, basegame start, cost/config association, and max-win clipping/termination.
- Run a seeded simulation and inspect RTP and trigger frequency by mode and set.

## Implementation and seeded simulation results

- Deterministic/parser/report/player/viewer/replay checks: 56 assertions in `src/test/java/game/walkingwild/tests/WalkingWildGameTests.java`.
- Normal: 1,000,000 rounds, seed `1001001`, RTP `42.3257%`, 2,474 Freegame triggers, report at `reports/G001-walkingwild/Normal/`.
- BB1: 1,000,000 rounds, seed `1001002`, RTP `1.2157%` against its 100× purchase cost, 500,367 Freegame triggers, report at `reports/G001-walkingwild/BB1/`.
- BB1's configured max win remains 100× base stake; the cost multiplier changes stake exposure only. Review these PAR-driven results for balance intent before treating them as approved targets.
- Framework changes: a game-provided mode cost is now carried into stake exposure and report metadata, and total-game stats accumulate one round result per simulated round. See root `G001-LESSONS-LEARNED.md`.
- Presentation/replay: G001 outcomes expose the grid and selected line awards to the Game launcher and Win viewer. Reports now include G001 event payloads; replay rebuilds screens from recorded stops and placements, re-evaluates wins, and passes validation for both modes.

## Decisions and change history

| Date | Review update | Impact |
|---|---|---|
| 2026-10-16 | Recorded the owner's answers on freegame wild destination selection and maximum active wild count. | Closed Q-001 and Q-002; updated the GDD, flowchart, and edge-case source with same-distribution placement, occupied-cell exclusions, and a five-wild cap. |
| 2026-10-16 | Clarified how excluded cell weights are handled during wild placement. | Updated the GDD, flowchart, edge case, and Q-001 answer: zero occupied-cell weights in a temporary table for the current placement sequence; keep source PAR weights unchanged. |
| 2026-10-16 | Converted `source/flowchart.pdf` into a full Mermaid round flow and re-reviewed it against the GDD, PAR, and shared line rules. | Confirmed the wild move sequence. At this point destination allocation and exhausted-position fallback remained open; the owner resolved these in a later update recorded above. |
| 2026-10-16 | Re-reviewed G001 after updates to `game/broadGDDs/lines.md` and the GDD. | Closed generic wild/pay selection, mode-selection, cost, payout, six-symbol award, and cap questions. At that point persistent wild movement remained unresolved; it has since been clarified. Parser sheet mapping and source-template completion remain implementation/documentation follow-ups. |
| 2026-10-16 | Recorded the owner's mode-prefixed sheet and simulation selection rules. | Mode sheet parsing and run-level selection are no longer open design questions; implementation must keep each mode isolated and default to Normal. |
| 2026-10-16 | Implemented G001 from its selected mode PAR, added deterministic rule/report tests, and ran seeded million-round Normal and BB1 simulations. | Confirmed scatter insertion, scatter-triggered Freegames, persistent wild placement, mode cost accounting, report export, and the configured cap. Results are recorded above; source metadata and balance approval remain open. |
| 2026-10-16 | Added G001 to the Game launcher and detailed Win Viewer; added recorded-stop/placement replay payloads and report replay validation. | Both modes now share the player/viewer/replay workflow used by Expanding Wild. Replay reconstructs screens from recorded reel stops and feature placements, then recalculates the payouts. |
