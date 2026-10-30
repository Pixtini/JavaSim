# Persistent state and meters

Defines the rules around persistence.

This is a feature for respins, and freegames only. Cascades persistence is implied.

**PER-001 -** Symbols on the grid, Multipliers and other side features may keep their state from spin to spin.
**PER-002 -** All persistence ends either at the end of the respin or end of freegames. It is not carried over into the next paid round.
**PER-003 -** For grid persistence, the sets, reels and features are still spun normally with the persisted symbols being overlayed on the result, the symbols on the position which the persistence fell cease to exist.
**PER-004 -** Any new global multiplier in the case of persistant multiplier is added to the global multiplier.
**PER-005 -** If a persisted symbol would move location it respects exclusions rules. A fixed symbol does not have to.

# Examples

Example of feature. A global multi is set at 2x on spin 1, for spin 2 it will still remain at 2x, on spin 3 another 3x is added making the new global multi 5x, for the remainder of free spins the global multi is 5x applied end of each spin.

Example of feature. Wilds are sticky and remain on the screen from spin to spin in the location it landed. A wild lands on (0,0) and (2,2) on a spin. The next spin, the positions of the wilds are remembered, the reels are spun, the wilds stay in place, any symbol that lands "behind" those wilds cease to exist and are not shown to the player. 