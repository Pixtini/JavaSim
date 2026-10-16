# Edge Cases and Expected Behavior

Record expected outcomes from the approved rules. Add a source reference for each case.

| ID | Situation / setup | Expected behavior | Source reference | Test status |
|---|---|---|---|---|
| EC-001 | No win on the basegame | | | Not tested |
| EC-002 | Minimum winning outcome | | | Not tested |
| EC-003 | Multiple simultaneous wins | | | Not tested |
| EC-004 | Feature trigger at the exact threshold | | | Not tested |
| EC-005 | Feature retrigger or retrigger is disallowed | | | Not tested |
| EC-006 | Win reaches or exceeds the round cap | | | Not tested |
| EC-007 | Zero-weight or unavailable configuration choice | | | Not tested |
| EC-008 | Feature outcome has no win | | | Not tested |

## Game-specific cases

| ID | Situation / setup | Expected behavior | Source reference | Test status |
|---|---|---|---|---|
| EC-009 | Freegame spin with persisted and newly added wilds | Determine added wild count from the mode's 2D table. New wilds land with the reels; persisted wilds move after reel 6 stops using a temporary copy of the Freegame location weights. Set occupied cells to zero for the placement sequence, including destinations selected earlier in the sequence; leave source PAR weights unchanged. | `GDD.md`, Feature Games; `flowchart.md`; PAR `FG-0-wild-dependancyInsertQuantity` and `FG-0-wild-location` | Not tested |
| EC-010 | PAR configuration could produce more than 5 active wilds | Reject the invalid configuration before simulation; the configured quantity table must keep total active wilds at or below 5. | `GDD.md`, Feature Games; PAR `FG-0-wild-dependancyInsertQuantity` | Not tested |
