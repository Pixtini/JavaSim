# Source routing and review checks

Paths are relative to this file; this skill depends on the JavaSim repository.

- [Root instructions](../../../../AGENTS.md) and [game instructions](../../../../game/AGENTS.md): boundaries, configuration protection, and completion requirements.
- [Creation form](../../../../docs/game-creation-form.md): canonical intake checklist; do not maintain another copy.
- [Creation workflow](../../../../docs/game-creation-flow.md): current implementation expectations and package layout.
- [Shared line rules](../../../../game/broadGDDs/lines.md): use for line games, respecting approved game exceptions.
- [G001 lessons](../../../../docs/lessons/G001-LESSONS-LEARNED.md): units, feature transitions, accounting, and evidence.
- [G001 review](../../../../game/G001-walkingwild/AGENT-REVIEW.md): example of findings and owner decisions, not rules for another game.
- [Workbook reader](../../../../toolkit/par/ExcelWorkbookReader.java) and [mode discovery](../../../../toolkit/par/ParWorkbookModes.java): inspection infrastructure. Generic reading does not validate every game's schema.

## Review focus

- Establish stake, payout, purchase-cost, rounding, and cap units separately. G001's BB1 cost is not a default for another game.
- Check symbols/IDs, dimensions, reel wrapping, coordinates, evaluation rules, wild substitution, simultaneous wins, and multiplier order.
- Trace one complete Basegame round and each randomly triggered Feature Game: thresholds, awards, retriggers, persistent state, placement/movement order, collision exclusions, and cap termination.
- Check Config/Reels mode pairs, table names, set weights, feature eligibility, legal values, impossible requests, and referential integrity. Unprefixed Config/Reels maps to Normal; select one complete mode per run without merging modes.
- Separate metadata gaps from unresolved mathematics. Balance approval can remain separate; do not fabricate target RTP.
- Map boundary cases to deterministic tests with expected outcomes and source references. Monte Carlo cannot settle an ambiguous rule.

When docs conflict with implementation, inspect current code and report the discrepancy. Historical status paragraphs may lag later decisions.
