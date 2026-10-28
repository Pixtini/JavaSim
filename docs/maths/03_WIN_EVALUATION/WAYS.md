# Ways evaluation

Defines the rules around ways/anyways/all-ways/winning ways.

**WAY-001 -** Ways are given by every single possible winline. They arnt defined anywhere in the config. 
**WAY-002 -** Each winning ways can only contain one position from each reel.
**WAY-003 -** Winning ways must be consectutive unless specified otherwise. 
**WAY-004 -** All winning ways pay indivudally. 
**WAY-005 -** Only the highest paying amount on a winning way is awarded.
**WAY-006 -** Winning ways pay Left to Right (L2R) unless specified otherwise.
**WAY-007 -** When Winning on multiple winning ways, all wins are added to the total win.

# Examples

Example of WAY-001, 3x3 grid size, there are 3^3 possible winning ways. 

Example of WAY-006, winning ways is made of [Wild, Wild, Wild, L1, L2], 3 of a kind wild pays 5x, and 4oak L1 pays 1x, then 5x is awarded for 3oak Wilds and the 4oak L1 is essentially ignored.