# Research Skill Experience — Two-Week Pilot — 20 October 2026

## Result and scope

One assistant used `javasim-research-slot-market` for a release scan covering **7–20 October 2026** and a current established-performer comparison. This is an operational evaluation of an actual run, not a simulated evaluation or an independent multi-agent review. No subagents were spawned.

The run produced a twelve-title shortlist, a separate performance-cohort analysis, limited sentiment findings, original JavaSim design hypotheses, and a 42-claim evidence ledger. It found useful material and important gaps. It did not verify global turnover, obtain full paid datasets, or establish representative fresh player sentiment.

The skill itself was left unchanged. Recommendations below are proposals for the user to review.

## What the skill did well

- Separating turnover, theoretical-win indices and counters prevented a misleading combined leaderboard.
- The requirement to open originals caught release-date and specification conflicts that search results alone concealed.
- Explicit operator coverage kept Duel distinct, and made Rainbet's access limitation visible rather than silently omitting it.
- Broader supplier discovery found useful screen/action examples outside the largest studios.
- Bias guidance helped distinguish original player accounts, affiliate editorial, supplier promotion and unverified search excerpts.
- Mapping findings back to Basegame/Feature Game contracts made the report useful to JavaSim without automatically altering game code.

These are observed benefits of the instructions, not a causal benchmark against a control run without the skill.

## Steps actually taken

1. Read the skill, discovery guide, evidence rules, Notion guide, report template and ledger header. Selected the user's fortnight instead of the default 90-day release window.
2. Set inclusive calendar dates and a separate established-performance track. Logged the actual run start from the clock tool.
3. Inspected connected Notion tool availability and live schemas. Read the native enhanced-Markdown specification before preparing publication.
4. Searched release calendars and performance sources, then opened operator lobbies and original supplier pages. Pursued direct named report pages when broad searches were noisy.
5. Compared official catalog history with fresh roundups. Kept conflicting release candidates out of the verified-window list.
6. Sampled operator counters and distinguished them from the panel and wager-share cohorts. Kept cached/rendered observations separate from historical measurements.
7. Looked for original community discussion and counterevidence. Direct Reddit fetches failed; a public alternate-host attempt also failed. A retrieved forum thread showed that a recent-looking search result contained older comments. Those were retained only as historical context.
8. Followed an official game-rules link, exposing unresolved runtime placeholders. Compared operator and publisher descriptions and checked arithmetic examples rather than assuming every official sentence was internally consistent.
9. Inspected the actual JavaSim session/partition logic to ground the proposed implications of persistent state and interactive inputs.
10. Wrote local Markdown backups and a structured ledger; prepared two separate Notion private drafts and a ledger attachment. Used read-back checks to validate saved content. Publication URLs and final check outcomes are recorded in the local publication record.

## Concrete problems and suggested changes

| Priority | Observed problem | Proposed skill improvement |
|---|---|---|
| P0 | A two-week release scan can accidentally imply that older panel results measure those same dates | Require explicit release-window, evidence-publication-window and measurement-window fields; label each title's inclusion reason |
| P0 | Search excerpts can look like original player evidence even when original retrieval fails | Add retrieval-status and admissibility fields; index-only discussions stay in a leads appendix and cannot support the verified sentiment conclusion |
| P0 | A public source contained a conflicting denominator; another used inconsistent numerical examples | Add a lightweight source-consistency check: denominators, units, arithmetic, RTP version and whether explanatory examples match stated rules |
| P0 | The skill's Notion guide suggests finding an existing project parent, while the current connector requires a private draft when the user has not named a destination | Align the guide with the live connector: default to private drafts; use a named parent when supplied; never silently move or share |
| P1 | Slot-title variants and reporting labels can differ | Add canonical title, studio, distributor/group, jurisdiction/build, and variant-match status; do not merge without evidence |
| P1 | Live-looking counters came from independently cached pages | Capture response time, source update time if available, exact label and observation context; do not describe them as synchronized live measurements |
| P1 | Publisher rules can contain template placeholders rather than deployed parameters | Add a rules-completeness field and an explicit distinction between catalog mechanics and verified live paytable |
| P1 | Newly released titles have little independent feedback | Add a “too early to assess” sentiment outcome and a targeted later follow-up list; do not fill the gap with affiliate ratings |
| P1 | The 21-field ledger lacks access outcome, window classification, timestamp precision and linked conflicts | Extend it with the four extra columns used in this pilot; retain an explicit Unknown where precision is unavailable |
| P1 | A broad search can spend attention on irrelevant dates/topics and repetitive press coverage | Use a staged search plan: operator observations, dated releases, direct research reports, rule verification, feedback and exclusion review |
| P2 | The report template encourages many repeated title facts | Add a compact comparison table plus deeper profiles only for the strongest mechanics; keep coverage gaps visible without padding |

