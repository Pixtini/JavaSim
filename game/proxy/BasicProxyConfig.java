package game.proxy;

public class BasicProxyConfig {

    public double[] winSize = {1.0 , 2.0, 5.0};
    // Cumulative roll thresholds: 90% no win, then 5% small, 4% medium, 1% big.
    public double[] winChanceThresholds = {0.90, 0.95, 0.99};

    public int[] paytable = {0,0,0,0};

    public int freeSpinAmount = 3;

}
