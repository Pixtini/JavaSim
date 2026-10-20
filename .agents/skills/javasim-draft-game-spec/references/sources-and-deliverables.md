# Sources, evidence and output placement

Paths below resolve from this reference in the installed repository skill.

## Maintained repository inputs

- [Root instructions](../../../../AGENTS.md), [game instructions](../../../../game/AGENTS.md) and instructions in the actual destination.
- [Broad line GDD](../../../../game/broadGDDs/lines.md): reel wrapping, zero-based paylines, highest award per line, independent overlapping lines, stake/cost separation, payout increments, cap termination, set selection, wilds and scatters. Inventory the directory again on each invocation; do not maintain a speculative list of supported future families.
- [Canonical creation form](../../../../docs/game-creation-form.md) and [creation workflow](../../../../docs/game-creation-flow.md): current intake and later implementation expectations.
- [G001 lessons](../../../../docs/lessons/G001-LESSONS-LEARNED.md): examples of unit/transition/config mistakes. A different game's values, buy price and feature rules are not defaults.
- [Research workflow](../../javasim-research-slot-market/SKILL.md) and its [publication destinations](../../javasim-research-slot-market/references/notion-publishing.md): locate the latest relevant research rather than hardcoding one dated report.
- [Source-review workflow](../../javasim-review-game-spec/SKILL.md): later reconciliation of drafted rules and actual PAR. Do not write a self-approved `AGENT-REVIEW.md`.

Read only implementation references needed to identify feasibility gaps. Existing code is not a source of intended new-game math or permission to change the engine.

## Use the research critically

Record report path/Notion URL, revision, measurement period, market, relevant claim IDs and original-source links. Read corrections and latest attachments before treating an older report as authoritative. Include high-performing established candidates as well as releases; unknown launch dates do not justify silently omitting an established performer.

Choose a few design-relevant patterns. Keep actual turnover, bets, player counters, panel indexes and sentiment separate. Do not convert a sparse forum sample into population preference. Distinguish sourced mechanic facts, analyst interpretation and original design proposals. An unverified mechanic in a report stays unverified; prefer a simpler, explicit rule rather than copying conflicting product copy.

An inspiration may use ways, clusters or complicated progression. Its player-facing idea can inspire a simpler line-compatible proposal, but the new evaluation and behavior must be stated as new design decisions. Do not copy its proprietary PAR or claim equivalence to its results.

## Package

Create six template-based documents plus `game-creation-form.md` adapted from the canonical form. All generated documents identify the draft status and agree on rule IDs, game name, counts, mode costs, evaluation, trigger sequence and caps. Keep rules in the GDD and reference them elsewhere; avoid divergent duplicate specifications.

For a new draft, `docs/game-specs/<slug>/` is staging, not a registered `GXXX` game. Mark game ID/package/workbook/integration fields unallocated or pending. When later implementation is authorized, promote the reviewed documents into the wizard-created workspace and repair relative links. Do not run the wizard, add Java classes or produce a blank workbook as supposed mathematical input for this task alone.

For an explicitly identified workspace, preserve approved sources and owner decisions; draft new changes separately when replacement is not authorized. No existing PAR/config is edited.

## Completeness and status

A useful draft makes structural rules concrete; it is not just headings full of unknowns. Select reasonable, clearly proposed design values where the authoring request permits judgment. Reserve unresolved fields for genuine evidence/math/owner decisions. Keep proposed paytables, strips, weights, buy price and balance targets distinct from approved values; do not invent an achieved RTP or mathematically certified trigger ratio.

The creation form's implementation/test sections remain pending. Include a specific later validation plan, not fabricated execution results. A missing balanced PAR prevents implementation-ready mathematical status, not completion of the requested draft. Publish to Notion only if the user requests that for the spec; the research skill's report publication behavior does not automatically apply here.

