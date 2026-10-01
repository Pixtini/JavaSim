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

public class print {
    private static final DateTimeFormatter REPORT_FOLDER_TIME =
            DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    private final standardStats statB;
    private final standardStats statF;
    private final standardStats statT;

    public print(standardStats statB, standardStats statF, standardStats statT) {
        this.statB = statB;
        this.statF = statF;
        this.statT = statT;
    }

    private void printSimulationStats(PrintWriter output, long rounds, double stake) {
        double totalWinnings = statB.getTotalWinnings() + statF.getTotalWinnings();
        long totalSpins = statB.getRounds() + statF.getRounds();

        output.println("TotalGame");
        output.println("--------");
        output.println("Rounds: " + rounds);
        output.println("Spins: " + totalSpins);
        output.println("FG Triggers: " + statF.getFreegameTriggers());
        output.println("Total staked: £" + (rounds * stake));
        output.println("Total winnings: £" + totalWinnings);
        output.printf(Locale.ROOT, "Simulated RTP: %.4f%%%n", (totalWinnings / (rounds * stake)) * 100);
        output.printf(Locale.ROOT, "Standard Deviation: %.2f%n", statF.getStandardDeviation());
        output.println();
    }

    private void printStats(PrintWriter output, standardStats stats, String type) {
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
        printStats(output, statB, "Basegame");
        printStats(output, statF, "Freegame");
        output.flush();
    }

    private void printRunSettings(PrintWriter output, SimConfig simConfig) {
        output.println("Logical partitions: " + simConfig.partitions);
        output.println("Seed: " + simConfig.seed);
    }

    private void writeAggregatedWinBand(PrintWriter output, int index,
            standardStats.WinBand band) {
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
        Path reportsRoot = Path.of("reports");
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
            writeWinDistributionRows(output, "Total Game", statT.getWinDist());
            output.println();
            writeWinDistributionRows(output, "Basegame", statB.getWinDist());
            output.println();
            writeWinDistributionRows(output, "Freegame", statF.getWinDist());
        }

        try (PrintWriter output = new PrintWriter(Files.newBufferedWriter(
                reportFolder.resolve("win_distribution_aggregated.csv"), StandardCharsets.UTF_8))) {
            output.println("Distribution,Range Min,Range Max,Hits,% Of Winnings,% Of Hits,Frequency,RTP,Total Win");
            double totalStaked = simConfig.rounds * simConfig.stake;
            writeAggregatedWinDistributionRows(output, "Total Game", statT, totalStaked);
            output.println();
            writeAggregatedWinDistributionRows(output, "Basegame", statB, totalStaked);
            output.println();
            writeAggregatedWinDistributionRows(output, "Freegame", statF, totalStaked);
        }

        return reportFolder;
    }

    private void writeWinDistributionRows(PrintWriter output, String distribution,
            Map<Double, Long> winDist) {
        new TreeMap<>(winDist).forEach((win, count) ->
                output.printf(Locale.ROOT, "%s,%.2f,%d%n", distribution, win, count));
    }

    private void writeAggregatedWinDistributionRows(PrintWriter output, String distribution,
            standardStats stats, double totalStaked) {
        List<standardStats.WinBand> bands = stats.getAggregatedWinDistribution(totalStaked);
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
