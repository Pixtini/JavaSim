# Balancing with recorded gameplay books

The standard authoring flow is **GDD → agent implements the game → human balances reel/config math → simulate and harvest complete replays → human balances replay weights → simulate the weighted book**. This adds a second outcome source to Simulation; game rules and replay reconstruction remain game-owned.

## Inputs per purchase mode

Keep the game's usual Config/Reels mode pair in its PAR. Add a sheet named `<Mode>-Books`, for example `Normal-Books`, `EM1-Books` or `BB1-Books`:

| Gameplay ID | Weight |
|---:|---:|
| 1 | 10 |
| 2 | 30 |
| 3 | 0 |

Headers belong in A1:B1. Each following consecutive row names one harvested **complete paid round**, including every awarded feature spin. IDs are positive integers; weights are nonnegative integers whose combined total fits a signed Java long. At least one weight must be positive. Zero disables an entry. Repeated IDs contribute their summed weight; repeating an ID three times with weight10 has the same probability as one row with weight30. Additional CSV gameplays absent from the positive-weight table are never selected. A positive-weight ID missing from the CSV is an error.

Store the original `saved_gameplays.csv` export beside the PAR under:

```text
source/
  PAR.xlsx
  books/
    saved_gameplays_normal.csv
    saved_gameplays_em1.csv
    saved_gameplays_bb1.csv
```

Names are matched without case sensitivity; G002's existing `source/Books/` directory is supported. Ambiguous case variants are rejected. The mode suffix is the mode name lowercased. Keep each harvest's matching mathematical PAR: recorded payouts are checked against recalculation before the book can run.

## Harvest and balance

1. Implement complete, versioned `ReplayEvent` capture and RNG-free `GameSession.replayRound` for new games. Existing games without replay cannot use books until they support that contract.
2. Run a regular simulation with **Use weighted gameplay books unchecked**, **Save report files checked**, and enough **Replay records to keep**. The capture setting retains the first N complete paid rounds, including losses; selecting only wins biases a book unless its weights deliberately compensate.
3. Copy that mode's `saved_gameplays.csv` to the mode-specific filename above. Keep its matching report PAR as provenance. Gameplay IDs are local to that CSV, not shared across modes or arbitrary harvests.
4. Populate the corresponding Books sheet and balance the weights. Do not edit recorded payouts or payloads to change returns. Changing reels, paytable, features or cap can invalidate a harvest; reharvest or restore the matching configuration when recalculation differs. Selection-only rule weights can differ if replayed outcomes still validate.
5. Select game, PAR and mode in the **Simulation** window. Enable **Use weighted gameplay books** and run. The runner preflights all active referenced gameplays before starting workers. Missing sheet, missing mode CSV, bad weights, unknown ID, wrong game/mode, incomplete events, payout mismatch or unsupported replay blocks the run; there is no fallback to fresh spins.

New wizard workspaces include an empty `Normal-Books` table and `source/books/` directory. Add a matching Books sheet/CSV for each other mode when harvested. Empty starter tables deliberately cannot run as books. Regular simulation remains available without Books inputs.

## Runtime and statistics

A book round draws one ID **with replacement**, using probability `combined weight / total weight`. It returns that recorded complete round only. No reel, set, feature-trigger, multiplier or placement choice is redrawn. Game-specific replay reconstructs and validates typed rounds once during preflight, then immutable outcomes are cached at the requested base stake. Runtime randomness only selects book entries through the existing seeded partition streams.

The standard statistics path still records base/free/total wins, hits, trigger counts, individual free-spin counts, per-set results, named awards, cap hits and distributions. Requested capture limits still apply to new saved-round output. Base stake scales reconstructed wins and cap; purchase cost only changes paid exposure. Holding seed, partitions, stake, book and configuration fixed reproduces results when worker count changes.

A book's expected return can be calculated directly:

```text
Expected RTP = Σ(weight_i / totalWeight × reconstructedRoundWin_i)
               / (baseStake × modeCost)
Expected feature probability = Σ(weight_i for triggered rounds) / totalWeight
```

This distribution is distinct from the original reel game's distribution. In particular, independently balancing Normal and EM1 Books does **not** automatically preserve the original fourfold EM1 trigger relation. If that remains a product requirement, the human balance must enforce it in the resulting books. A finite harvest also cannot represent outcomes it never captured; a high sampled return or absent rare outcome is not evidence about the unrestricted reel game.

## Reports and reproduction

Book runs print normal statistics and the Book CSV path. Logical partitions, the book toggle/sheet/counts/weights, finite-book return/trigger expectations and source hashes are omitted from console and text reports. These values remain in the run configuration/book metadata for internal validation and provenance checks.

When exported, each report retains the selected Config/Reels/**Books** sheets and `books/saved_gameplays_<mode>.csv`; HTML renders the selected book weights. These inputs are sufficient to run the same book again. Export refuses inputs that changed after preflight. `saved_gameplays.csv` beside the report workbook contains the newly sampled rounds, with new sequential run IDs; source-book IDs remain defined by the separate book CSV and Books table. The standard report replayer continues to recalculate captured outcomes against the exported PAR.

Programmatic use sets `SimConfig.useBooks = true` before invoking the usual `SimulationRunner`; it defaults to false. This option belongs to simulation orchestration, not individual Game implementations or statistics classes. Shared implementation lives in `simulation/books/`; CSV bulk loading and validation live in `simulation/replay/`.

G002's current book inputs and checks are recorded in its [book validation report](../game/G002-ember-forge/validation/books-2026-10-21/validation-report.md). The [original G002 validation](../game/G002-ember-forge/validation/validation-report.md) describes the earlier reel-based prototype and remains historical evidence for those inputs.
