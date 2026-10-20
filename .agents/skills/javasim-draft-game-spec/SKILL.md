---
name: javasim-draft-game-spec
description: Draft a small JavaSim game specification from existing slot-market research when a fallback concept is requested. Produces a GDD, flows, math/config plan, edge cases and review handoff within available broad GDDs. Use for fallback spec authoring, not routine human-led GDD work, implementation or mathematical approval.
---

# Draft a small fallback game specification

Use this as a fallback when the user wants a research-led concept because other design routes have stalled. Human-assisted GDD authoring remains the primary workflow. Creating this skill does not request a game draft; invoking it requests documentation, not implementation.

Read applicable repository instructions and [source routing and deliverables](references/sources-and-deliverables.md). Read [scope and mode rules](references/scope-and-modes.md) before designing.

## Non-negotiable scope for this workflow

| Dimension | Limit |
|---|---|
| Gameplay | One Basegame plus exactly one randomly triggered Freegame definition |
| Purchase modes | Normal, EM1 and BB1; these are not three different Freegames |
| Sets | 1–3 Basegame sets and 1–3 Freegame sets; fewer is preferable |
| Symbols | At most 10 regular/simple types and 5 special types across the whole game |
| Rule family | Only families supported by actual applicable files in `game/broadGDDs/`; currently lines |
| Complexity | Familiar core loop; include an optional feature only with a concrete player benefit |

Do not add super/epic Freegames, nested bonuses, another feature game or concealed extra sets under another name. Keep all purchase modes on the same Freegame rules. Future broad GDDs expand eligibility only after they exist and have been read; research or available Java classes alone do not authorize ways, clusters or another unsupported family.

## Workflow

1. **Establish inputs.** Use the requested report/brief and destination. Otherwise locate the latest relevant research produced by `javasim-research-slot-market` in `docs/research/` or its configured Notion hub. Read its revision/corrections, evidence ledger, candidate dispositions and methodology limitations. Assess measurement dates against the intended market/window; a recent publication or high turnover does not prove a mechanic is popular or caused success. If evidence is stale or missing, state that and verify material current claims as needed; do not launch a full research-and-publication cycle merely to draft a spec.
2. **Check eligibility first.** Inventory `game/broadGDDs/` and map each proposed evaluation/major mechanic to an applicable source. Keep small game-specific variations compatible with those rules. Simplify an unsupported inspiration or record it as rejected/deferred; do not invent a missing broad GDD to make a concept pass.
3. **Choose a compact concept.** Compare a few short alternatives if useful, then produce one coherent draft unless more were requested. Use familiar mechanics with a modest differentiator. For every optional feature ask **“Will the player actually care about this feature?”** Record its observable effect, likely benefit, evidence or design hypothesis, and complexity cost. A bare yes or marketing adjective is insufficient; remove weak additions.
4. **Specify the complete round.** Define the screen, explicit paylines and symbols, payout selection, wild/scatter behavior, trigger/award map, single Freegame loop, state lifetime/reset, operation order, termination and cap handling. Assign stable rule IDs. Apply Normal, EM1 and BB1 using the mode reference; distinguish proposed parameters from verified math.
5. **Write the source package.** Adapt the templates below, and copy/adapt the canonical repository creation form with its links corrected for the destination. Keep GDD, flow, config plan and expected edge cases consistent. Record rejected mechanics and feature decisions, rather than attaching every market idea.
6. **Review and hand off.** Count states, sets and symbols across the complete design. Trace every purchase mode through Basegame, the same Freegame and settlement; check EM1's full probability relation. Resolve design choices where reasonable, label unprovided math inputs and approval status honestly, and identify any blocking conflict. Finish the documentation even when balance remains unapproved. Route later source review to `javasim-review-game-spec`; implementation and math validation are separate tasks.

## Output templates

- [GDD](assets/GDD.md): rules and game identity.
- [Research rationale](assets/research-rationale.md): evidence, eligibility and player-value decisions.
- [Math/config design](assets/math-design.md): mode law, sets, units and outstanding PAR work.
- [Flowchart](assets/flowchart.md): editable Mermaid round flow and operation sequence.
- [Edge cases](assets/edge-cases.md): deterministic scenarios and expected outcomes.
- [Scope review and handoff](assets/scope-review.md): counts, consistency and readiness.

For a new unallocated concept, default to `docs/game-specs/<slug>/`; use an existing game workspace's `source/` only when the user identifies it. Preserve populated/approved files. Do not allocate a game ID or create an executable workspace just to write a draft.

Follow the root documentation-only validation policy: inspect rules, diagrams, local links, templates and cross-document consistency; do not compile Java or run game tests/simulations. Update `docs/architecture.md` with the draft/skill location as applicable. Return the artifact links, scope counts, mode summary and unresolved inputs. Do not claim implementation, owner approval, balanced RTP, validated EM1 math or a production-ready PAR without the corresponding evidence.

