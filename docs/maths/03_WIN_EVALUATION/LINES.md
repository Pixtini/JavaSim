# line evaluation

Defines the rules around winlines/lines/paylines.

**LIN-001 -** Winlines are defined by X integer indices where X is the number of reels.
**LIN-002 -** Winlines are given by a 0 based index of the screen height.
**LIN-003 -** Winlines can only contain one position from each reel.
**LIN-004 -** Winlines must be consectutive unless specified otherwise. 
**LIN-005 -** All winlines pay indivudally. 
**LIN-006 -** Winlines may overlap. 
**LIN-007 -** Only the highest paying amount on a winline is awarded.
**LIN-008 -** Winlines pay Left to Right (L2R) unless specified otherwise.
**LIN-009 -** When Winning on multiple paylines, all wins are added to the total win.
**LIN-010 -** Winline start from reel 1 , unless specified otherwise.

# Examples

Example of LIN-001/LIN-002, 3x3 grid size, the winline [0,1,2] Would be consist of the positions, (0,0),(1,1),(2,2). Diagonally downwards from top left to bottom right. 

Example of LIN-007, winline is made of [Wild, Wild, Wild, L1, L2], 3 of a kind wild pays 5x, and 4oak L1 pays 1x, then 5x is awarded for 3oak Wilds and the 4oak L1 is essentially ignored.