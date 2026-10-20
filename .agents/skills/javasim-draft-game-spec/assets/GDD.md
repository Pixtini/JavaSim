# Game Design Document — {{game name}}

Status: proposed fallback design; owner review and math approval pending.
Date/revision: {{date/version}}. Draft location/ID: {{path; unallocated if new}}.
Research rationale: [research-rationale.md](research-rationale.md).
Shared source: {{actual broad GDD path/revision; repair links for destination}}.

## Concept and scope

Player hook: {{one sentence}}.
Familiar foundation and modest differentiator: {{explain}}.
Basegame: one. Freegame: one. Purchase modes: Normal, EM1, BB1.
Basegame sets: {{1–3}}. Freegame sets: {{1–3}}.
Regular types: {{0–10}}. Special types: {{0–5}}.
Excluded ideas: {{brief list with reasons; no second bonus}}.

## Rules

Use stable IDs such as G-01, BG-01, FG-01, M-01 and CAP-01.

| Rule ID | Setting / behavior | Concrete proposed rule | Source / design decision |
|---|---|---|---|
| G-01 | Base stake, payout increments, dimensions, coordinates | {{values}} | {{reference}} |
| G-02 | Paylines, direction, minimum matches and per-line award selection | {{explicit lines or linked table}} | {{reference}} |
| BG-01 | Set selection, reel stops and wraparound | {{ordered rules}} | {{reference}} |
| BG-02 | Transformations, line evaluation and trigger/cap ordering | {{ordered rules}} | {{reference}} |
| FG-01 | Trigger threshold / award map / initial state | {{one Freegame}} | {{reference}} |
| FG-02 | Per-spin sequence, persistence, consumption and reset | {{ordered rules}} | {{reference}} |
| FG-03 | Retrigger or explicit absence; termination | {{same Freegame only}} | {{reference}} |
| M-01 | Normal / EM1 / BB1 cost and entry | {{see math design}} | {{reference}} |
| CAP-01 | Combined round cap and clipping/termination order | {{base-stake units}} | {{reference}} |

## Symbol inventory

| ID / name | Regular or special | Where used | Paytable / substitution / trigger behavior |
|---|---|---|---|
| {{stable ID}} | {{class}} | {{BG / FG / both}} | {{explicit role}} |

Count types over the whole game. Describe any wild's own pays, scatter pay and exclusion rules. Define proposed paytable amounts separately from approved/balanced values.

## Round and presentation

One ordered Basegame sequence followed, when triggered, by the single Freegame loop and settlement. Name all random draws, transient/persistent state, resets and player-visible events. Follow [flowchart.md](flowchart.md); no hidden bonus branches.

## Companion documents and readiness

- [Math/config design](math-design.md)
- [Edge cases](edge-cases.md)
- [Scope review](scope-review.md)
- [Creation form](game-creation-form.md)

Structural decisions: {{resolved / proposed}}.
Unresolved inputs: {{named values or owner decisions and impact}}.
No implementation, test execution or mathematical approval is implied.

