public class standardStats {

    double totalWinnings;
    double rtp;
    double totalStaked;
    long spins;
    double stake;

    public standardStats(long spins, double stake, double totalWinnings ) {
        this.spins = spins;
        this.stake = stake;
        this.totalWinnings = totalWinnings;
    }

    public void addWin(double win) {
        this.totalWinnings = totalWinnings + win;
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
    }
        
    }

