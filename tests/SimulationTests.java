import engine.SimulationRunner;
import config.GameConfig;
import config.SimConfig;
import game.Game;
import game.GameSession;
import game.proxy.BasicProxyGame;
import java.util.List;
import java.util.Random;
import model.GameRoundResult;
import model.SpinResult;
import reporting.Print;
import result.SimulationResult;
import stats.StandardStats;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;

public final class SimulationTests {
    private static int assertions;

    public static void main(String[] args) throws Exception {
        testStatisticsAndMerge();
        testWinBandBoundaries();
        testReproducibilityAcrossThreadCounts();
        testEngineWithIndependentGameImplementation();
        testFreshSeedAndConsoleOutput();
        testReportFilesAndMergedCounts();
        System.out.println("Passed " + assertions + " assertions.");
    }

    private static void testStatisticsAndMerge() {
        StandardStats combined = new StandardStats(2.0, new int[4]);
        StandardStats other = new StandardStats(2.0, new int[4]);

        combined.addResult(new SpinResult(0.0, 0, false));
        combined.recordWinInDistribution(0.0);
        combined.recordFreegameTrigger();

        other.addResult(new SpinResult(2.0, 2, false));
        other.recordWinInDistribution(2.0);
        other.addResult(new SpinResult(5.0, 3, false));
        other.recordWinInDistribution(5.0);
        other.recordFreegameTrigger();

        combined.mergeFrom(other);

        check(combined.getRounds() == 3, "merged round count");
        check(combined.getTotalWinnings() == 7.0, "merged winnings");
        checkClose(combined.getRtp(), 7.0 / 6.0, "merged RTP");
        check(combined.getHits() == 2, "merged hit count");
        check(Arrays.equals(combined.getPaytable(), new int[] {1, 0, 1, 1}), "merged paytable");
        check(combined.getFreegameTriggers() == 2, "merged freegame trigger count");
        check(combined.getWinDist().equals(Map.of(0.0, 1L, 2.0, 1L, 5.0, 1L)), "merged distribution");
        checkClose(combined.getStandardDeviation(), Math.sqrt(38.0 / 9.0), "population standard deviation");
    }

    private static void testWinBandBoundaries() {
        StandardStats stats = new StandardStats(1.0, new int[4]);
        for (double win : new double[] {0.0, 1.0, 2.0, 3.0, 5.0, 10_000_000.0}) {
            stats.recordWinInDistribution(win);
        }

        var bands = stats.getAggregatedWinDistribution(6.0);
        check(bands.size() == 19, "expected number of win bands");
        for (int index : new int[] {0, 1, 2, 3, 4, 18}) {
            check(bands.get(index).getHits() == 1, "exact value belongs in band " + index);
        }
        check(bands.get(1).getUpperBound() == 1.0, "first positive band upper boundary");
        checkClose(bands.get(1).getPercentOfHits(), 100.0 / 6.0, "band hit percentage");
        checkClose(bands.get(1).getFrequency(), 6.0, "band frequency");
        checkClose(bands.get(1).getRtp(), 100.0 / 6.0, "band RTP");
    }

    private static void testReproducibilityAcrossThreadCounts() {
        SimConfig singleThreadConfig = simulationConfig(1, 24, 2_000, 8675309L);
        SimConfig multiThreadConfig = simulationConfig(4, 24, 2_000, 8675309L);

        SimulationResult singleThreadResult = new SimulationRunner(
                singleThreadConfig, new BasicProxyGame(new GameConfig())).run();
        SimulationResult multiThreadResult = new SimulationRunner(
                multiThreadConfig, new BasicProxyGame(new GameConfig())).run();

        assertSameStats(singleThreadResult.getBaseGameStats(), multiThreadResult.getBaseGameStats(), "basegame");
        assertSameStats(singleThreadResult.getFreeGameStats(), multiThreadResult.getFreeGameStats(), "freegame");
        assertSameStats(singleThreadResult.getTotalGameStats(), multiThreadResult.getTotalGameStats(), "total game");
    }

    private static void testEngineWithIndependentGameImplementation() {
        Game fixedOutcomeGame = new Game() {
            @Override
            public GameSession createSession(Random random) {
                return () -> new GameRoundResult(
                        new SpinResult(1.0, 1, true),
                        List.of(new SpinResult(2.0, 1, false)));
            }

            @Override
            public int[] getPaytable() {
                return new int[] {0, 0};
            }
        };

        SimConfig config = simulationConfig(1, 1, 2, 456L);
        SimulationResult result = new SimulationRunner(config, fixedOutcomeGame).run();

        check(result.getBaseGameStats().getTotalWinnings() == 2.0,
                "engine records base results from a non-proxy game");
        check(result.getFreeGameStats().getFreegameTriggers() == 2,
                "engine records triggered freegames from a non-proxy game");
        check(result.getFreeGameStats().getRounds() == 2,
                "engine records each returned freegame spin");
        check(result.getTotalGameStats().getWinDist().equals(Map.of(3.0, 2L)),
                "engine combines base and freegame wins per round");
    }

    private static void testFreshSeedAndConsoleOutput() throws Exception {
        SimConfig config = simulationConfig(2, 8, 256, Long.MIN_VALUE);
        config.usePreviousSeed = false;
        config.exportReport = false;
        SimulationResult result = new SimulationRunner(
                config, new BasicProxyGame(new GameConfig())).run();
        Path reportRoot = Files.createTempDirectory("sim-no-report-test-");

        try {
            check(config.seed != Long.MIN_VALUE, "fresh-seed mode replaces the configured seed");
            String console = captureConsole(() -> new Print(result, reportRoot).printToConsole(config));
            check(console.contains("Seed: " + config.seed), "console prints the effective seed");
            check(console.contains("Time Taken: "), "console prints elapsed simulation time");
            check(console.indexOf("Seed: ") < console.indexOf("Time Taken: "), "time follows the seed");
            check(!console.contains("Win Dist"), "console omits win distributions");
            check(!console.contains("Range Min"), "console omits aggregated tables");
            try (var children = Files.list(reportRoot)) {
                check(children.findAny().isEmpty(), "disabled report saving creates no files");
            }
        } finally {
            deleteTree(reportRoot);
        }
    }

