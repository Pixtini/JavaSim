import java.util.Map;
import java.util.TreeMap;
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
        double totalWinnings = statB.getTotalWinnings() + statF.getTotalWinnings();
        long totalSpins = statB.getRounds() + statF.getRounds();

        System.out.println("Rounds: " + rounds);
        System.out.println("Spins: " + totalSpins);
        System.out.println("FG Triggers: " + statF.getFreegameTriggers());
        System.out.println("Total staked: £" + (rounds * stake));
        System.out.println("Total winnings: £" + totalWinnings);
        System.out.printf("Simulated RTP: %.4f%%%n", (totalWinnings / (rounds * stake)) * 100);
        System.out.printf("Standard Deviation: %.2f%n", statF.getStandardDeviation());
    }

    public void printStats(standardStats stats, String type) {
        System.out.println("\n" + type);
        System.out.println("--------");

        System.out.println("Rounds: " + stats.getRounds());
        System.out.println("Total winnings: £" + stats.getTotalWinnings());
        System.out.printf("Simulated RTP: %.4f%%%n", stats.getRtp() * 100);
        System.out.println("Awards: "
                + java.util.Arrays.toString(stats.getPaytable()));
        System.out.println("Hits: " + stats.getHits());
    }

    public void printWinDist(String title, Map<Double, Long> winDist) {
        System.out.println("\n" + title);
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

        printWinDist("Basegame Win Dist", statB.getWinDist());
        printWinDist("Freegame Win Dist", statF.getWinDist());
        printWinDist("Total Game Win Dist", statT.getWinDist());
    }
}
