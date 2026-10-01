import java.util.HashMap;
import java.util.Map;
public class standardStats {

    private double totalWinnings;
    private double rtp;
    private double totalStaked;
    private long rounds;
    private long hits;
    private int freegameTriggers;
    private int[] paytable;
    private double standardDeviation;
    private final Map<Double, Long> winDist = new HashMap<>();
    

    public standardStats(double stake, int[] paytable) {
        this.totalStaked = 0.0;
        this.paytable = paytable.clone();
        this.stake = stake;
    }

    private final double stake;

    public void addResult(spinResult result) {
        this.totalWinnings += result.getWin();
        this.paytable[result.getWinSize()]++;
        this.rounds++;
        winDist(result.getWin());

    }

    public void calculateStats() {
        this.totalStaked = rounds * stake;
        this.rtp = totalWinnings / totalStaked;
        this.hits = rounds - paytable[0];
        this.standardDeviation = standardDeviation(winDist);


    } 

    public double getTotalWinnings() {
        return totalWinnings;
    }

    public double getRtp() {
        calculateStats();
        return rtp;
    }

    public long getRounds() {
        return rounds;
    }

    public long getHits() {
        calculateStats();
        return hits;
    }

    public int[] getPaytable() {
        return paytable.clone();
    }

    public int getFreegameTriggers() {
        return freegameTriggers;
    }

    public void recordFreegameTrigger() {
        freegameTriggers++;
    }

    public double getStandardDeviation() {
        return standardDeviation(winDist);
    }

    public Map<Double, Long> getWinDist() {
        return Map.copyOf(winDist);
    }

    public double standardDeviation(Map<Double, Long> winDist) {
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

    public void winDist(double win) {
        winDist.merge(win, 1L, Long::sum);

    } 


}
