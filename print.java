import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import config.SimConfig;

public class Print {
    private static final DateTimeFormatter REPORT_FOLDER_TIME =
            DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    private final StandardStats baseGameStats;
    private final StandardStats freeGameStats;
    private final StandardStats totalGameStats;
    private final SimulationResult simulationResult;
    private final Path reportsRoot;

    public Print(SimulationResult simulationResult) {
        this(simulationResult, Path.of("reports"));
    }

    public Print(SimulationResult simulationResult, Path reportsRoot) {
        this.simulationResult = simulationResult;
        this.baseGameStats = simulationResult.getBaseGameStats();
        this.freeGameStats = simulationResult.getFreeGameStats();
        this.totalGameStats = simulationResult.getTotalGameStats();
        this.reportsRoot = reportsRoot;
    }

    private void printSimulationStats(PrintWriter output, long rounds, double stake) {
        double totalWinnings = baseGameStats.getTotalWinnings() + freeGameStats.getTotalWinnings();
        long totalSpins = baseGameStats.getRounds() + freeGameStats.getRounds();

        output.println("TotalGame");
        output.println("--------");
        output.println("Rounds: " + rounds);
        output.println("Spins: " + totalSpins);
        output.println("FG Triggers: " + freeGameStats.getFreegameTriggers());
        output.println("Total staked: £" + (rounds * stake));
        output.println("Total winnings: £" + totalWinnings);
        output.printf(Locale.ROOT, "Simulated RTP: %.4f%%%n", (totalWinnings / (rounds * stake)) * 100);
        output.printf(Locale.ROOT, "Standard Deviation: %.2f%n", freeGameStats.getStandardDeviation());
        output.println();
    }

    private void printStats(PrintWriter output, StandardStats stats, String type) {
        output.println(type);
        output.println("--------");
        output.println("Rounds: " + stats.getRounds());
        output.println("Total winnings: £" + stats.getTotalWinnings());
        output.printf(Locale.ROOT, "Simulated RTP: %.4f%%%n", stats.getRtp() * 100);
        output.println("Awards: " + java.util.Arrays.toString(stats.getPaytable()));
        output.println("Hits: " + stats.getHits());
        output.println();
    }

    private void printRegularStats(PrintWriter output, SimConfig simConfig) {
        printSimulationStats(output, simConfig.rounds, simConfig.stake);
        printStats(output, baseGameStats, "Basegame");
        printStats(output, freeGameStats, "Freegame");
        output.flush();
    }

    private void printRunSettings(PrintWriter output, SimConfig simConfig) {
        output.println("Logical partitions: " + simConfig.partitions);
        output.println("Seed: " + simConfig.seed);
        output.printf(Locale.ROOT, "Time Taken: %.3f seconds%n",
                simulationResult.getElapsedNanos() / 1_000_000_000.0);
    }

    private void writeAggregatedWinBand(PrintWriter output, int index,
            StandardStats.WinBand band) {
        String rangeMin = index == 0
                ? String.format(Locale.ROOT, "%.2f", band.getLowerBound())
                : String.format(Locale.ROOT, "> %.2f", band.getLowerBound());

        output.printf(Locale.ROOT,
                "%s,%.2f,%d,%.8f%%,%.8f%%,%.2f,%.2f%%,%.2f%n",
                rangeMin,
                band.getUpperBound(),
                band.getHits(),
                band.getPercentOfWinnings(),
                band.getPercentOfHits(),
                band.getFrequency(),
                band.getRtp(),
                band.getTotalWin());
    }

    private Path createReportFiles(SimConfig simConfig) throws IOException {
        Files.createDirectories(reportsRoot);
        String timestamp = LocalDateTime.now().format(REPORT_FOLDER_TIME);
        String folderPrefix = "simulation-" + timestamp;
        Path reportFolder = reportsRoot.resolve(folderPrefix);
        int suffix = 2;
        while (Files.exists(reportFolder)) {
            reportFolder = reportsRoot.resolve(folderPrefix + "-" + suffix);
            suffix++;
        }
        Files.createDirectory(reportFolder);

        try (PrintWriter output = new PrintWriter(Files.newBufferedWriter(
            reportFolder.resolve("simulation_stats.txt"), StandardCharsets.UTF_8))) {
            printRegularStats(output, simConfig);
            printRunSettings(output, simConfig);
        }

        try (PrintWriter output = new PrintWriter(Files.newBufferedWriter(
                reportFolder.resolve("win_distributions.csv"), StandardCharsets.UTF_8))) {
            output.println("Distribution,Win,Hits");
            writeWinDistributionRows(output, "Total Game", totalGameStats.getWinDist());
            output.println();
            writeWinDistributionRows(output, "Basegame", baseGameStats.getWinDist());
            output.println();
            writeWinDistributionRows(output, "Freegame", freeGameStats.getWinDist());
        }

        try (PrintWriter output = new PrintWriter(Files.newBufferedWriter(
                reportFolder.resolve("win_distribution_aggregated.csv"), StandardCharsets.UTF_8))) {
            output.println("Distribution,Range Min,Range Max,Hits,% Of Winnings,% Of Hits,Frequency,RTP,Total Win");
            double totalStaked = simConfig.rounds * simConfig.stake;
            writeAggregatedWinDistributionRows(output, "Total Game", totalGameStats, totalStaked);
            output.println();
            writeAggregatedWinDistributionRows(output, "Basegame", baseGameStats, totalStaked);
            output.println();
            writeAggregatedWinDistributionRows(output, "Freegame", freeGameStats, totalStaked);
        }

        return reportFolder;
    }

    private void writeWinDistributionRows(PrintWriter output, String distribution,
            Map<Double, Long> winDist) {
        new TreeMap<>(winDist).forEach((win, count) ->
                output.printf(Locale.ROOT, "%s,%.2f,%d%n", distribution, win, count));
    }

    private void writeAggregatedWinDistributionRows(PrintWriter output, String distribution,
            StandardStats stats, double totalStaked) {
        List<StandardStats.WinBand> bands = stats.getAggregatedWinDistribution(totalStaked);
        for (int i = 0; i < bands.size(); i++) {
            output.print(distribution + ",");
            writeAggregatedWinBand(output, i, bands.get(i));
        }
    }

    public void printToConsole(SimConfig simConfig) {
        PrintWriter console = new PrintWriter(System.out, true);
        printRegularStats(console, simConfig);

        if (simConfig.exportReport) {
            try {
                Path reportFolder = createReportFiles(simConfig);
                console.println("Detailed report written to: " + reportFolder);
            } catch (IOException exception) {
                throw new RuntimeException("Could not create simulation report files", exception);
            }
        }
        printRunSettings(console, simConfig);
        console.flush();
    }
}
