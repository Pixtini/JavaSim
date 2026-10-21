# Ember Forge numerical design — prototype revision 1

20 October 2026. An agent-selected rough balance around 97% was used for this end-to-end prototype; the user did not supply a production RTP target. Human math/product approval remains separate. No existing game's PAR was changed.

## Inputs and units

[balance-input.json](balance-input.json) contains every strip, payline, payout, mode cost and trigger denominator. The numerical baseline can be regenerated with `python3 game/G002-ember-forge/source/build_par.py --output /tmp/G002-baseline.xlsx`, with three isolated Config/Reels mode pairs. The current [PAR.xlsx](PAR.xlsx) now also includes human-balanced Books sheets and is authoritative for book runs; the helper refuses to overwrite an existing Books workbook. The JSON does not contain those human book weights. [GDD.md](GDD.md) defines the fixed eight-spin, maximum-five Heat rule. The writer uses Python's standard library only.

Payouts and the 8,100× cap use base stake. Purchase costs affect the paid exposure denominator, not symbol awards or Heat. Computation retains doubles without per-line currency rounding. The prototype rounds its displayed wallet to cents after debit/settlement; simulation math uses unrounded values.

| Mode | Cost in base stakes | BG-0 weight | BG-1 weight | Trigger per paid round |
|---|---:|---:|---:|---:|
| Normal | 1 | 138 | 1 | 1/139 = 0.719424% |
| EM1 | 2 | 135 | 4 | 4/139 = 2.877698% |
| BB1 | 47 | 0 | 139 | 1 |

BG-0 ordinary strips have 12 stops per reel. BG-1 has three per reel and always shows exactly three Scatters, with no line award. FG-0 has 12 stops per reel. Every mode uses identical strips, paytable, lines and Freegame initialization/progression. BB1 omits the zero-weight BG-0 entry when constructing the runtime positive-weight selector.

Thus EM1's trigger probability is exactly four times Normal's **per round**, not four times per unit spent. Expected paid rounds per feature: Normal 139; EM1 34.75. Neither mode manipulates or rejects a generated screen.

## Exact component calculation

The [audit](../src/test/java/game/ember_forge/tests/EmberForgeMathAudit.java) independently enumerates all 12⁵ = 248,832 stop combinations for BG-0 and again for FG-0. It uses integer 0.05-unit ticks and tries every eligible regular substitution, then compares every screen with the actual GMF-backed game evaluator. This avoids using the game calculator as its own expected-value oracle. These are uniform stop combinations, including repeated symbol windows, not equally weighted unique pictures.

Let B be the ordinary Basegame win and X a Freegame win before Heat:

- E[B] = 0.647123360340; E[B²] = 5.020960889275.
- E[X] = 2.455664866255; P(X > 0) = 0.383680555556.
- BG-1 has zero pay on all 3⁵ = 243 stop combinations (checked separately).

For n remaining free spins and current Heat h, let m = min(h+1,5), q = P(X>0), μ = E[X], and μ₂ = E[X²]. Starting E₀(h) = S₀(h) = 0:

```text
Eₙ(h) = h μ + (1−q) Eₙ₋₁(h) + q Eₙ₋₁(m)
Sₙ(h) = h² μ₂ + (1−q) Sₙ₋₁(h) + q Sₙ₋₁(m)
         + 2 h μ Eₙ₋₁(m)
```

The cross term uses m because any positive current win raises the next spin's Heat; zero wins contribute no cross term. Future reel draws are independent. The resulting full-feature mean F = E₈(1) is **45.675585266473 base stakes**, and its second moment is 9760.97242426922. This includes every possible Heat path rather than treating eight spins as having a fixed multiplier.

Ten highest-paying lines at 27×, multiplied by Heat sequence 1,2,3,4,5,5,5,5, bound the feature at 8,100×. Entry pays zero; ordinary BG pays at most 270×. The configured cap therefore does not remove any excess payout from these expectations. A deterministic all-EMBER replay and generated round reach exactly 8,100×; lower injected validation caps are separately tested.

## Selected rough balance

For a mode's entry probability p, mean round win is `(1−p) E[B] + p F`. RTP divides that mean by mode cost. Second moments follow the same mixture, giving the theoretical variance used for simulation uncertainty.

| Mode | Theoretical RTP | Round standard deviation, base stakes |
|---|---:|---:|
| Normal | 97.106913% | 8.617696 |
| EM1 | 97.145322% | 16.792644 |
| BB1 | 97.182096% | 87.605441 |

These returns are close, not identical. With an unpaid entry set, selecting D near `2(F−E[B])/E[B]` equalizes Normal and EM1 RTPs; integer rounding gives D=139. An integer 47× buy price keeps BB1 close to those returns. This design favors a simple, inspectable rough balance; no prescribed production target or commercial volatility band was supplied.

Machine-readable results: [exact-math.csv](../validation/exact-math.csv). Observed simulations, confidence intervals, hashes, retained reports and limitations: [validation report](../validation/validation-report.md). Changing the strips/paytable invalidates the recorded expectations and requires rebuilding PAR and repeating the audit. This audit is deliberately bounded to small reel products and this Heat law, not a general exact slot solver.


## Recorded-book balance added 21 October 2026

The expectations above describe the unrestricted reel game. Current `<Mode>-Books` sheets and `Books/saved_gameplays_<mode>.csv` define a separate finite weighted distribution. See [shared book workflow](../../../docs/gameplay-books.md) and [current book validation](../validation/books-2026-10-21/validation-report.md). Reweighting replay IDs can change RTP and trigger frequencies independently of D; do not use the original reel expectations to validate book-enabled simulations.
