package game;

import model.GameRoundResult;

/** Executes one complete basegame round, including any triggered freegame spins. */
public interface GameSession {
    GameRoundResult playRound();
}
