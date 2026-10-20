# Replay contracts and commands

## Source map

- [Replay workflow](../../../../docs/replay-debugging-flow.md): maintained overview.
- [ReplayerMain](../../../../toolkit/replay/ReplayerMain.java): config resolution and comparisons.
- [SavedGameplayCsv](../../../../simulation/replay/SavedGameplayCsv.java), [SavedGameplay](../../../../simulation/replay/SavedGameplay.java), [ReplayEvent](../../../../game/model/ReplayEvent.java): current versions, summaries, ordering and payloads. Inspect code rather than assuming historical format docs are current.
- [GameSession](../../../../game/GameSession.java): replay is optional; default throws unsupported-operation.
- [Replay tests](../../../../tests/ReplayTests.java): generic fixtures and behavior.
- [G001 payload](../../../../game/G001-walkingwild/src/main/java/game/walkingwild/model/WalkingWildReplayPayload.java), [session](../../../../game/G001-walkingwild/src/main/java/game/walkingwild/WalkingWildGameSession.java): game-owned reconstruction.
- [Expanding Wild payload](../../../../game/expandingwild/ExpandingWildReplayPayload.java), [session](../../../../game/expandingwild/ExpandingWildGameSession.java): another encoding example.

## Reproduce from root

```sh
./build.sh
java -cp build/classes toolkit.replay.ReplayerMain /path/to/saved_gameplays.csv 123
java -cp build/classes ReplayTests
```

Replace example path/ID with evidence. Gameplay ID is not a CSV row number. Select game regression suites from the architecture or the validation skill's [test map](../../javasim-validate-math/references/execution.md).

## Configuration resolution

Currently ReplayerMain uses the saved game ID/mode and looks beside the CSV for .xlsx files. One is loaded for that mode; multiple workbooks fail. Without a workbook, non-Normal fails, but Normal falls back to the game's default config. That fallback can replay old evidence against changed math: establish provenance first. The cap supplied to the session is the loaded game's cap times stored base stake.

Comparison covers Basegame, Feature Game and total wins, event count, types and per-event wins using tolerances. Success alone does not prove every displayed field or payload choice was preserved; check these separately if relevant.

Capture obeys maxSavedGameplays and retains only complete events. No saved rounds can mean disabled, unsupported or incomplete capture. Use the validation skill's controlled simulation template to produce temporary reports without persistent config edits.
