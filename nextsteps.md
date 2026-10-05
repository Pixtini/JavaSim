# Next steps

## 1. Add cancellation to exhaustive runs

Allow a full reelset run to stop cleanly on user request and preserve the completed-combination count in a partial result. Exhaustive runs can include hundreds of millions of combinations, so interruption should not lose all progress already computed.

## 2. Add game-specific report exports

Add a CSV export for the full reelset award matrix, feature-trigger count, total winnings, and RTP. This lets large exact-run results be inspected and compared without relying on console output.

## 3. Generate the game config from spreadsheet data

Build an importer that generates or refreshes the single `ExpandingWildConfig` source from spreadsheet tables for symbols, reel strips, paylines, paytable values, scatter-trigger settings, and weighted outcomes. Validate symbol IDs and report the source sheet, row, and column for invalid values.

## 4. Include weighted multiplier tables in run reports

Add the selected game's multiplier values and relative weights to its report metadata. This makes the odds used by a simulation visible beside its seed and other run settings.
