---
name: javasim-review-game-spec
description: Review a JavaSim game's GDD, PAR workbook, flowchart, and edge cases for missing or conflicting mathematical rules before implementation. Use for game intake reviews and updating AGENT-REVIEW.md; implementation and statistical validation are separate workflows.
---

# Review a game specification

Work from the JavaSim repository root and read applicable `AGENTS.md` files. Identify the target `game/GXXX-game-name/` workspace from the request; ask only if multiple games fit and the target matters.

Read [source routing and review checks](references/source-review.md), then inspect the target's actual source package. Repository documents remain the maintained source of rules.

1. Inventory sources, versions, owners, and approval status. Read the GDD, edge cases, flowchart, creation form, applicable shared rules, and PAR. Inspect workbook contents, including mode pairs and tables; a filename is not evidence of completeness. State any PDF/workbook coverage that could not be inspected.
2. Reconcile each rule with its configuration representation and an expected deterministic test. Distinguish missing metadata, conflicting rules, missing config, implementation defects, and balance questions. Existing code demonstrates current behavior, not intended rules.
3. Cite document sections, workbook sheet/table/cell ranges, or code locations for findings. Respect explicit source precedence and approved game-specific exceptions. Surface unresolved conflicts rather than inventing rules or balance values.
4. Update the game's `AGENT-REVIEW.md`, preserving owner answers, stable IDs, and resolved history. For a new log, adapt [the review template](assets/review-log.md). Ask concise questions for blocking decisions while continuing independent review work.
5. Report readiness for implementation within the reviewed scope, remaining decisions, and uninspected sources. Internal consistency does not imply source or balance approval.

Review authorization permits review artifacts, not game implementation or PAR/config edits. Keep recommendations separate from approved decisions. Follow project documentation completion requirements and the [root validation policy](../../../AGENTS.md). A source-only review requires source, content, link, and format checks, not Java compilation, game tests, or simulations. Run runtime checks only when relevant implementation/config changes or an explicit validation request make them applicable.
