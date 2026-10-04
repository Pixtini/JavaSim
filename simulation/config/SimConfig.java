package simulation.config;

public class SimConfig {

    public String gameId = "expanding-wild";
    public long rounds = 1_000_000;
    public double stake = 1.0;
    public long seed = 5051410756155515478L;
    public boolean usePreviousSeed = false;
    public boolean exportReport = true;
    public boolean showAwards = false;
    public int threads = Runtime.getRuntime().availableProcessors();
    public int partitions = 256;

}
