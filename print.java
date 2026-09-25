public class print {
    private final standardStats statB;
    private final standardStats statF;

    public print(standardStats statB, standardStats statF) {
        this.statB = statB;
        this.statF = statF;
    }

    public static void printToConsole(standardStats statB, standardStats statF) {

        System.out.println("TotalGame");
        System.out.println("--------");

        statB.printSimulationStats( statB.totalWinnings, statF.totalWinnings);

        System.out.println("");
        System.out.println("Basegame");
        System.out.println("--------");
        
        statB.calculateStats();
        statB.printStats();
        
        System.out.println("");
        System.out.println("Freegame");
        System.out.println("--------");
        
        statF.calculateStats();
        statF.printStats();
    }
}