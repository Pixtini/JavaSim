package toolkit.reelset;

import java.util.Map;

/** Aggregated results from one complete Cartesian traversal of reel stops. */
public record ReelsetSimulationResult(long combinations, long featureTriggers,
        double totalStake, double totalWinnings, Map<String, Long> awardHits) {
    public ReelsetSimulationResult {
        awardHits = Map.copyOf(awardHits);
    }

    public double rtp() {
        return totalStake == 0.0 ? 0.0 : totalWinnings / totalStake;
    }
}
