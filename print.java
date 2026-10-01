import java.util.Map;
import java.util.TreeMap;
import config.GameConfig;
import config.SimConfig;

public class print {
    private final standardStats statB;
    private final standardStats statF;
    private final standardStats statT;

    public print(standardStats statB, standardStats statF, standardStats statT) {
        this.statB = statB;
        this.statF = statF;
        this.statT = statT;
    }

    public void printSimulationStats(long rounds, double stake) {
        double totalWinnings = statB.totalWinnings + statF.totalWinnings;
        long totalSpins = statB.rounds + statF.rounds;
        statF.calculateStats();

        System.out.println("Rounds: " + rounds);
        System.out.println("Spins: " + totalSpins);
        System.out.println("FG Triggers: " + statF.freegameTriggers);
        System.out.println("Total staked: £" + (rounds * stake));
        System.out.println("Total winnings: £" + totalWinnings);
        System.out.printf("Simulated RTP: %.4f%%%n", (totalWinnings / (rounds * stake)) * 100);
        System.out.printf("Standard Deviation: %.2f%n", statF.standardDeviation);
    }

    public void printStats(standardStats stats, String type) {
        stats.calculateStats();
        System.out.println("\n" + type);
        System.out.println("--------");

        GameConfig gameConfig = new GameConfig();

        System.out.println("Rounds: " + stats.rounds);
        System.out.println("Total winnings: £" + stats.totalWinnings);
        System.out.printf("Simulated RTP: %.4f%%%n", stats.rtp * 100);
        System.out.println("Awards: "
                + java.util.Arrays.toString(stats.paytable));
        System.out.println("Hits: " + stats.hits);
    }

    public void printWinDist(Map<Double, Integer> winDist) {
        System.out.println("\nWin Dist");
        System.out.println("--------");
        new TreeMap<>(winDist).forEach((win, count) ->
                System.out.println(win + " -> " + count));
    }

    public void printToConsole() {
        SimConfig simConfig = new SimConfig();

        System.out.println("TotalGame");
        System.out.println("--------");
        printSimulationStats(simConfig.rounds, simConfig.stake);

        printStats(statB, "Basegame");
        printStats(statF, "Freegame");

        printWinDist(statT.winDist);
    }
}
