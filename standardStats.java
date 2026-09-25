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

}