## What I supplemented beyond the skill

These actions were needed for this request or imposed by the environment; they were not additional market findings.

- **User-requested secondary report:** designed this evaluation document because the skill has no evaluation template.
- **Live connector protocol:** read tool schemas, inspected plan/access limits, used Notion's actual Markdown format and the private-draft default. The skill says to verify access but does not encode the current connector's creation policy.
- **Ledger extension:** added `retrieval_status`, `window_match`, `observed_at_utc` and `conflict_id` to preserve distinctions the template cannot express directly.
- **Original-source reconciliation:** checked exact post dates and mathematical consistency rather than trusting a fresh crawl timestamp or an “official” label.
- **Temporary local backups and publication record:** made the work independently reviewable and recoverable if a remote write failed.
- **Repository completion:** updated architecture documentation and ran the required compilation, relevant existing tests and a small seeded simulation. Those checks protect repository workflow; they do not validate external game rules or market conclusions.
- **Private data boundaries:** avoided publishing raw operator chat, personal identifiers or bulk scraped discussion. No account login, paid report purchase, wagering, messages to operators, automation schedule, skill edits or engine changes were performed.

The source consistency work supplements the skill's general evidence guidance; it is not a claim that the skill prohibited it.

## Important limits of this run

The player-feedback objective was only partially evidenced: one recent affiliate editorial and an older original community thread were readable, while fresh Reddit leads could not be fully retrieved. The report therefore has no representative sentiment score. Public paid-report summaries were enough to locate useful performers, but not enough to reconstruct turnover or all measurement methodology.

The twelve-game shortlist is a selection, not all releases, and several performance benchmarks did not receive a complete rules audit. Screen and interaction ideas are hypotheses, not proven causes of retention or turnover.

This run is not a model comparison. No alternate model or independent evaluator was used, so it cannot establish which model is best. Actual elapsed time is recorded after publication; it is not evidence that a fuller monthly report will take the same time.

## Recommended next revision and next run

First revise temporal provenance, source-admissibility and Notion destination handling. Then add the ledger fields and a small evaluation checklist. Preserve the current separation between evidence, inference, sentiment and original design proposals.

For the next research run, use a named Notion destination and obtain accessible first-hand feedback or supplied data exports. Choose two or three mechanics for deeper rule verification rather than expanding the shortlist merely to increase title count. Persistent-state and input-policy questions should be resolved before turning any idea into a simulation specification.


## Delivery and verification

[Notion market report](https://app.notion.com/p/3ffa48e4006081fda60dc5a324ef9042) · [Notion skill experience](https://app.notion.com/p/3ffa48e4006081f8be45ee93890b6e8a)

Elapsed from recorded research start (13:57:49 UTC) through repository checks (14:07:48 UTC): 9 minutes 59 seconds; final cross-link publication/read-back followed. This bounded pilot is not a monthly-runtime benchmark.

Compilation passed; SimulationTests passed 88 assertions; a 1,000-round G001 Normal simulation with seed 440055, two workers and 16 partitions completed with three feature triggers. Local validation checked 42 unique evidence claims, twelve profiles and unchanged hashes for six skill resources.
