# walkingWild Game Flowchart

This Markdown diagram combines the full round flow from the GDD and shared line-game rules with the freegame spin sequence shown in [`flowchart.pdf`](flowchart.pdf). The PDF covers the freegame subflow only.

```mermaid
flowchart TD
    Start([Player selects mode and starts a round]) --> Base[Play one Basegame spin using selected mode]
    Base --> BaseWins[Evaluate paylines and add Basegame win]
    BaseWins --> BaseCap{Round win cap reached?}
    BaseCap -->|Yes| End([Round complete])
    BaseCap -->|No| ScatterCount{Count Basegame scatters}
    ScatterCount -->|Below 4| End
    ScatterCount -->|4| Award5[Award 5 Freegames]
    ScatterCount -->|5| Award8[Award 8 Freegames]
    ScatterCount -->|6| Award12[Award 12 Freegames]
    Award5 --> FGStart
    Award8 --> FGStart
    Award12 --> FGStart

    FGStart[Start next Freegame spin and consume one] --> CountWilds[Count persisted wilds]
    CountWilds --> NewWildCount[Use 2DCHANCETABLE to determine new wild count]
    NewWildCount --> ReelSpin[Spin reels; new wilds land with the reels; total wilds capped at 5 by config]
    ReelSpin --> Reel6{Has reel 6 stopped?}
    Reel6 -->|No| WaitReels[Wait for remaining reels to stop]
    WaitReels --> Reel6
    Reel6 -->|Yes| MoveWilds[Copy location weights; set occupied and newly selected wild cells to 0; move persisted wilds]
    MoveWilds --> FGWins[Evaluate paylines and add Freegame win]
    FGWins --> TotalCap{Round win cap reached?}
    TotalCap -->|Yes| End
    TotalCap -->|No| MoreSpins{Freegames remaining?}
    MoreSpins -->|Yes| Persist[Remove regular symbols; keep wilds]
    Persist --> FGStart
    MoreSpins -->|No| End
```

Notes:

- The selected mode is chosen before the round and supplies the Config/Reels pair for its spins.
- The PAR scatter map awards 5, 8, or 12 Freegames for 4, 5, or 6 scatters. Counts below 4 do not trigger the feature.
- The PDF says persisted wilds remain in limbo above the reels while the reels spin, then move after reel 6 stops. Newly selected wilds land with the reels.
- Persisted wilds use a temporary copy of the same Freegame location weights. Set cells occupied by newly landed wilds and persisted wilds already placed to 0 for this placement sequence; preserve the original PAR weights. The config limits the total to 5 active wilds, preventing more wilds from being requested than the screen can support.
- The PDF does not define Freegame retriggers; the GDD says there are no scatters in the Freegame, so this flow has no retrigger branch.
- The common max-win rule clips the spin that reaches the cap and ends any remaining Freegames.
