package simulation.result;

import simulation.stats.StandardStats;
import simulation.replay.SavedGameplay;
import java.util.List;
import java.util.Map;

public final class SimulationResult {

    private final StandardStats totalGameStats;
    private final StandardStats baseGameStats;
    private final StandardStats freeGameStats;
    private final Map<Integer, StandardStats> baseGameSetStats;
    private final Map<Integer, StandardStats> freeGameSetStats;
    private final List<SavedGameplay> savedGameplays;
    private final long elapsedNanos;

    public SimulationResult(StandardStats totalGameStats,
            StandardStats baseGameStats, StandardStats freeGameStats, long elapsedNanos) {
        this(totalGameStats, baseGameStats, freeGameStats, Map.of(), Map.of(),
                List.of(), elapsedNanos);
    }

    public SimulationResult(StandardStats totalGameStats,
            StandardStats baseGameStats, StandardStats freeGameStats,
            Map<Integer, StandardStats> baseGameSetStats,
            Map<Integer, StandardStats> freeGameSetStats,
            long elapsedNanos) {
        this(totalGameStats, baseGameStats, freeGameStats, baseGameSetStats,
                freeGameSetStats, List.of(), elapsedNanos);
    }

    public SimulationResult(StandardStats totalGameStats,
            StandardStats baseGameStats, StandardStats freeGameStats,
            Map<Integer, StandardStats> baseGameSetStats,
            Map<Integer, StandardStats> freeGameSetStats,
            List<SavedGameplay> savedGameplays,
            long elapsedNanos) {
        this.totalGameStats = totalGameStats;
        this.baseGameStats = baseGameStats;
        this.freeGameStats = freeGameStats;
        this.baseGameSetStats = Map.copyOf(baseGameSetStats);
        this.freeGameSetStats = Map.copyOf(freeGameSetStats);
        this.savedGameplays = List.copyOf(savedGameplays);
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

    public Map<Integer, StandardStats> getBaseGameSetStats() {
        return baseGameSetStats;
    }

    public Map<Integer, StandardStats> getFreeGameSetStats() {
        return freeGameSetStats;
    }

    public List<SavedGameplay> getSavedGameplays() {
        return savedGameplays;
    }

    public long getElapsedNanos() {
        return elapsedNanos;
    }
}
