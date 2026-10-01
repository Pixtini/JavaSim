import java.util.HashMap;
import java.util.Map;
import config.SimConfig;
import config.GameConfig;


public class standardStats {

    private final spinResult result;
    double totalWinnings;
    double rtp;
    double totalStaked;
    long rounds;
    long hits;
    double freespins;
    int freegameTriggers;
    int[] paytable;
    double standardDeviation;
    Map<Double, Integer> winDist = new HashMap<>();
    

    public standardStats() {
        GameConfig gameConfig = new GameConfig();
        this.result = new spinResult(0.0 ,0,false);
        this.rounds = 0;
        this.totalWinnings = 0.0;
        this.paytable = gameConfig.paytable;
        this.freespins = 0.0;
        this.hits = 0;

    }

    public void addResult(spinResult result) {
        this.totalWinnings += result.win;
        this.paytable[result.winSize]++;
        this.rounds++;
        winDist(result.win);

    }

    public void calculateStats() {
        SimConfig simConfig = new SimConfig();
        this.totalStaked = rounds * simConfig.stake;
        this.rtp = totalWinnings / totalStaked;
        this.hits = rounds - paytable[0];
        this.standardDeviation = standardDeviation(winDist);


    } 

    public double standardDeviation(Map<Double, Integer> winDist) {
        long totalCount = 0;
        double weightedTotal = 0.0;

        for (Map.Entry<Double, Integer> entry : winDist.entrySet()) {
            int count = entry.getValue();
            totalCount += count;
            weightedTotal += entry.getKey() * count;
        }

        if (totalCount == 0) {
            return Double.NaN;
        }

        double mean = weightedTotal / totalCount;
        double weightedSquaredDifferences = 0.0;
        for (Map.Entry<Double, Integer> entry : winDist.entrySet()) {
            double difference = entry.getKey() - mean;
            weightedSquaredDifferences += difference * difference * entry.getValue();
        }

        // This is the population standard deviation of the observed wins.
        return Math.sqrt(weightedSquaredDifferences / totalCount);
    }

    public void winDist(double win) {
        if (winDist.containsKey(win)){
            winDist.computeIfPresent(win, (key, value) -> value + 1);
        }
        else{
            winDist.put(win, 1);
        }

    } 


}
