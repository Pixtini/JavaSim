# Ember Forge boundary behavior

Expected behavior; execution evidence belongs in the validation report.

| Case | Expected result / source |
|---|---|
| Reel stop at index 11 on a 12-symbol strip | Visible positions 11, 0, 1; G-01 |
| Ten identical five-EMBER lines at stake 2, Heat 3 | 10×27×2×3 = 1,620; one selected award per line, G-02 |
| All-wild line | Substitute for highest regular, five EMBER pays 27×; G-02 |
| Ordinary BG set | No Scatter, never enters feature; BG-01 |
| Any of 243 entry-set stop combinations | Exactly three Scatters, no line wins, eight free spins; BG-01/FG-01 |
| Winning free spin at Heat 2 | Evaluate at 2×, next spin Heat 3; FG-01 |
| Losing free spin at Heat 4 | Zero pay, next spin remains Heat 4 |
| Winning free spin at Heat 5 | Remains at Heat 5 |
| New feature or paid round | Heat resets; no cross-round accumulation |
| Normal vs EM1 | Tickets 1/D and 4/D exactly; cost 1× and 2×; same FG |
| BB1 | Guaranteed non-paying entry plus same eight-spin FG; no additional charge per free spin |
| Lower validation cap reached mid-feature | Clip reaching spin, discard remaining spins; preserve detailed win totals and replay |
| Invalid/missing mode pair, divergent FG or extra symbols/sets | Reject configuration before play |
| Truncated/extra/malformed replay events | Reject; recorded win values cannot replace recalculation |
| Fixed seed and partitions, different worker counts | Same mathematical totals/distribution within floating-point tolerance |

Fixtures and rejection cases are implemented in the game-specific test suite. Production balance approval remains separate.
