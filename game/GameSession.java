package game;

import game.model.GameRoundResult;

/** Executes one complete basegame round, including any triggered freegame spins. */
public interface GameSession {
    GameRoundResult playRound();

    /** Plays a round with the configured total round stake. */
    default GameRoundResult playRound(double totalRoundStake) {
        return playRound();
    }
}
