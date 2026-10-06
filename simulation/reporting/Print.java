package simulation.reporting;

import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.Comparator;
import simulation.config.SimConfig;
import simulation.result.SimulationResult;
import simulation.replay.SavedGameplayCsv;
import simulation.stats.StandardStats;

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
        output.println("Wincaps: " + totalGameStats.getWinCaps());
        output.println("Total staked: £" + (rounds * stake));
        output.println("Total winnings: £" + formatWin(totalWinnings, stake));
        output.printf(Locale.ROOT, "Simulated RTP: %.4f%%%n", (totalWinnings / (rounds * stake)) * 100);
        output.printf(Locale.ROOT, "Standard Deviation: %.2f%n", totalGameStats.getStandardDeviation());
        output.println();
    }

    private void printStats(PrintWriter output, StandardStats stats, String type,
            boolean showAwards, double stake) {
        output.println(type);
        output.println("--------");
        output.println("Rounds: " + stats.getRounds());
        output.println("Total winnings: £" + formatWin(stats.getTotalWinnings(), stake));
        output.printf(Locale.ROOT, "Simulated RTP: %.4f%%%n", stats.getRtp() * 100);
        if (showAwards) {
            output.println("Awards: " + (stats.getAwardCounts().isEmpty()
                    ? java.util.Arrays.toString(stats.getPaytable())
                    : stats.getAwardCounts()));
        }
        output.println("Hits: " + stats.getHits());
        output.println();
    }

    private void printRegularStats(PrintWriter output, SimConfig simConfig) {
        printSimulationStats(output, simConfig.rounds, simConfig.stake);
        printStats(output, baseGameStats, "Basegame", simConfig.showAwards, simConfig.stake);
        printSetBreakdown(output, simulationResult.getBaseGameSetStats(),
                "Basegame Set ", false, simConfig.rounds * simConfig.stake, simConfig.stake);
        printStats(output, freeGameStats, "Freegame", simConfig.showAwards, simConfig.stake);
        printSetBreakdown(output, simulationResult.getFreeGameSetStats(),
                "Freegame Set ", true, simConfig.rounds * simConfig.stake, simConfig.stake);
        output.flush();
    }

    private void printSetBreakdown(PrintWriter output, Map<Integer, StandardStats> setStats,
            String titlePrefix, boolean freeGame, double totalBaseGameStake, double stake) {
        setStats.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.naturalOrder()))
                .forEach(entry -> {
                    StandardStats stats = entry.getValue();
                    double rtp = freeGame
                            ? stats.getRtpForTotalStake(totalBaseGameStake)
                            : stats.getRtp();
                    output.println(titlePrefix + entry.getKey());
                    output.println("--------");
                    output.println("Rounds: " + stats.getRounds());
                    output.println("Total winnings: £" + formatWin(stats.getTotalWinnings(), stake));
                    output.printf(Locale.ROOT, "Simulated RTP: %.4f%%%n", rtp * 100);
                    output.println("Hits: " + stats.getHits());
                    output.println();
                });
    }

    private void printRunSettings(PrintWriter output, SimConfig simConfig) {
        output.println("Game: " + simConfig.gameId);
        output.println("Logical partitions: " + simConfig.partitions);
        output.println("Seed: " + simConfig.seed);
        output.printf(Locale.ROOT, "Time Taken: %.3f seconds%n",
                simulationResult.getElapsedNanos() / 1_000_000_000.0);
    }

    private void writeAggregatedWinBand(PrintWriter output, int index,
            StandardStats.WinBand band, double stake) {
        String rangeMin = index == 0
                ? String.format(Locale.ROOT, "%.2f", band.getLowerBound())
                : String.format(Locale.ROOT, "> %.2f", band.getLowerBound());

        output.printf(Locale.ROOT,
                "%s,%.2f,%d,%.8f%%,%.8f%%,%.2f,%.2f%%,%s%n",
                rangeMin,
                band.getUpperBound(),
                band.getHits(),
                band.getPercentOfWinnings(),
                band.getPercentOfHits(),
                band.getFrequency(),
                band.getRtp(),
                formatWin(band.getTotalWin(), stake));
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
            writeWinDistributionRows(output, "Total Game", totalGameStats.getWinDist(), simConfig.stake);
            output.println();
            writeWinDistributionRows(output, "Basegame", baseGameStats.getWinDist(), simConfig.stake);
            output.println();
            writeWinDistributionRows(output, "Freegame", freeGameStats.getWinDist(), simConfig.stake);
        }

        try (PrintWriter output = new PrintWriter(Files.newBufferedWriter(
                reportFolder.resolve("win_distribution_aggregated.csv"), StandardCharsets.UTF_8))) {
            output.println("Distribution,Range Min,Range Max,Hits,% Of Winnings,% Of Hits,Frequency,RTP,Total Win");
            double totalStaked = simConfig.rounds * simConfig.stake;
            writeAggregatedWinDistributionRows(output, "Total Game", totalGameStats, totalStaked,
                    simConfig.stake);
            output.println();
            writeAggregatedWinDistributionRows(output, "Basegame", baseGameStats, totalStaked,
                    simConfig.stake);
            output.println();
            writeAggregatedWinDistributionRows(output, "Freegame", freeGameStats, totalStaked,
                    simConfig.stake);
        }

        if (!simulationResult.getSavedGameplays().isEmpty()) {
            SavedGameplayCsv.write(reportFolder.resolve("saved_gameplays.csv"),
                    simulationResult.getSavedGameplays());
        }

        return reportFolder;
    }

    private void writeWinDistributionRows(PrintWriter output, String distribution,
            Map<Double, Long> winDist, double stake) {
        Map<Double, Long> displayDistribution = new TreeMap<>();
        winDist.forEach((win, count) -> displayDistribution.merge(
                roundedWin(win, stake), count, Long::sum));
        displayDistribution.forEach((win, count) ->
                output.printf(Locale.ROOT, "%s,%s,%d%n", distribution,
                        formatWin(win, stake), count));
    }

    private void writeAggregatedWinDistributionRows(PrintWriter output, String distribution,
            StandardStats stats, double totalStaked, double stake) {
        List<StandardStats.WinBand> bands = stats.getAggregatedWinDistribution(totalStaked);
        for (int i = 0; i < bands.size(); i++) {
            output.print(distribution + ",");
            writeAggregatedWinBand(output, i, bands.get(i), stake);
        }
    }

    private static String formatWin(double amount, double stake) {
        BigDecimal rounded = BigDecimal.valueOf(roundedWin(amount, stake));
        int decimalPlaces = Math.max(1,
                BigDecimal.valueOf(stake).stripTrailingZeros().scale() + 1);
        return rounded.setScale(decimalPlaces, RoundingMode.HALF_UP).toPlainString();
    }

    private static double roundedWin(double amount, double stake) {
        BigDecimal stakeValue = BigDecimal.valueOf(stake);
        return BigDecimal.valueOf(amount).divide(stakeValue, 1, RoundingMode.HALF_UP)
                .multiply(stakeValue).doubleValue();
    }

    public void printToConsole(SimConfig simConfig) {
        printToConsole(simConfig, new PrintWriter(System.out, true));
    }

    /** Writes the regular summary and optional report path to the supplied destination. */
    public void printToConsole(SimConfig simConfig, PrintWriter console) {
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
