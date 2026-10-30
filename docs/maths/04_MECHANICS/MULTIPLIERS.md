# Multiplier

Defines the rules around multipliers.

**MUL-001 -** Multipliers can be in many forms defined by game design.
**MUL-002 -** If a multiplier is global, then it is applied to all wins that are summed from the win evaluation.
**MUL-003 -** A multiplier may only be valid for some line/cluster/way etc, those are applied as they are being added to total win.
**MUL-004 -** When multiple multipliers are involved they can be additive or multiplicative defined by game design.
**MUL-005 -** A multipled win still cannot exceed the win cap.
**MUL-006 -** When a global multi is not existant , the wins are not multiplied by 0. 

# Examples

Example of feature. A global multi is set at 2x , line wins for a spin are 10x, therefore the total win for the spin is 20x base bet.

Example of feature. A game has a feature where the wilds have multipliers, they only count for the line and are additive. A line consists of [T1, W(3x), T1, W(10x), W(8x)], therefore the win would 5oak T1 x (3+10+8). 

Example of feature. The above game also features a global mulitplier. The multipliers on wilds are additive and the global is multiplicative, the same line [T1, W(3x), T1, W(10x), W(8x)] and a global multiplier of 3x would lead to a win of size 5oak T1 x (3+10+8) x 3 = 5oak T1 x 63.