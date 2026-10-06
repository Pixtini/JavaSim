package game.model;

import java.util.List;

/** The outcomes produced by one basegame round and its triggered freegame spins. */
public final class GameRoundResult {
    private final SpinResult baseGameResult;
    private final List<SpinResult> freeGameResults;
    private final boolean winCapReached;
    private final List<ReplayEvent> replayEvents;

    public GameRoundResult(SpinResult baseGameResult, List<SpinResult> freeGameResults) {
        this(baseGameResult, freeGameResults, false);
    }

    public GameRoundResult(SpinResult baseGameResult, List<SpinResult> freeGameResults,
            boolean winCapReached) {
        this(baseGameResult, freeGameResults, winCapReached,
                replayEventsFrom(baseGameResult, freeGameResults));
    }

    /** Creates a round with an explicit ordered event stream for any feature structure. */
    public GameRoundResult(SpinResult baseGameResult, List<SpinResult> freeGameResults,
            boolean winCapReached, List<ReplayEvent> replayEvents) {
        this.baseGameResult = baseGameResult;
        this.freeGameResults = List.copyOf(freeGameResults);
        this.winCapReached = winCapReached;
        this.replayEvents = List.copyOf(replayEvents);
    }

    public SpinResult getBaseGameResult() {
        return baseGameResult;
    }

    public List<SpinResult> getFreeGameResults() {
        return freeGameResults;
    }

    public boolean isWinCapReached() {
        return winCapReached;
    }

    /** Replay events in the order they occurred, including all feature spins. */
    public List<ReplayEvent> getReplayEvents() {
        return replayEvents;
    }

    /** Applies a shared round cap, clipping the spin that reaches it and discarding later spins. */
    public GameRoundResult cappedAt(double maximumWin) {
        if (!Double.isFinite(maximumWin) || maximumWin <= 0.0) {
            if (maximumWin == Double.POSITIVE_INFINITY) {
                return this;
            }
            throw new IllegalArgumentException("Maximum win must be positive and finite");
        }

        double remaining = maximumWin;
        SpinResult cappedBase = baseGameResult.cappedAt(remaining);
        double totalWin = cappedBase.getWin();
        remaining = maximumWin - totalWin;
        boolean reached = baseGameResult.getWin() >= maximumWin;
        java.util.ArrayList<SpinResult> cappedFreeGames = new java.util.ArrayList<>();
        if (!reached) {
            for (SpinResult freeGameResult : freeGameResults) {
                SpinResult cappedFree = freeGameResult.cappedAt(remaining);
                while (totalWin + cappedFree.getWin() > maximumWin) {
                    cappedFree = freeGameResult.cappedAt(Math.nextDown(cappedFree.getWin()));
                }
                cappedFreeGames.add(cappedFree);
                double availableBeforeSpin = remaining;
                totalWin += cappedFree.getWin();
                remaining = maximumWin - totalWin;
                if (freeGameResult.getWin() >= availableBeforeSpin) {
                    reached = true;
                    break;
                }
            }
        }
        List<ReplayEvent> cappedEvents = updateEventWins(
                replayEvents, cappedBase, cappedFreeGames);
        return new GameRoundResult(cappedBase, cappedFreeGames, reached, cappedEvents);
    }

    private static List<ReplayEvent> replayEventsFrom(SpinResult base,
            List<SpinResult> features) {
        java.util.ArrayList<ReplayEvent> events = new java.util.ArrayList<>();
        base.getReplayEvent().ifPresent(events::add);
        features.stream().flatMap(result -> result.getReplayEvent().stream()).forEach(events::add);
        return events;
    }

    private List<ReplayEvent> updateEventWins(List<ReplayEvent> events,
            SpinResult cappedBase, List<SpinResult> cappedFeatures) {
        if (events.isEmpty()) {
            return events;
        }
        if (events.size() != 1 + cappedFeatures.size()) {
            throw new IllegalStateException(
                    "Capped replay events must correspond one-to-one with round spins");
        }
        java.util.ArrayList<ReplayEvent> updated = new java.util.ArrayList<>(events.size());
        updated.add(events.get(0).withWin(cappedBase.getWin()));
        for (int index = 0; index < cappedFeatures.size(); index++) {
            updated.add(events.get(index + 1).withWin(cappedFeatures.get(index).getWin()));
        }
        return List.copyOf(updated);
    }
}
