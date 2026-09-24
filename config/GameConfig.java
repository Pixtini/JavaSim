package config;

public class GameConfig {

    public double[] winSize = {5.0 , 10.0, 25.0};
    // Cumulative roll thresholds: 90% no win, then 5% small, 4% medium, 1% big.
    public double[] winChanceThresholds = {0.90, 0.95, 0.99};

}
