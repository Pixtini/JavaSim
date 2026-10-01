package config;

public class SimConfig {

    public long rounds = 1_000;
    public double stake = 1.0;
    public long seed = 12345L;
    public boolean usePreviousSeed = true;
    public boolean exportReport = true;
    public int threads = Runtime.getRuntime().availableProcessors();
    public int partitions = 256;

}
