# Ember Forge round flow

Rules: [GDD.md](GDD.md). Normal and EM1 differ only in selection weights and cost; BB1 selects the same entry set with probability one.

```mermaid
flowchart TD
    Start["Select purchase mode and base stake"]
    Cost["Charge once: Normal 1x, EM1 2x, BB1 PAR price"]
    Set{"First draw: BG set"}
    Regular["BG-0: spin five regular/wild reels"]
    Entry["BG-1: spin entry reels; three Scatters; zero line wins"]
    Resolve["Evaluate BG lines; apply cap"]
    Cap{"Cap reached?"}
    Trigger{"Three Scatters?"}
    Init["Eight Freegames; Heat = 1"]
    Free["Spin FG-0; evaluate lines with current Heat"]
    Win["Apply remaining round cap"]
    CapFG{"Cap reached?"}
    Heat["Positive win: next Heat +1, max 5; otherwise unchanged"]
    Left{"Spins remain?"}
    End["Settle and clear state"]
    Start --> Cost --> Set
    Set -->|BG-0| Regular --> Resolve
    Set -->|BG-1| Entry --> Resolve
    Resolve --> Cap
    Cap -->|Yes| End
    Cap -->|No| Trigger
    Trigger -->|No| End
    Trigger -->|Yes| Init --> Free --> Win --> CapFG
    CapFG -->|Yes| End
    CapFG -->|No| Heat --> Left
    Left -->|Yes| Free
    Left -->|No| End
```

All spins use the supplied session RNG. A mode is not redrawn inside a round. Replay recalculates every screen and Heat change from its recorded inputs.
