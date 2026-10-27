Winlines - 

Winlines are given by a 0 based index of the screen height.
Winlines can only contain 1 position from each reel.
Winlines must be consectutive unless specified otherwise. 
All winlines pay indivudally. 
Winlines may overlap. 
Only the highest paying amount on a winline is awarded

, for example winline 1 is made of [Wild, Wild, Wild, L1, L2], 3 of a kind wild pays 5x, and 4oak L1 pays 1x, then 5x is awarded for 3oak Wilds and the 4oak L1 is essentially ignored.

A game may feature a win cap. The maximun win for basegame + freegame added togethor cannot exceed that cap. The cap clips the winning spin and ends the freegames there if half way through a round.

Sets - 

A game may feature multiple config options defined by sets. The set is the first thing to be selected by a table of weights for each spin. and any features are played using that sets weights for that spin. It is often normal that some feature are disabled or enabled by weights using the config. 


Recorded gameplay balancing -

After the game is implemented and its normal mathematical configuration is balanced, humans can harvest complete simulation replays and reweight them through `<Mode>-Books` sheets. Each selected book entry represents the whole paid round, including its feature outcomes. Book simulation samples only those recorded rounds using relative weights; it does not spin new reels or redraw feature decisions. Payouts still scale with base stake and mode cost affects exposure only. Reweighting changes the resulting trigger/win distribution, so mode relationships must be checked against the balanced book itself. Missing book inputs block book-enabled runs. See [recorded gameplay books](../../docs/gameplay-books.md).
