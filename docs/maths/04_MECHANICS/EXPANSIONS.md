# Symbol expansion

Defines the rules around expansion.

Expansion is sub feature of Transformations.

**EXP-001 -** A symbol is selected or designated to be the expanding symbol.
**EXP-002 -** An expanded size/shape is selected. 
**EXP-003 -** Expansions will respect exlcusion rules.
**EXP-004 -** An expanded symbol is part visual and part mechanical , while it looks like one big symbol, its positions on the grid mostly act as single symbols.

# Examples

Example of feature. A dedidcated wild symbol always expands to fill the reel after all reels land. Lets say this symbol lands in reel 1, on the 2nd row, (0,1) and the reel is 4 symbols high. Then the symbol will expand and fill the reel covering new positions (0,0), (0,2), (0,3). Across these positions visually this is one big 1x4 symbol. But those 4 positions are treated as 1x1 wilds for the purposes of win calculation. Each position will share the characteristics of the giant symbol, any multipliers or wild entitlement.   
