package simulation.stats;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Collections;
import game.model.SpinResult;

public class StandardStats {

    private static final double[] WIN_BAND_UPPER_BOUNDS = {
            0, 1, 2, 3, 5, 10, 20, 50, 100, 200,
            500, 1_000, 2_000, 5_000, 10_000, 15_000,
            20_000, 25_000, 10_000_000
    };

    private double totalWinnings;
    private long rounds;
    private long freegameTriggers;
    private long winCaps;
    private final int[] paytable;
    private final Map<Double, Long> winDist = new HashMap<>();
    private final Map<String, Long> awardCounts = new LinkedHashMap<>();
    private final double stake;
    private double totalStakeBasis;

    public StandardStats(double stake, int[] paytable) {
        this(stake, paytable, List.of());
    }

    public StandardStats(double stake, int[] paytable, List<String> awardLabels) {
        this.paytable = paytable.clone();
        this.stake = stake;
        for (String awardLabel : awardLabels) {
            if (awardCounts.putIfAbsent(awardLabel, 0L) != null) {
                throw new IllegalArgumentException("Duplicate award label: " + awardLabel);
            }
        }
    }

    public void addResult(SpinResult result) {
        addResult(result, stake);
    }

    /** Adds a spin result and the stake contribution attributed to this stats series. */
    public void addResult(SpinResult result, double stakeContribution) {
        this.totalWinnings += result.getWin();
        this.paytable[result.getWinSize()]++;
        this.rounds++;
        this.totalStakeBasis += stakeContribution;
        for (String award : result.getAwards()) {
            awardCounts.merge(award, 1L, Long::sum);
        }
    }

    /** Adds stake exposure without adding a separately staked result, for freegame RTP. */
    public void recordStakeBasis(double stakeContribution) {
        totalStakeBasis += stakeContribution;
    }

    public double getTotalWinnings() {
        return totalWinnings;
    }

    public void mergeFrom(StandardStats other) {
        if (paytable.length != other.paytable.length) {
            throw new IllegalArgumentException("Cannot merge statistics with different paytable sizes");
        }

        totalWinnings += other.totalWinnings;
        rounds += other.rounds;
        freegameTriggers += other.freegameTriggers;
        winCaps += other.winCaps;
        totalStakeBasis += other.totalStakeBasis;
        for (int i = 0; i < paytable.length; i++) {
            paytable[i] += other.paytable[i];
        }
        other.awardCounts.forEach((award, count) -> awardCounts.merge(award, count, Long::sum));
        other.winDist.forEach((win, count) -> winDist.merge(win, count, Long::sum));
    }

    public double getRtp() {
        return getRtpForTotalStake(totalStakeBasis);
    }

    /** Calculates RTP against a caller-supplied stake basis, such as basegame stake. */
    public double getRtpForTotalStake(double totalStake) {
        return totalWinnings / totalStake;
    }

    public long getRounds() {
        return rounds;
    }

    public long getHits() {
        return rounds - paytable[0];
    }

    public int[] getPaytable() {
        return paytable.clone();
    }

    /** Returns named award counts, including configured categories with zero hits. */
    public Map<String, Long> getAwardCounts() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(awardCounts));
    }

    public long getFreegameTriggers() {
        return freegameTriggers;
    }

    public void recordFreegameTrigger() {
        freegameTriggers++;
    }

    public long getWinCaps() {
        return winCaps;
    }

    public void recordWinCap() {
        winCaps++;
    }

    public double getStandardDeviation() {
        return standardDeviation(winDist);
    }

    public Map<Double, Long> getWinDist() {
        return Map.copyOf(winDist);
    }

    public List<WinBand> getAggregatedWinDistribution(double totalStaked) {
        long totalHits = 0;
        double totalDistributionWinnings = 0.0;
        for (Map.Entry<Double, Long> entry : winDist.entrySet()) {
            totalHits += entry.getValue();
            totalDistributionWinnings += entry.getKey() * entry.getValue();
        }

        List<WinBand> bands = new ArrayList<>(WIN_BAND_UPPER_BOUNDS.length);
        double lowerBound = 0.0;
        for (int bandIndex = 0; bandIndex < WIN_BAND_UPPER_BOUNDS.length; bandIndex++) {
            double upperBound = WIN_BAND_UPPER_BOUNDS[bandIndex];
            long bandHits = 0;
            double bandWinnings = 0.0;

            for (Map.Entry<Double, Long> entry : winDist.entrySet()) {
                double win = entry.getKey();
                boolean inBand = bandIndex == 0
                        ? win <= upperBound
                        : win > lowerBound && win <= upperBound;
                if (inBand) {
                    bandHits += entry.getValue();
                    bandWinnings += win * entry.getValue();
                }
            }

            double percentOfWinnings = totalDistributionWinnings == 0.0
                    ? 0.0 : bandWinnings / totalDistributionWinnings * 100.0;
            double percentOfHits = totalHits == 0
                    ? 0.0 : (double) bandHits / totalHits * 100.0;
            double frequency = bandHits == 0
                    ? 0.0 : (double) totalHits / bandHits;
            double bandRtp = totalStaked == 0.0
                    ? 0.0 : bandWinnings / totalStaked * 100.0;

            bands.add(new WinBand(
                    lowerBound,
                    upperBound,
                    bandHits,
                    percentOfWinnings,
                    percentOfHits,
                    frequency,
                    bandRtp,
                    bandWinnings));
            lowerBound = upperBound;
        }
        return List.copyOf(bands);
    }

    public static final class WinBand {
        private final double lowerBound;
        private final double upperBound;
        private final long hits;
        private final double percentOfWinnings;
        private final double percentOfHits;
        private final double frequency;
        private final double rtp;
        private final double totalWin;

        private WinBand(double lowerBound, double upperBound, long hits,
                double percentOfWinnings, double percentOfHits,
                double frequency, double rtp, double totalWin) {
            this.lowerBound = lowerBound;
            this.upperBound = upperBound;
            this.hits = hits;
            this.percentOfWinnings = percentOfWinnings;
            this.percentOfHits = percentOfHits;
            this.frequency = frequency;
            this.rtp = rtp;
            this.totalWin = totalWin;
        }

        public double getLowerBound() { return lowerBound; }
        public double getUpperBound() { return upperBound; }
        public long getHits() { return hits; }
        public double getPercentOfWinnings() { return percentOfWinnings; }
        public double getPercentOfHits() { return percentOfHits; }
        public double getFrequency() { return frequency; }
        public double getRtp() { return rtp; }
        public double getTotalWin() { return totalWin; }
    }

    private double standardDeviation(Map<Double, Long> winDist) {
        long totalCount = 0;
        double weightedTotal = 0.0;

        for (Map.Entry<Double, Long> entry : winDist.entrySet()) {
            long count = entry.getValue();
            totalCount += count;
            weightedTotal += entry.getKey() * count;
        }

        if (totalCount == 0) {
            return Double.NaN;
        }

        double mean = weightedTotal / totalCount;
        double weightedSquaredDifferences = 0.0;
        for (Map.Entry<Double, Long> entry : winDist.entrySet()) {
            double difference = entry.getKey() - mean;
            weightedSquaredDifferences += difference * difference * entry.getValue();
        }

        // This is the population standard deviation of the observed wins.
        return Math.sqrt(weightedSquaredDifferences / totalCount);
    }

    public void recordWinInDistribution(double win) {
        winDist.merge(win, 1L, Long::sum);
    } 


}
