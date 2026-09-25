import java.util.HashMap;
import java.util.Map;
import config.SimConfig;

public class standardStats {

    double totalWinnings;
    double rtp;
    double totalStaked;
    long spins;
    double stake;
    double freespins;
    int[] paytable;
    Map<Double, Integer> winDist = new HashMap<>();
    

    public standardStats(long spins, double stake, double totalWinnings, int[] paytable ) {
        this.spins = spins;
        this.stake = stake;
        this.totalWinnings = totalWinnings;
        this.paytable = paytable;
        this.freespins = 0.0;
    }

    public void addWin(double win) {
        this.totalWinnings = totalWinnings + win;
    }

    public void addPaytable(int winSizeCount) {
        this.paytable[winSizeCount]++; 
    }

    public void calculateStats() {
        SimConfig simConfig = new SimConfig();
        this.spins = simConfig.spins;
        this.totalStaked = spins * stake;
        this.rtp = totalWinnings / totalStaked;

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
