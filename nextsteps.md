# Next steps

## 1. Add focused automated tests

This is the most effective next change. Test `standardStats` calculations, win-band boundaries, and merging of worker-local statistics. Confirm that fixed partitions give identical results across thread counts with the same seed, that fresh-seed mode records the effective seed, and that reports contain the merged totals.

## 2. Validate simulation configuration and distribution ranges

- Reject invalid round counts and stakes before starting a run.
- Check that the paytable has a bucket for every possible win-size index.
- Ensure the final aggregate band covers all possible wins; it currently ends at 10,000,000.
- Define RTP behavior when there are zero rounds.

## 3. Benchmark large simulations

Measure throughput and memory at increasing round counts, including 100 million rounds. Tune worker and partition defaults from those results, and profile per-spin allocations before optimizing them.

## 4. Clean up naming and unused code

As files are next touched, adopt Java class naming conventions (`StandardStats`, `Print`, `GameBasic`, and `SpinResult`) and remove unused imports and fields. Coordinate renames so file names and callers stay aligned.
