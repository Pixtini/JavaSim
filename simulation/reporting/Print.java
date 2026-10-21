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
import toolkit.par.ParWorkbookHtmlReport;
import toolkit.par.ParWorkbookModes;
import toolkit.par.ParWorkbookSheetExporter;

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

    private void printSimulationStats(PrintWriter output, long rounds, double stake,
            double costMultiplier) {
        double totalWinnings = baseGameStats.getTotalWinnings() + freeGameStats.getTotalWinnings();
        long totalSpins = baseGameStats.getRounds() + freeGameStats.getRounds();

        output.println("TotalGame");
        output.println("--------");
        output.println("Rounds: " + rounds);
        output.println("Spins: " + totalSpins);
        double totalStake = rounds * stake * costMultiplier;
        output.println("Total staked: £" + String.format(Locale.ROOT, "%.1f", totalStake));
        output.println("Total winnings: £" + formatWin(totalWinnings, stake));
        output.println();
        output.printf(Locale.ROOT, "Simulated RTP: %.4f%%%n",
                totalWinnings / totalStake * 100);
        output.printf(Locale.ROOT, "Basegame RTP: %.4f%%%n", baseGameStats.getRtp() * 100);
        output.printf(Locale.ROOT, "Freegame RTP: %.4f%%%n", freeGameStats.getRtp() * 100);
        output.println();
        output.println("Hit Rate (1 in): " + frequency(rounds, baseGameStats.getHits()));
        output.println();
        long freegameTriggers = freeGameStats.getFreegameTriggers();
        output.println("FG Triggers: " + freegameTriggers);
        output.println("Freegame Freq (1 in): " + frequency(rounds, freegameTriggers));
        output.println("Freegame Avg Win: " + (freegameTriggers == 0 ? "N/A"
                : String.format(Locale.ROOT, "£%.2f",
                        freeGameStats.getTotalWinnings() / freegameTriggers)));
        output.println();
        output.println("Max Win: " + formatMaxWin(simulationResult.getMaxWinMultiplier(), stake));
        output.println("Wincaps: " + totalGameStats.getWinCaps());
        output.println("Max Win Freq (1 in): " + frequency(rounds, totalGameStats.getWinCaps()));
        output.println("Standard Deviation: " + formatDeviation(totalGameStats.getStandardDeviation()));
        output.println("Base SD: " + formatDeviation(baseGameStats.getStandardDeviation()));
        output.println("Free SD: " + formatDeviation(freeGameStats.getStandardDeviation()));
        output.println();
    }

    private void printStats(PrintWriter output, StandardStats stats, String type,
            boolean showAwards, double stake) {
        output.println(type);
        output.println("--------");
        output.println("Rounds: " + stats.getRounds());
        output.println("Total winnings: £" + formatWin(stats.getTotalWinnings(), stake));
        output.printf(Locale.ROOT, "Simulated RTP: %.4f%%%n", stats.getRtp() * 100);
        output.println("Hits: " + stats.getHits());
        output.println("Standard Deviation: " + formatDeviation(stats.getStandardDeviation()));
        if (showAwards) {
            output.println("Awards: " + (stats.getAwardCounts().isEmpty()
                    ? java.util.Arrays.toString(stats.getPaytable())
                    : stats.getAwardCounts()));
        }
        output.println();
    }

    private static String frequency(long rounds, long occurrences) {
        if (occurrences == 0) return "N/A";
        return String.format(Locale.ROOT, "%.2f", (double) rounds / occurrences);
    }

    private static String formatDeviation(double deviation) {
        return Double.isFinite(deviation)
                ? String.format(Locale.ROOT, "%.2f", deviation) : "N/A";
    }

    private static String formatMaxWin(double multiplier, double stake) {
        if (!Double.isFinite(multiplier)) return "Unlimited";
        return String.format(Locale.ROOT, "%.2f× round stake (£%s)",
                multiplier, formatWin(multiplier * stake, stake));
    }

    private void printRegularStats(PrintWriter output, SimConfig simConfig) {
        double totalStake = simConfig.rounds * simConfig.stake * simConfig.modeCostMultiplier;
        printSimulationStats(output, simConfig.rounds, simConfig.stake,
                simConfig.modeCostMultiplier);
        printStats(output, baseGameStats, "Basegame", simConfig.showAwards, simConfig.stake);
        printSetBreakdown(output, simulationResult.getBaseGameSetStats(),
                "Basegame Set ", false, totalStake, simConfig.stake);
        printStats(output, freeGameStats, "Freegame", simConfig.showAwards, simConfig.stake);
        printSetBreakdown(output, simulationResult.getFreeGameSetStats(),
                "Freegame Set ", true, totalStake, simConfig.stake);
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
                    output.println("Simulated RTP: " + (Double.isFinite(rtp)
                            ? String.format(Locale.ROOT, "%.4f%%", rtp * 100) : "N/A"));
                    output.println("Hits: " + stats.getHits());
                    output.println();
                });
    }

    private void printRunSettings(PrintWriter output, SimConfig simConfig) {
        output.println("Game: " + simConfig.gameId);
        output.println("Mode: " + simConfig.gameMode);
        output.println("Mode cost multiplier: " + simConfig.modeCostMultiplier + "× base stake");
        if (simConfig.parWorkbookPath != null) {
            output.println("PAR workbook: " + simConfig.parWorkbookPath.toAbsolutePath().normalize());
        }
        if (simConfig.bookMetadata != null) {
            output.println("Book CSV: " + simConfig.bookMetadata.gameplayCsv());
        }
        output.println("Seed: " + simConfig.seed);
        output.printf(Locale.ROOT, "Time Taken: %.3f seconds%n",
                simulationResult.getElapsedNanos() / 1_000_000_000.0);
    }

    private void writeAggregatedWinBand(PrintWriter output, StandardStats.WinBand band) {
        output.printf(Locale.ROOT,
                "%.2f,%s,%d,%.8f%%,%.2f,%.2f%%%n",
                band.getLowerBound(),
                formatBandUpperBound(band.getUpperBound()),
                band.getHits(),
                band.getPercentOfHits(),
                band.getFrequency(),
                band.getRtp());
    }

    private static String formatBandUpperBound(double upperBound) {
        return Double.isInfinite(upperBound)
                ? "∞" : String.format(Locale.ROOT, "%.2f", upperBound);
    }

    private Path createReportFiles(SimConfig simConfig) throws IOException {
        if (simConfig.useBooks) {
            if (simConfig.bookMetadata == null) throw new IOException("Missing validated book metadata");
            simConfig.bookMetadata.verifyInputsUnchanged();
        }
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

        if (simConfig.parWorkbookPath != null) {
            Path selectedWorkbook = simConfig.parWorkbookPath.toAbsolutePath().normalize();
            if (!Files.isRegularFile(selectedWorkbook)) {
                throw new IOException("Selected PAR workbook does not exist: " + selectedWorkbook);
            }
            String workbookName = selectedWorkbook.getFileName().toString();
            Path reportWorkbook = reportFolder.resolve(workbookName);
            var modeSheets = ParWorkbookModes.select(selectedWorkbook, simConfig.gameMode);
            var includedSheets = new java.util.LinkedHashSet<>(modeSheets.sheetNames());
            if (simConfig.useBooks) includedSheets.add(simConfig.bookMetadata.sheetName());
            ParWorkbookSheetExporter.export(selectedWorkbook, reportWorkbook, includedSheets);
            if (simConfig.useBooks) {
                Path bookFolder = Files.createDirectory(reportFolder.resolve("books"));
                String filename = "saved_gameplays_" + simConfig.bookMetadata.mode().toLowerCase(Locale.ROOT) + ".csv";
                Files.copy(simConfig.bookMetadata.gameplayCsv(), bookFolder.resolve(filename));
            }
            ParWorkbookHtmlReport.write(reportWorkbook, simConfig.gameMode,
                    reportFolder.resolve("config.html"));
        }

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
            output.println("Distribution,Range Min (>=),Range Max (<),Hits,Hit Rate,HR Freq,RTP");
            double totalStaked = simConfig.rounds * simConfig.stake * simConfig.modeCostMultiplier;
            writeAggregatedWinDistributionRows(output, "Total Game", totalGameStats, totalStaked);
            output.println();
            writeAggregatedWinDistributionRows(output, "Basegame", baseGameStats, totalStaked);
            output.println();
            writeAggregatedWinDistributionRows(output, "Freegame", freeGameStats, totalStaked);
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
            StandardStats stats, double totalStaked) {
        List<StandardStats.WinBand> bands = stats.getAggregatedWinDistribution(totalStaked);
        for (StandardStats.WinBand band : bands) {
            output.print(distribution + ",");
            writeAggregatedWinBand(output, band);
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
