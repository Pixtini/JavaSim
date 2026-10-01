public final class SimulationResult {

    private final standardStats totalGameStats;
    private final standardStats baseGameStats;
    private final standardStats freeGameStats;

    public SimulationResult(standardStats totalGameStats,
            standardStats baseGameStats, standardStats freeGameStats) {
        this.totalGameStats = totalGameStats;
        this.baseGameStats = baseGameStats;
        this.freeGameStats = freeGameStats;
    }

    public standardStats getTotalGameStats() {
        return totalGameStats;
    }

    public standardStats getBaseGameStats() {
        return baseGameStats;
    }

    public standardStats getFreeGameStats() {
        return freeGameStats;
    }
}
