This document outlines the behaviour which is consistent across all lined slot games.

Reels -

The game will feature X reels, that where only some symbols are visible at a time. The reels are made up on symbol IDs which correlate to symbols. On any spin 1 reel stop is selected at random per reel and that is the top most position that is visible. The reels form a loop, hence at the end of the reels it starts again at reel index 0, so if index Y is selected being the maximun length and there are 4 symbols to be shown, Y, 0, 1, 2 will be shown.

Winlines - 

Winlines are given by a 0 based index of the screen height. Winlines can only contain 1 position from each reel. Winlines must be consectutive unless specified otherwise. All winlines pay indivudally. Winlines may overlap. Only the highest paying amount on a winline is awarded, for example winline 1 is made of [Wild, Wild, Wild, L1, L2], 3 of a kind wild pays 5x, and 4oak L1 pays 1x, then 5x is awarded for 3oak Wilds and the 4oak L1 is essentially ignored.

Pays -

The default cost for a game is 1 unit. Modes may multiple this cost by some amount. The base unit remains at 1 unit regardless of any multiple of cost. Any payout from any line is a multiple of the base unit. Hence if a payout is 20x, and we are on a £2 stake. Then thats 20 units of £2 for £40. If we are on a bonus buy of 100x stake on £2 stake, the total cost per round is £200, that same 20x win is still £40, using the base unit of £2.


Wins must be in at least multiples of 0.05 of a unit. 

A game may feature a win cap. The maximun win for basegame + freegame added togethor cannot exceed that cap. The cap clips the winning spin and ends the freegames there if half way through a round.

Modes - 

Every game contains a normal mode. This is usually cost of 1x stake. Games may feature other modes of different multipliers of stake. The logic is the exact same , and is usally only a PAR config change to force some behaviours to happen. The player selects which mode he plays, the stake and then is charged the multiple of that stake and plays the game using the appropiate config. 

The mode is selected before the player presses spin. The default is normal. Only one mode can be selected at a time. The config used is whatever matches the mode the player has enabled.

Basegame Freegames - 

The player always starts on Basegame, there may be an route towards a freespins round, usually awarded by a minimun amount of scatters. A scatter map will confirm these amounts. 

Sets - 

A game may feature multiple config options defined by sets. The set is the first thing to be selected by a table of weights for each spin. and any features are played using that sets weights for that spin. It is often normal that some feature are disabled or enabled by weights using the config. 

Math Approval - 

Math Approval is done seperately for any game, a GDD will not define that behaviour for any goals or targets. A succesful engine is a bug free engine which accurately acheives expected behaviours. 

Wilds -

If present , wilds subsitute for every regular symbol. They can also create their own wins if there payout is the highest available on that line. 

Scatters - 

If present, scatters award anywhere on the screen, they may have a payout or may trigger a feature like freegames.

Recorded gameplay balancing -

After the game is implemented and its normal mathematical configuration is balanced, humans can harvest complete simulation replays and reweight them through `<Mode>-Books` sheets. Each selected book entry represents the whole paid round, including its feature outcomes. Book simulation samples only those recorded rounds using relative weights; it does not spin new reels or redraw feature decisions. Payouts still scale with base stake and mode cost affects exposure only. Reweighting changes the resulting trigger/win distribution, so mode relationships must be checked against the balanced book itself. Missing book inputs block book-enabled runs. See [recorded gameplay books](../../docs/gameplay-books.md).
