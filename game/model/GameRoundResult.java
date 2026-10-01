package game.model;

import java.util.List;

/** The outcomes produced by one basegame round and its triggered freegame spins. */
public final class GameRoundResult {
    private final SpinResult baseGameResult;
    private final List<SpinResult> freeGameResults;

    public GameRoundResult(SpinResult baseGameResult, List<SpinResult> freeGameResults) {
        this.baseGameResult = baseGameResult;
        this.freeGameResults = List.copyOf(freeGameResults);
    }

    public SpinResult getBaseGameResult() {
        return baseGameResult;
    }

    public List<SpinResult> getFreeGameResults() {
        return freeGameResults;
    }
}