    private static void testReportFilesAndMergedCounts() throws Exception {
        SimConfig config = simulationConfig(3, 12, 600, 123456L);
        config.exportReport = true;
        SimulationResult result = new SimulationRunner(
                config, new BasicProxyGame(new GameConfig())).run();
        Path reportRoot = Files.createTempDirectory("sim-report-tests-");

        try {
            String console = captureConsole(() -> new Print(result, reportRoot).printToConsole(config));
            check(console.contains("Detailed report written to:"), "console reports the output folder");
            check(console.contains("Seed: " + config.seed), "console reports the configured seed");
            check(console.indexOf("Seed: ") < console.indexOf("Time Taken: "), "console timing follows seed");
            check(!console.contains("Win Dist"), "report mode keeps distributions off console");

            Path reportFolder;
            try (var children = Files.list(reportRoot)) {
                reportFolder = children.findFirst().orElseThrow();
            }
            Path statsFile = reportFolder.resolve("simulation_stats.txt");
            Path distributionsFile = reportFolder.resolve("win_distributions.csv");
            Path aggregatedFile = reportFolder.resolve("win_distribution_aggregated.csv");
            String statsText = Files.readString(statsFile);
            String distributionsCsv = Files.readString(distributionsFile);
            String aggregatedCsv = Files.readString(aggregatedFile);

            check(statsText.contains("Seed: " + config.seed), "stats file reports the seed");
            check(statsText.contains("Time Taken: "), "stats file reports elapsed time");
            check(statsText.indexOf("Seed: ") < statsText.indexOf("Time Taken: "), "file timing follows seed");
            checkSectionOrder(distributionsCsv, "raw distribution CSV");
            checkSectionOrder(aggregatedCsv, "aggregated distribution CSV");
            check(sumCounts(result.getTotalGameStats().getWinDist()) == config.rounds,
                    "total-game distribution includes every base round");
            check(sumCounts(result.getBaseGameStats().getWinDist()) == config.rounds,
                    "basegame distribution includes every base round");
            check(sumCounts(result.getFreeGameStats().getWinDist())
                    == result.getFreeGameStats().getFreegameTriggers(),
                    "freegame distribution includes one entry per trigger");
            check(distributionsCsv.contains("Total Game,"), "raw CSV contains merged total-game data");
            check(aggregatedCsv.contains("Freegame,"), "aggregated CSV contains freegame data");
        } finally {
            deleteTree(reportRoot);
        }
    }

    private static SimConfig simulationConfig(int threads, int partitions, long rounds, long seed) {
        SimConfig config = new SimConfig();
        config.threads = threads;
        config.partitions = partitions;
        config.rounds = rounds;
        config.seed = seed;
        config.usePreviousSeed = true;
        config.exportReport = false;
        return config;
    }

    private static void assertSameStats(StandardStats expected, StandardStats actual, String label) {
        check(expected.getRounds() == actual.getRounds(), label + " round count is reproducible");
        check(expected.getTotalWinnings() == actual.getTotalWinnings(), label + " winnings are reproducible");
        check(sameDouble(expected.getRtp(), actual.getRtp()), label + " RTP is reproducible");
        check(expected.getHits() == actual.getHits(), label + " hits are reproducible");
        check(Arrays.equals(expected.getPaytable(), actual.getPaytable()), label + " paytable is reproducible");
        check(expected.getWinDist().equals(actual.getWinDist()), label + " distribution is reproducible");
        check(sameDouble(expected.getStandardDeviation(), actual.getStandardDeviation()),
                label + " deviation is reproducible");
    }

    private static boolean sameDouble(double left, double right) {
        return (Double.isNaN(left) && Double.isNaN(right))
                || Math.abs(left - right) < 1.0e-12;
    }

    private static long sumCounts(Map<Double, Long> distribution) {
        return distribution.values().stream().mapToLong(Long::longValue).sum();
    }

    private static void checkSectionOrder(String csv, String description) {
        int totalIndex = csv.indexOf("Total Game,");
        int baseIndex = csv.indexOf("\n\nBasegame,");
        int freeIndex = csv.indexOf("\n\nFreegame,");
        check(totalIndex >= 0 && totalIndex < baseIndex && baseIndex < freeIndex,
                description + " section order and blank lines");
    }

    private static String captureConsole(ThrowingRunnable action) throws Exception {
        PrintStream previousOutput = System.out;
        ByteArrayOutputStream capturedOutput = new ByteArrayOutputStream();
        try (PrintStream capture = new PrintStream(capturedOutput, true, StandardCharsets.UTF_8)) {
            System.setOut(capture);
            action.run();
        } finally {
            System.setOut(previousOutput);
        }
        return capturedOutput.toString(StandardCharsets.UTF_8);
    }

    private static void deleteTree(Path root) throws Exception {
        try (var paths = Files.walk(root)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.delete(path);
            }
        }
    }

    private static void checkClose(double actual, double expected, String message) {
        check(Math.abs(actual - expected) < 1.0e-9, message + ": expected " + expected + ", got " + actual);
    }

    private static void check(boolean condition, String message) {
        assertions++;
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }
}
