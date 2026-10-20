---
name: javasim-debug-replay
description: Reconstruct and diagnose JavaSim saved gameplay mismatches using recorded events, matching PAR and mode, and game-specific replay logic. Use for saved_gameplays.csv failures or replay regressions; general RTP analysis belongs to math validation.
---

# Diagnose saved gameplay

Work from the repository root and read applicable `AGENTS.md` files. Identify CSV, gameplay ID, expected behavior, game, and source revision. Read [replay contracts and commands](references/replay.md), then inspect actual artifacts and current code.

1. Preserve the original CSV and workbook. Verify the config and mode replay will load before interpreting mismatches. Keep one matching report workbook beside the CSV; report ambiguous/missing provenance rather than substituting an arbitrary current PAR.
2. Compile and reproduce with the real replay entry point. Capture the command and full failure. Distinguish malformed CSV, unsupported format/payload, missing game/config, unsupported replay and recalculated-win mismatch.
3. Locate the first divergent event: compare order, type, captured choices, state transitions, cap timing and wins. Inspect game-owned payload decoding and `replayRound`. Reconstruct from captured decisions rather than drawing fresh random choices.
4. Check completeness: mode/set, stops, insertions, destinations, multipliers and other game choices. Trace feature state across events. Distinguish capture defects, decoding/evaluation defects, rule/config changes and intentional incompatibility; do not automatically reinterpret historical payloads.
5. When a fix is authorized, make the smallest relevant correction and a deterministic regression using the saved sequence or minimal equivalent fixture. Handle version compatibility intentionally. Do not rewrite expected saved wins to make comparison pass. For diagnosis-only requests, report the fix without implementation changes.
6. After relevant game, replay, capture, config, or engine behaviour changes, run affected tests, replay the failing case, and run a small seeded simulation where needed to exercise that behaviour. Under the [root validation policy](../../../AGENTS.md), documentation or non-executable skill/template edits alone require content checks, not Java compilation, game tests, or simulations. For an explicit diagnosis-only request, reproduce the failing replay without automatically adding unrelated simulations. If capture changed, replay a newly captured round with its exported workbook. Use [the investigation template](assets/replay-investigation.md), follow architecture documentation requirements, and state missing coverage.

Without complete events the current mechanism cannot reconstruct the round. A seed alone cannot replace missing config, partitioning, session history and captured decisions.
