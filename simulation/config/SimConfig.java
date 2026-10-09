package simulation.config;

import java.nio.file.Path;

public class SimConfig {

    public String gameId = "expanding-wild";
    /** Selected game PAR workbook; required by the simulation UI before starting a run. */
    public Path parWorkbookPath;
    public long rounds = 1_000_000;
    public double stake = 1.0;
    public long seed = 5051410756155515478L;
    public boolean usePreviousSeed = false;
    public boolean exportReport = true;
    public boolean showAwards = false;
    /** Maximum number of replayable rounds retained in a report; zero disables capture. */
    public int maxSavedGameplays = 1_000;
    public int threads = Runtime.getRuntime().availableProcessors();
    public int partitions = 256;

}
