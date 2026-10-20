# Game flow — {{game name}}

Status: draft. Fill operation labels and rule references from the GDD. This skeleton depicts one Basegame and one Freegame; amend ordering to match the chosen trigger/cap rules without introducing another feature mode.

```mermaid
flowchart TD
    Start["Select Normal, EM1 or BB1 and base stake"]
    Charge["Lock config; charge once using mode cost"]
    BG["Select BG set; generate legal screen or BB1 trigger entry"]
    ResolveBG["Apply specified BG operations; evaluate pays and trigger"]
    CapBG{"Round cap reached?"}
    Trigger{"Single Freegame awarded?"}
    Init["Initialize the same Freegame"]
    FG["Select FG set; generate and resolve one free spin"]
    CapFG{"Round cap reached?"}
    Left{"Spins remaining under the same FG rules?"}
    End["Settle capped round; clear round state"]

    Start --> Charge --> BG --> ResolveBG --> CapBG
    CapBG -->|Yes| End
    CapBG -->|No| Trigger
    Trigger -->|No| End
    Trigger -->|Yes| Init --> FG --> CapFG
    CapFG -->|Yes| End
    CapFG -->|No| Left
    Left -->|Yes| FG
    Left -->|No| End
```

## Operation sequence

| Step / rule ID | Random draw | State read / written | Award / cap / transition |
|---|---|---|---|
| {{step}} | {{draw or none}} | {{state}} | {{expected behavior}} |

Show Normal/EM1 probability differences and BB1 forced entry explicitly in the text, retaining the same FG loop. Explain any retrigger ordering and reset.

