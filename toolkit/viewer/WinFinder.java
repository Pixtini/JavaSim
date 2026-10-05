package toolkit.viewer;

import game.Game;
import game.model.GameRoundResult;
import game.model.SpinResult;
import java.util.Objects;
import java.util.Random;

/** Plays complete rounds until the first winning spin or the spin limit. */
public final class WinFinder {
    private final Game game;
    private final Random random;
    private final double stake;
    private final long maximumSpins;

    public WinFinder(Game game, Random random, double stake, long maximumSpins) {
        this.game = Objects.requireNonNull(game, "game");
        this.random = Objects.requireNonNull(random, "random");
        if (!Double.isFinite(stake) || stake <= 0.0) {
            throw new IllegalArgumentException("Stake must be finite and greater than zero");
        }
        if (maximumSpins <= 0) {
            throw new IllegalArgumentException("Maximum spins must be greater than zero");
        }
        this.stake = stake;
        this.maximumSpins = maximumSpins;
    }

    public Result find() {
        long spinsExamined = 0;
        var session = game.createSession(random);
        while (spinsExamined < maximumSpins) {
            GameRoundResult round = session.playRound(stake);
            SpinResult baseResult = round.getBaseGameResult();
            if (baseResult.getWin() > 0.0) {
                return new Result(spinsExamined + 1, round, baseResult, false);
            }
            spinsExamined++;

            for (SpinResult freeGameResult : round.getFreeGameResults()) {
                if (spinsExamined >= maximumSpins) {
                    break;
                }
                if (freeGameResult.getWin() > 0.0) {
                    return new Result(spinsExamined + 1, round, freeGameResult, true);
                }
                spinsExamined++;
            }
        }
        return new Result(spinsExamined, null, null, false);
    }

    public record Result(long spinsPlayed, GameRoundResult winningRound,
            SpinResult winningSpin, boolean freeGameWin) {
        public boolean foundWin() {
            return winningSpin != null;
        }
    }
}
