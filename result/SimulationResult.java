package result;

import stats.StandardStats;

public final class SimulationResult {

    private final StandardStats totalGameStats;
    private final StandardStats baseGameStats;
    private final StandardStats freeGameStats;
    private final long elapsedNanos;

    public SimulationResult(StandardStats totalGameStats,
            StandardStats baseGameStats, StandardStats freeGameStats, long elapsedNanos) {
        this.totalGameStats = totalGameStats;
        this.baseGameStats = baseGameStats;
        this.freeGameStats = freeGameStats;
        this.elapsedNanos = elapsedNanos;
    }

    public StandardStats getTotalGameStats() {
        return totalGameStats;
    }

    public StandardStats getBaseGameStats() {
        return baseGameStats;
    }

    public StandardStats getFreeGameStats() {
        return freeGameStats;
    }

    public long getElapsedNanos() {
        return elapsedNanos;
    }
}
