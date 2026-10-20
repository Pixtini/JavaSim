# Edge cases and future deterministic checks — {{game name}}

Status: expected scenarios only; tests have not been implemented or run.

| ID / GDD rule | Mode and controlled inputs | Expected outcome / ordering | Parameter dependency |
|---|---|---|---|
| EC-01 / reel rule | Stop at final reel index | {{wrapped visible positions}} | {{strip/window}} |
| EC-02 / line rule | Wild-only candidate competes with a longer substituted match | {{highest final award on that line; no double count}} | {{proposed paytable}} |
| EC-03 / trigger | Just below, exactly at and above the trigger threshold | {{awards; one FG definition}} | {{scatter map}} |
| EC-04 / ordering | Simultaneous line/scatter/feature events | {{exact order and permitted combined pays}} | {{rules}} |
| EC-05 / FG reset | Last free spin, and next paid round | {{state consumption and reset}} | {{state model}} |
| EC-06 / cap | BG reaches cap; FG reaches or exceeds cap | {{clip winning spin and terminate remaining feature}} | {{cap}} |
| EC-07 / purchase units | Same base stake in Normal, EM1 and BB1 | {{one correct charge; awards/cap stay in base units}} | {{buy price}} |
| EC-08 / EM1 | Complete Normal and EM1 trigger distributions | {{analytic verification of exactly fourfold entry probability; not a single-spin test}} | {{final weights/strips}} |
| EC-09 / BB1 | Every possible entry choice | {{guaranteed legal entry to same FG; specified BG payout}} | {{buy entry law}} |
| EC-10 / invalid config | Zero selector totals, bad IDs, fourth BG/FG set, symbol-budget excess | {{clear rejection or draft scope failure}} | {{schema}} |

Add only scenarios needed for included features: collisions, multiplier order, retrigger boundary or impossible placement. Use explicit fixture inputs and arithmetic outcomes when values exist; otherwise name the unresolved input. Do not report a planned check as executed evidence.

