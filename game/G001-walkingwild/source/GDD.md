# walkingWild Game Design Document

**Owner:**
**Status/version/date:**
**Authoritative source:**

## Game overview

A game with a basegame and freegame, basegame is standard with no additional features, just 8 symbols 1 wild and 1 scatter symbol. Freegame wilds persist spin to spin and move to a different position than it was on before. 

There is also a bonus buy mode which costs the user a multiplier of base stake. The game rules are basically the same but the game is played using the new config and cost, the user can select what mode to play before each spin.

## Basegame

- Screen / reels: 6x4
- Type : Lines game
- Symbols and special behavior: N/A
- Feature trigger conditions: Scatters

Basegame features 8 Regular Symbols, 1 Scatter Symbol, 1 Wild Symbol

## Feature Games

Same as Base, however wilds persist spin to spin, and there are no scatters. 

- At the start of each Freegame spin, count the wilds persisted from the previous spin. Use the mode's 2D chance table to determine how many new wilds to add for that count.
- New wilds land with the reels. Persisted wilds remain in limbo above the reels while they spin.
- After reel 6 stops, move each persisted wild to a position selected using the same Freegame wild-location distribution. For the current placement sequence, use a temporary copy of the location weights and set the weight of each occupied wild position to 0. After placing a wild, set its destination weight to 0 before placing the next one. Do not change the source PAR weights.
- The configuration limits the total active wilds to 5. Configurations must not request more than 5 active wilds.
- Evaluate the spin after the new and moved wilds are in place, then preserve the wilds for the next Freegame spin.


## Decisions still requiring approval

There are no remaining open game-mechanic decisions in this document. See [`AGENT-REVIEW.md`](../AGENT-REVIEW.md) for implementation follow-ups and source metadata still to complete.

## Source references

- [G001-walkingWildsPAR.xlsx](G001-walkingWildsPAR.xlsx) — mode-specific Config/Reels, paytable, scatter awards, and wild quantity/location tables.
- [flowchart.pdf](flowchart.pdf) — source diagram for the Freegame wild transition sequence; transcribed and expanded in [flowchart.md](flowchart.md).
- [../../broadGDDs/lines.md](../../broadGDDs/lines.md) — shared line evaluation, stake, mode, and win-cap conventions.
