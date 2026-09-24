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
        this.totalStaked = spins * stake;
        this.rtp = totalWinnings / totalStaked;

    } 

    public void printStats() {
        System.out.println("Spins: " + spins);
        System.out.println("Total staked: £" + totalStaked);
        System.out.println("Total winnings: £" + totalWinnings);

        System.out.println();
        System.out.printf("Simulated RTP: %.4f%%%n", rtp * 100);

        System.out.println();


        System.out.println("Awards: " + java.util.Arrays.toString(paytable));
    }
        
    }

