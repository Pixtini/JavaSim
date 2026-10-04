# Next steps

## 1. Cover GMF policy and multiplier variation

Add deterministic tests for custom `LineWinSelectionPolicy` implementations and candidate-specific multiplier functions. This protects the new reusable payline calculator while keeping each game’s award rule explicit at its configuration boundary.

## 2. Add game-specific report exports

Provide an optional expanding wild detail report containing stopped grids, expanded reels, banner multipliers, scatter counts, and the selected paylines that paid. Preserve the current summary and `showAwards` toggle alongside it.

## 3. Generate the game config from spreadsheet data

Build an importer that generates or refreshes the single `ExpandingWildConfig` source from spreadsheet tables for symbols, reel strips, paylines, paytable values, scatter-trigger settings, and weighted outcomes. Validate symbol IDs and report the source sheet, row, and column for invalid values.

## 4. Include weighted multiplier tables in run reports

Add the selected game's multiplier values and relative weights to its report metadata. This makes the odds used by a simulation visible beside its seed and other run settings.
