# Scope, counting and purchase modes

## Count the actual design

- Exactly two gameplay states: Basegame and one Freegame. Normal/EM1/BB1 are purchase configurations, not extra gameplay states. BB1 purchases entry into that same Freegame. Do not rename a stronger bonus as a purchase setting to evade this limit.
- Use 1–3 sets for Basegame and 1–3 for Freegame. Count across the whole specification, not a fresh budget for every purchase mode. Reuse definitions and explain mode-specific selectors/overrides. Materially distinct per-spin reel/feature profiles are additional definitions even if they reuse an ID; do not hide a fourth set in a phase, table or mode.
- Maximum 10 regular/simple symbol types and 5 special types, counting the union across Basegame, Freegame, sets and purchase modes. A self-paying wild is still special. Cosmetic aliases do not create another type, but behaviorally different variants must be counted. List IDs, class, behavior and substitutions explicitly.
- Prefer one set per gameplay state, a compact fixed screen, one understandable trigger route and a familiar wild/scatter vocabulary. These are simplicity preferences, not extra numeric limits on grid size or paylines.
- Retriggers, multipliers, persistence and transformations each need the player-value test and explicit ordering/reset rules. A retrigger, if justified, extends the same Freegame with the same rules; it cannot unlock another bonus tier. Default to omitting optional complexity with weak benefit.
- Optional features must produce a clear player-visible effect or meaningful choice. State the expected benefit and the state/rule/communication burden. Evidence may be weak: label the benefit a design hypothesis and prefer the simpler alternative when unconvinced.

## Supported rules

Currently [lines.md](../../../../game/broadGDDs/lines.md) is the applicable broad GDD. Use its stake units, minimum payout granularity of 0.05 base units, line selection, wraparound and cap semantics. Specify line direction, minimum matches, paytable and transformation timing rather than importing assumptions from another game.

A small game-specific feature may extend the lined design if it preserves the applicable broad rules and is fully specified. A new evaluation family or major mechanic without an applicable broad GDD is deferred. Do not use available generic classes or attractive research alone as rule coverage; do not author broad-GDD exceptions automatically.

## Mandatory purchase modes

Let `s` be the player's base stake and `pN` the probability of entering the single Freegame from a Normal paid Basegame round under the complete specified trigger law.

| Mode | Round cost | Entry and Freegame |
|---|---|---|
| Normal | `s` | Default selected mode; random Basegame trigger probability `pN` |
| EM1 | `2s` | Same game and same Freegame; trigger probability exactly `4 × pN` per paid Basegame round |
| BB1 | `C_BB × s` | Guaranteed legal entry into the same Freegame; positive buy multiplier explicitly proposed or unresolved |

Only one purchase mode can be selected before a round. Lock its configuration and stake for the complete round. Free spins do not incur new charges; payouts and the combined Basegame/Freegame cap stay in base-stake units, not cost-multiplied units.

EM1 changes trigger likelihood and cost, not the Freegame's conditional rules or a new enhanced bonus. Keep Freegame entry awards and conditional configuration equivalent; any Basegame payout-distribution effect of the trigger change must be documented. The factor of four is per paid round, not per currency unit; do not call it four times the chance per pound.

For a nonzero viable trigger, `0 < pN <= 0.25`. If the proposed design cannot meet `pEM1 = 4pN`, revise it or flag a blocking mathematical conflict; do not clamp the probability, substitute “approximately four times” or silently weaken the requirement.

Four times an insertion weight or four times as many scatters is not proof of four times trigger probability. Use the full joint trigger law, including set selection, natural/inserted scatters, collisions, replacements and cap/eligibility effects where relevant. Record the proposed relation, derivation/check needed and its current evidence status. Do not claim validated behavior before parameterization and appropriate analysis.

For an independent trigger/no-trigger draw, changing its probability can provide a simple design route, but explain conditional screen generation and prevention of unintended natural triggers. The final screen's scatter map and documented trigger rule must agree with that draw.

## BB1 and balancing boundaries

Do not inherit G001's 100× price. Specify an explicit proposed `C_BB` when justifiable, or carry it as a named unresolved math input. Describe the purchase entry, initial spin count, Basegame payouts, cap treatment and all random choices. Follow the shared convention of starting on Basegame: a forced legal trigger/entry Basegame is a straightforward route. Do not invent a conflicting direct-entry exception without owner instruction.

Prefer a non-paying BB1 entry Basegame. If entry pays are proposed, resolve their cap interaction so they cannot consume the round cap and cancel the purchased Freegame before it starts. The guaranteed-entry claim must hold over all legal buy outcomes.

BB1 must not add a separate Freegame, stronger rules, extra guaranteed feature or a special buy-only symbol set outside the scope budget. Distinguish the buy's expected-value work from structural game rules. RTP, volatility, reel strips and probabilities require separate parameterization/validation; market performance supplies no approved math targets.

