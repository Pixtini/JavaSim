# Ember Forge authoring review

Author consistency review under the user's end-to-end prototype authorization; not independent product/math sign-off.

| Constraint | Actual design |
|---|---|
| One Basegame + one Freegame | Ordinary/entry Basegame and one eight-spin Heat Freegame |
| Purchase modes | Normal, EM1, BB1; identical FG rules |
| BG sets under four | Two global definitions: BG-0 and BG-1 |
| FG sets under four | One definition: FG-0 |
| Symbols | Six regular + Wild/Scatter = two special |
| Broad rules | [lines.md](../../broadGDDs/lines.md); no ways/cluster evaluation |
| Added feature | Bounded Heat progression, justified in rationale |
| EM1 | Exact 4/D vs 1/D probability at 2× cost |
| BB1 | Non-paying legal entry; same Freegame |
| State/cap/replay | Defined in GDD; no carryover or further bonus |

No additional GMF algorithm or simulation-engine redesign is required: existing reels, paylines, weighted selection, results, replay and player presentation are reused. Game-specific Heat logic remains in this module.

Numerical values are intentionally chosen and may be adjusted only within this new game's rough-balance pass. Human product approval, commercial appeal, responsible deployment requirements and production math sign-off are not inferred from tests.
