# Stake, payout, and settlement

Define wager amounts, payout units, rounding, and the calculation of final payouts.

**MON-001 -** Player uses base stake amount, b. Usually defined as 1 for £1 or 100 for 100pence.
**MON-002 -** The player is charged b * cost for each round. Cost is default 1 for Normal mode and set by config for other modes.
**MON-003 -** Wins are awarded as multiplier of base stake amount b. 
**MON-004 -** Win multpliers cannot be under 0.05 and must be in 0.05 increments at least. 0.1 is preferred. 
**MON-005 -** The total win in a round can exceed the win cap. See WIN_CAPS.md for more.
**MON-006 -** The total win for a round is the sum of all wins within that round. 
**MON-007 -** Base stake is impossible to ammend during a round. Only after a round is concluded may the player increase the stake.
