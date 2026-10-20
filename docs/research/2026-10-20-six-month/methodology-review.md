# Methodology review — Waylanders Forge omission

Published under [Notion Skill Evaluations](https://app.notion.com/p/3ffa48e40060813cb91bd7a55743e315).

Review date: 20 October 2026. Applies to the [six-month report](market-report.md), first published at 14:39 UTC. The user supplied [Stake Engine Insights](https://stake-engine.com/insights), exposing a material gap in the report's discovery and selection.

## Finding

**The omission was a research and selection failure.** Waylanders Forge had already appeared in the Stake lobby inspected during the original run. It was absent from the report, ledger and provider coverage, with no recorded exclusion reason. The skill allowed broader sources; the execution failed to find and use an especially relevant first-party analytics route.

The earlier statement that BingBong was the closest accessible direct wagering evidence was too broad and has been corrected. The report remains a useful sampled mechanics/panel survey, but its original performer selection was incomplete for the user's named Stake/crypto-market scope. Counting sources, checking links and verifying Notion publication did not establish adequate market coverage.

## What the new evidence establishes

The public dashboard's read-only [last-30-day turnover request](https://stake-engine.com/api/gameRanking?sort=turnover&time=past30Days&size=10), retrieved during this review, places Waylanders Forge first with USD 1,173,496,311.88 turnover and 93,888,554 bets. It also leads the [previous-month](https://stake-engine.com/api/gameRanking?sort=turnover&time=pastMonth&size=10) and [all-time](https://stake-engine.com/api/gameRanking?sort=turnover&time=allTime&size=10) turnover requests. In the separately fetched [last-30-day bet-count view](https://stake-engine.com/api/gameRanking?sort=betcount&time=past30Days&size=10), it ranks third: choosing a metric changes the leaders.

These are **self-reported Engine-cohort observations**, not independently audited cash-only turnover, all-catalog Stake rankings, six-month totals or unique-player counts. The API returns no exact period boundaries or operator split. Promotional/demo activity, currency conversion and bonus-buy treatment are not established. A current leader can and should enter the established-performer scan even when its exact launch date is unknown.

The public [Insights client](https://stake-engine.com/_app/immutable/nodes/140.bjIXCNRA.js) divides ranking turnover by 1,000,000 before USD display; the platform stats endpoint uses another display scale. The saved [snapshot](stake-engine-snapshot.json) retains raw values, requests, retrieval times and conversion provenance. Indexed search figures were older and differed; they were not relabeled as current.

## Where the original method failed

| Failure | Evidence in the original work | Consequence |
|---|---|---|
| Priority-source discovery | Stake was explicitly in scope, but its Engine Insights route was not investigated. Operator work stopped at lobby counters and page copy. | Available title-turnover evidence was missed. |
| Candidate follow-through | Waylanders Forge appeared in the inspected lobby with a displayed activity signal; no candidate/disposition register existed. | A conspicuous game disappeared between discovery and publication. |
| Source imbalance | Sixteen of 45 registered sources and 33 of 72 claims came from one publisher, Eilers. Those panels cannot stand in for the named crypto operators. | Many source pages gave an appearance of breadth while the most relevant ecosystem remained shallowly covered. |
| Selection criteria | Interesting mechanics and accessible launch pages drove the casebook. No comparison against performance leaders was recorded. | Novelty/accessibility displaced the user's high-turnover and high-activity priorities. |
| Temporal framing | The report had monthly panel editions, but no consistent six-month title series; recent leaders were not systematically checked separately. | “Six-month deep dive” could be read as stronger longitudinal coverage than was achieved. |
| Validation target | Checks covered CSV structure, citations/links, skill metadata and publication. | A structurally valid report passed despite a material coverage omission. |

The skill's measurement and bias cautions were useful, but its stopping rule and candidate tracking were insufficiently concrete. This is not explained away by the absence of globally complete data: narrower, clearly labeled data was still worth using.

## Corrections made

- Add Waylanders Forge to the headline watchlist, the Engine turnover comparison and a new mechanic case. Its [Stake game page](https://stake.com/casino/games/valkyrie-waylanders-forge) identifies Valkyrie and a 6×4 ways/cascade structure; its product copy is not a complete implementation specification.
- Add the other recent Engine turnover leaders to a comparison table, rather than repairing only the user-named example. The [candidate register](candidate-disposition.csv) retains 23 distinct games across four top-ten views, with inclusion/deferment reasons.
- Update source register, claim ledger, method notes, publication counts/hashes and the Notion report. Retain the original publication metadata and mark replaced attachments as superseded in the manifest.
- Add first-party analytics discovery, candidate reconciliation and coverage review to the research skill. Preserve distinct turnover, bets, player-count, sentiment and historical-window conclusions.
- Publish this review separately under the hub's Skill Evaluations and link it to the report. No game or engine changes or simulations are involved.

This is a focused correction and method review, not a newly completed six-month census or a full rules/sentiment audit of all 23 additional candidates.

## Acceptance checks for future reports

1. For each priority operator, document analytics/insights sources investigated and what they cover. “No data found in the sources checked” is different from “no public data exists.”
2. Reconcile the shortlisted games against accessible turnover, bet-count/activity and established-performer leaders for the relevant windows. Record why any prominent candidate is omitted or deferred.
3. Keep two discovery streams: in-window releases and established/current performers. Do not require a verified new-release date to report an established leader.
4. Record source/publisher concentration and market coverage. Additional pages from the same panel are temporal depth, not new independent market coverage.
5. Check dynamic-page completeness. A loading shell requires another permitted retrieval route or an explicit unresolved access limitation; search-cache figures retain their original freshness status.
6. Review completeness of the research against the user's priorities before publication, separately from formatting and schema checks.

Validation of this correction checks reproducible ranking/order/conversion, ledger consistency, candidate dispositions, local links, skill metadata and Notion readback. Missing history, operator split and representative sentiment remain explicit limitations.

