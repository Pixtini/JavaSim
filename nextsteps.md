# Next steps

## 1. Capture per-line paytable awards in statistics

The expanding wild game records detailed per-line wins in its result, but generic stats reduce each spin to one loss/win bucket. Add a path for multiple line awards and game-specific result details. Also fix total-game standard deviation to use total-game outcomes and calculate freegame RTP against basegame stake.

## 2. Define line stake and RTP conventions

Decide whether configured paytable values are credits per line or multiples of total round stake, then pass the relevant stake context to games or represent it in game configuration. Report freegame contribution against basegame stake.

## 3. Add game-specific report exports

Provide an optional expanding wild detail report containing stopped grids, expanded reels, banner multipliers, scatter counts, and the selected paylines that paid. Preserve the current generic summary alongside it.

## 4. Generate the game config from spreadsheet data

Build an importer that generates or refreshes the single `ExpandingWildConfig` source from spreadsheet tables for symbols, reel strips, paylines, paytable values, scatter-trigger settings, and weighted outcomes. Validate symbol IDs and report the source sheet, row, and column for invalid values.

## 5. Include weighted multiplier tables in run reports

Add the selected game's multiplier values and relative weights to its report metadata. This makes the odds used by a simulation visible beside its seed and other run settings.
