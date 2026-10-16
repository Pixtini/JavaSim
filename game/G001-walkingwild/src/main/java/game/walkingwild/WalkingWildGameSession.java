package game.walkingwild;

import game.GameSession;
import game.model.GameRoundResult;
import game.model.SpinResult;
import game.model.ReplayEvent;
import game.walkingwild.basegame.WalkingWildBaseGame;
import game.walkingwild.config.WalkingWildConfig;
import game.walkingwild.features.WalkingWildFreeGame;
import game.walkingwild.model.WildPosition;
import game.walkingwild.model.WalkingWildSpin;
import game.walkingwild.model.WalkingWildReplayPayload;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

/** Executes one complete G001 round and its scatter-triggered Freegames. */
public final class WalkingWildGameSession implements GameSession {
    private final WalkingWildConfig config;
    private final WalkingWildBaseGame baseGame;
    private final WalkingWildFreeGame freeGame;

    public WalkingWildGameSession(WalkingWildConfig config, Random random) {
        this.config = config;
        baseGame = new WalkingWildBaseGame(config, random);
        freeGame = new WalkingWildFreeGame(config, random);
    }

    @Override public GameRoundResult playRound() {
        return playRound(1.0, config.maxWinMultiplier());
    }

    @Override public GameRoundResult playRound(double baseStakeUnit) {
        return playRound(baseStakeUnit, config.maxWinMultiplier() * baseStakeUnit);
    }

    @Override public GameRoundResult playRound(double baseStakeUnit, double maximumWin) {
        return playRound(baseStakeUnit, maximumWin, false);
    }

    @Override public GameRoundResult playRound(double baseStakeUnit, double maximumWin,
            boolean captureReplay) {
        if (!Double.isFinite(baseStakeUnit) || baseStakeUnit <= 0.0
                || !Double.isFinite(maximumWin) || maximumWin <= 0.0) {
            throw new IllegalArgumentException("G001 stake and cap must be positive and finite");
        }
        var baseOutcome = baseGame.spin(baseStakeUnit, captureReplay);
        SpinResult baseResult = baseOutcome.result().cappedAt(maximumWin);
        boolean capped = baseOutcome.result().getWin() >= maximumWin;
        double totalWin = baseResult.getWin();
        double remaining = maximumWin - totalWin;
        List<SpinResult> freeResults = new ArrayList<>();

        int freeGames = baseOutcome.result().hasFreeSpin()
                ? freeGamesFor(baseOutcome.scatterCount()) : 0;
        List<WildPosition> persistedWilds = List.of();
        for (int index = 0; index < freeGames && !capped; index++) {
            var freeOutcome = freeGame.spin(baseStakeUnit, persistedWilds, captureReplay);
            double rawFreeWin = freeOutcome.result().getWin();
            SpinResult cappedFreeResult = freeOutcome.result().cappedAt(remaining);
            freeResults.add(cappedFreeResult);
            totalWin += cappedFreeResult.getWin();
            remaining = maximumWin - totalWin;
            persistedWilds = freeOutcome.wildPositions();
            if (rawFreeWin >= remaining + cappedFreeResult.getWin()) capped = true;
        }
        return new GameRoundResult(baseResult, freeResults, capped);
    }

    @Override
    public GameRoundResult replayRound(double baseStakeUnit, double maximumWin,
            List<ReplayEvent> events) {
        if (events == null || events.isEmpty() || !events.get(0).type().equals("walkingwild-base")) {
            throw new IllegalArgumentException("G001 replay must start with a Basegame event");
        }
        WalkingWildReplayPayload basePayload = WalkingWildReplayPayload.parse(events.get(0).payload());
        WalkingWildSpin base = baseGame.replay(basePayload, baseStakeUnit);
        List<SpinResult> features = new ArrayList<>();
        List<WildPosition> persistedWilds = List.of();
        for (int index = 1; index < events.size(); index++) {
            if (!events.get(index).type().equals("walkingwild-free")) {
                throw new IllegalArgumentException("G001 replay contains an unknown feature event");
            }
            WalkingWildReplayPayload payload = WalkingWildReplayPayload.parse(events.get(index).payload());
            WalkingWildSpin freeSpin = freeGame.replay(payload, baseStakeUnit, persistedWilds);
            features.add(freeSpin);
            persistedWilds = freeSpin.wildPositions();
        }
        return new GameRoundResult(base, features).cappedAt(maximumWin);
    }

    private int freeGamesFor(int scatterCount) {
        return config.scatterFreeGames().entrySet().stream()
                .filter(entry -> entry.getKey() <= scatterCount)
                .max(java.util.Map.Entry.comparingByKey())
                .map(java.util.Map.Entry::getValue).orElse(0);
    }
}
