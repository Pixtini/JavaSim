# Publish market research to Notion

Use the connected Notion tools and their live schemas. Do not hardcode tool arguments or assume a named plugin is installed. The skill is still useful for research when Notion is temporarily unavailable; publication remains incomplete until a confirmed write.

## Configured research destination

The user designated the private [JavaSim Research Hub](https://app.notion.com/p/3ffa48e4006081d1ac88d55ba4073624) as the directory for future research reports.

- **Hub page ID:** `3ffa48e4-0060-81d1-ac88-d55ba4073624`
- **Market reports:** [Market Reports](https://app.notion.com/p/3ffa48e400608109b14bec228a729876), page ID `3ffa48e4-0060-8109-b14b-ec228a729876`.
- **Separate process evaluations:** [Skill Evaluations](https://app.notion.com/p/3ffa48e40060817a9a30c6869c805640), page ID `3ffa48e4-0060-817a-9a30-c6869c805640`.

## Destination selection

1. An explicit destination in the current request overrides the configured default. Fetch the page before writing; for databases, fetch the schema and use the actual properties and correct data source.
2. Otherwise fetch the configured Market Reports page and create the dated report as its child using that page as the parent. This is an established user-designated destination; do not create a separate workspace-level draft or ask for placement again.
3. If the user requests a separate agent/process evaluation, place it under Skill Evaluations and link it to the market report. Do not create an evaluation for every report unless requested.
4. Keep evidence ledgers and supporting artifacts with the corresponding report. The pilot report and its evaluation were moved into these areas; their original page URLs remain valid.
5. If the configured destination is unavailable, finish the report and follow the live connector's private-draft fallback when available. State that it was not placed in the hub, retain a copy-ready/local version, and request only missing access or placement information. Do not silently use an unrelated or shared destination.

The separate JavaSim Context Hub holds repository documentation snapshots; do not put research there by default or replace imported source pages. Do not move or share the research hub without user direction.

Check tool access before writes when the connection provides an access map. Never request unrelated account scopes or send notifications/messages as part of research.

## Save and verify

- Use an identifiable title such as "Slot Market Research — YYYY-MM-DD — Online and Land-Based", using the user's date/timezone and scope.
- Include scope, data-as-of dates, evidence gaps, direct citations and the provider/market coverage table. Adapt the report template rather than publishing empty placeholders.
- Attach or include the ledger when useful; the report should still have citations near material claims.
- Search for an existing report with the same date/scope before creation. On an explicit refresh, update the intended report and retain a short change note. For a new reporting period, create a new dated report.
- After an uncertain create/write, search or fetch before retrying to avoid duplicate pages. Preserve unrelated content and user edits.
- Verify the returned page and read back unclear/partial writes, including subpages or attachments if used. A failed or partial save must be stated explicitly, with a copy-ready fallback.
- Return the confirmed Notion URL. If publishing is blocked, distinguish that from completed research and retain the report for later publication.

Running the research skill with its report-to-Notion behavior authorizes saving that report. Creating the skill alone does not authorize or require publishing test reports, and the skill does not schedule itself.
