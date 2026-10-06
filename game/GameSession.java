package game;

import game.model.GameRoundResult;
import game.model.ReplayEvent;
import java.util.List;

/** Executes one complete basegame round, including any triggered freegame spins. */
public interface GameSession {
    GameRoundResult playRound();

    /** Plays a round with the configured total round stake. */
    default GameRoundResult playRound(double totalRoundStake) {
        return playRound();
    }

    /** Plays a round under an absolute payout cap; games may override to stop feature generation early. */
    default GameRoundResult playRound(double totalRoundStake, double maximumWin) {
        return playRound(totalRoundStake).cappedAt(maximumWin);
    }

    /** Plays a round, optionally collecting replay payloads for its events. */
    default GameRoundResult playRound(double totalRoundStake, double maximumWin,
            boolean captureReplay) {
        return playRound(totalRoundStake, maximumWin);
    }

    /** Recalculates a recorded round from game-specific replay events. */
    default GameRoundResult replayRound(double totalRoundStake, double maximumWin,
            List<ReplayEvent> events) {
        throw new UnsupportedOperationException("This game session does not support replay");
    }
}
