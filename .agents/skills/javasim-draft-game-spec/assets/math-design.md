# Math and configuration design — {{game name}}

Status: proposed inputs; no achieved RTP, balanced PAR or verified mode ratio claimed.

## Units and purchase modes

Base stake `s`: {{definition}}. Payout increment: at least 0.05 base units.
Combined round cap: {{proposed value/units and GDD rule}}.

| Purchase mode | Cost | Basegame entry | Freegame rules / entry awards | Parameter status |
|---|---|---|---|---|
| Normal | 1× base stake | Random trigger, `pN` | {{single FG reference}} | {{proposed / unresolved}} |
| EM1 | 2× base stake | `pEM1 = 4pN` per paid round | Same conditional FG law as Normal | {{derivation pending / evidence}} |
| BB1 | `C_BB` × base stake | Guaranteed legal trigger/entry Basegame | Same FG; specify BG pays and initialization | {{proposed buy price; not inherited}} |

Check `0 < pN <= 0.25`; state complete trigger construction, accidental-trigger/collision handling, set weights and any eligibility/cap effects. Explain why the proposed mechanism can meet the exact EM1 relation. Weight multiplication alone is not proof. Record work needed to parameterize and verify it.

## Set inventory

| Gameplay state | Unique set ID / profile | Reels / feature inputs | Purchase modes using it | Selector / override and counting rationale |
|---|---|---|---|---|
| BG | {{ID}} | {{definition}} | {{Normal / EM1 / BB1}} | {{explicit}} |
| FG | {{ID}} | {{definition}} | {{all three; same FG}} | {{explicit}} |

Count distinct definitions globally: BG {{1–3}}, FG {{1–3}}. Resolve aliases and mode-specific variants honestly.

## PAR schema and open inputs

Describe required tables, reel dimensions, paytable, symbol IDs, trigger/award map, set selectors and any feature weights. Use isolated Normal/EM1/BB1 Config/Reels pairs if proposing a workbook; do not fabricate a populated workbook or combine modes.

| Input / table | Units and constraints | Proposed value or unresolved symbol | Evidence / approval needed |
|---|---|---|---|
| {{name}} | {{constraints}} | {{value/status}} | {{next check}} |

List optional user-supplied balance goals separately from approved parameters. Define later deterministic checks and suitable mathematical verification for each mode. No simulation is run for this documentation stage.

