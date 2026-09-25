public class print {
    private final standardStats statB;
    private final standardStats statF;

    public print(standardStats statB, standardStats statF) {
        this.statB = statB;
        this.statF = statF;
    }

    public void printSimulationStats(long spins, double stake) {
        double totalWinnings = statB.totalWinnings + statF.totalWinnings;

        System.out.println("Spins: " + spins);
        System.out.println("Total staked: £" + (spins * stake));
        System.out.println("Total winnings: £" + totalWinnings);
    }

    public void printStats(standardStats stats) {
        stats.calculateStats();

        System.out.println("Spins: " + stats.spins);
        System.out.println("Total winnings: £" + stats.totalWinnings);
        System.out.printf("Simulated RTP: %.4f%%%n", stats.rtp * 100);
        System.out.println("Awards: "
                + java.util.Arrays.toString(stats.paytable));
    }

    public void printToConsole(long spins, double stake) {
        System.out.println("TotalGame");
        System.out.println("--------");
        printSimulationStats(spins, stake);

        System.out.println("\nBasegame");
        System.out.println("--------");
        printStats(statB);

        System.out.println("\nFreegame");
        System.out.println("--------");
        printStats(statF);
    }
}
