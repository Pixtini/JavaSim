package game.expandingwild;

import game.GameSession;
import game.expandingwild.basegame.ExpandingWildBaseGame;
import game.expandingwild.config.ExpandingWildConfig;
import game.expandingwild.features.ExpandingWildFreeGame;
import game.expandingwild.model.ExpandingWildSpinResult;
import game.model.GameRoundResult;
import game.model.ReplayEvent;
import game.model.SpinResult;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;

/** Owns one partition-local game session, using only its supplied random stream. */
public final class ExpandingWildGameSession implements GameSession {
    private final ExpandingWildConfig config;
    private final ExpandingWildBaseGame baseGame;
    private final ExpandingWildFreeGame freeGame;

    public ExpandingWildGameSession(ExpandingWildConfig config, Random random) {
        this.config = config;
        this.baseGame = new ExpandingWildBaseGame(config, random);
        this.freeGame = new ExpandingWildFreeGame(config, random);
    }

    @Override
    public GameRoundResult playRound() {
        return playRound(1.0, config.maxWinMultiplier);
    }

    @Override
    public GameRoundResult playRound(double totalRoundStake) {
        return playRound(totalRoundStake, config.maxWinMultiplier * totalRoundStake);
    }

    @Override
    public GameRoundResult playRound(double totalRoundStake, double maximumWin) {
        return playRound(totalRoundStake, maximumWin, false);
    }

    @Override
    public GameRoundResult playRound(double totalRoundStake, double maximumWin,
            boolean captureReplay) {
        var rawBaseResult = baseGame.spin(totalRoundStake, captureReplay);
        var baseResult = rawBaseResult.cappedAt(maximumWin);
        double roundWin = baseResult.getWin();
        double remainingWin = Math.max(0.0, maximumWin - roundWin);
        boolean capReached = rawBaseResult.getWin() >= maximumWin;
        List<SpinResult> freeResults = new ArrayList<>();
        if (baseResult.hasFreeSpin() && !capReached) {
            for (int freeGameIndex = 0; freeGameIndex < config.freeGamesAwarded; freeGameIndex++) {
                var rawFreeResult = freeGame.spin(totalRoundStake, captureReplay);
                double availableBeforeSpin = remainingWin;
                SpinResult freeResult = capToRemainingWin(
                        rawFreeResult, roundWin, maximumWin, availableBeforeSpin);
                freeResults.add(freeResult);
                roundWin = Math.min(maximumWin, roundWin + freeResult.getWin());
                remainingWin = Math.max(0.0, maximumWin - roundWin);
                if (rawFreeResult.getWin() >= availableBeforeSpin || remainingWin == 0.0) {
                    capReached = true;
                    break;
                }
            }
        }
        return new GameRoundResult(baseResult, freeResults, capReached);
    }

    private SpinResult capToRemainingWin(SpinResult result, double accumulatedWin,
            double maximumWin, double remainingWin) {
        if (remainingWin == 0.0) {
            return result.cappedAt(0.0);
        }
        SpinResult capped = result.cappedAt(remainingWin);
        if (accumulatedWin + capped.getWin() > maximumWin) {
            capped = result.cappedAt(Math.nextDown(remainingWin));
            if (accumulatedWin + capped.getWin() > maximumWin) {
                capped = result.cappedAt(0.0);
            }
        }
        return capped;
    }

    @Override
    public GameRoundResult replayRound(double totalRoundStake, double maximumWin,
            List<ReplayEvent> events) {
        Objects.requireNonNull(events, "events");
        if (events.isEmpty() || !events.get(0).type().equals("basegame")) {
            throw new IllegalArgumentException("Replay must begin with one basegame event");
        }
        ExpandingWildSpinResult baseResult = baseGame.replay(events.get(0), totalRoundStake);
        if (!baseResult.hasFreeSpin() && events.size() > 1) {
            throw new IllegalArgumentException("A non-triggering basegame cannot have feature spins");
        }
        if (events.size() - 1 > config.freeGamesAwarded) {
            throw new IllegalArgumentException("Replay contains more freegames than configured");
        }
        List<SpinResult> freeResults = new ArrayList<>();
        for (int index = 1; index < events.size(); index++) {
            ReplayEvent event = events.get(index);
            if (!event.type().equals("freegame")) {
                throw new IllegalArgumentException("Unsupported expanding-wild event: " + event.type());
            }
            freeResults.add(freeGame.replay(event, totalRoundStake));
        }
        GameRoundResult replayed = new GameRoundResult(baseResult, freeResults).cappedAt(maximumWin);
        if (baseResult.hasFreeSpin() && freeResults.size() < config.freeGamesAwarded
                && !replayed.isWinCapReached()) {
            throw new IllegalArgumentException("Replay is missing freegame events");
        }
        return replayed;
    }
}
