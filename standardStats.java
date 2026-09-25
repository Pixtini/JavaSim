import config.SimConfig;

public class standardStats {

    double totalWinnings;
    double rtp;
    double totalStaked;
    long spins;
    double stake;
    int[] paytable;

    public standardStats(long spins, double stake, double totalWinnings, int[] paytable ) {
        this.spins = spins;
        this.stake = stake;
        this.totalWinnings = totalWinnings;
        this.paytable = paytable;
    }

    public void addWin(double win) {
        this.totalWinnings = totalWinnings + win;
    }

    public void addPaytable(int winSizeCount) {
        this.paytable[winSizeCount]++; 
    }

    public void calculateStats() {
        this.spins = java.util.Arrays.stream(paytable).sum();
        this.totalStaked = spins * stake;
        this.rtp = totalWinnings / totalStaked;

    } 

    public void printSimulationStats(double baseWinnings, double freeWinnings){
        SimConfig simConfig = new SimConfig();
        double staked = simConfig.spins * simConfig.stake;
        System.out.println("Spins: " + simConfig.spins);
        System.out.println("Total staked: £" + staked);
        System.out.println("Total winnings: £" + (baseWinnings+ freeWinnings));
    }

    public void printStats() {

        System.out.println("Spins: " + spins);
        System.out.println("Total winnings: £" + totalWinnings);

        System.out.println();
        System.out.printf("Simulated RTP: %.4f%%%n", rtp * 100);

        System.out.println();
        System.out.println("Awards: " + java.util.Arrays.toString(paytable));
    }

        
    }

