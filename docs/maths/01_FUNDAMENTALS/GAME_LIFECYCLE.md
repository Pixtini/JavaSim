# Game life cycle

Define the stages of a game round and the order in which they occur.

This is assuming , game , mode and bet amount has been selected already.

**GLC-001 -** Player confirms bet (Presses Spin). Spin button becomes inactive.
**GLC-002 -** Bet amount is taken from wallet.
**GLC-003 -** Initial state is provided from a RNG pull. 
**GLC-004 -** Initial state is shown to the player.
**GLC-005 -** Game play mechanics are resolved according to rule set until a final resolution.
**GLC-006 -** Total payout is awarded to wallet. 
**GLC-007 -** Spin button becomes active, waiting for next bet.

The above can be considered to be a Round.

