import config.SimConfig;

public class MonteCarloExample {

    public static void main(String[] args) {

        SimConfig simConfig = new SimConfig();

        standardStats stat = new standardStats(simConfig.spins, simConfig.stake, 0.0, new int[]{0,0,0,0});

        for (long i = 0; i < simConfig.spins; i++) {

            spinResult result = gameBasic.spin();

            stat.addWin(result.win);
            stat.addPaytable(result.winSize);
        }

        stat.calculateStats();
        stat.printStats();
        
    }
}