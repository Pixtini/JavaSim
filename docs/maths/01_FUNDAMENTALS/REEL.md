# Reels

This outlines the rules and considerations for reels.

**REE-001 -** A game will have multiple reels.
**REE-002 -** A reel consists of sequence N symbols given by their symbol ID which is an integer number.
**REE-003 -** A symbol on the reel can be referred to by its reel index. 0 -> N-1.
**REE-004 -** When the reels are spun, its a selection of the indexes uniformly.
**REE-005 -** The selected index defines the top symbol on row 0 of the grid.
**REE-006 -** The remaining symbols shown on the grid are the consectutive symbols of that reel from the selected index.
**REE-007 -** The reels wrap around. After index N-1, the next symbol is index 0.

# Examples

Example of REE-006, A reel is 10 symbols long, and the grid is 3 positions tall. Uniformly random, reel index 5 is selected. The shown symbols on that column of the grid is index 5, index 6 and index 7. 

Example of REE-007, Reel index 9 in randomly selected. The shown symbols on that column of the grid is index 9, index 0 and index 1.