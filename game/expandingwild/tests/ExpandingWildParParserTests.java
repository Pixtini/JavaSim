package game.expandingwild.tests;

import game.expandingwild.ExpandingWildGame;
import game.expandingwild.config.ExpandingWildConfig;
import game.expandingwild.config.ExpandingWildParParser;
import game.GameFactory;
import simulation.config.SimConfig;
import simulation.engine.SimulationRunner;
import simulation.reporting.Print;
import toolkit.replay.ReplayerMain;
import toolkit.par.ExcelWorkbookReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

/** Verifies that the expanding wild PAR workbook populates the existing game config. */
public final class ExpandingWildParParserTests {
    private ExpandingWildParParserTests() {}

    public static void main(String[] args) throws Exception {
        Path parPath = args.length == 0
                ? Path.of("game/expandingwild/expandingWildPAR.xlsx")
                : Path.of(args[0]);
        ExpandingWildConfig config = new ExpandingWildParParser().parse(parPath);
        new ExpandingWildGame(config);
        require(config.reelCount == 5 && config.visibleRows == 5, "screen dimensions mapped");
        require(config.paylines.size() == 15, "PAR paylines mapped");
        require(config.freeGameTriggerScatterCount == 3 && config.freeGamesAwarded == 5,
                "scatter award map mapped");
        require(config.baseGame.setSelectionWeights.get(0).weight() == 3,
                "basegame set selector mapped");
        require(config.baseGame.sets.get(0).reelStrips.get(0).size() > 0,
                "reel stop list mapped");
        require(config.baseGame.sets.get(0).symbolInsertions.size() == 2,
                "basegame scatter and banner rules mapped for set zero");
        require(config.baseGame.sets.get(1).symbolInsertions.size() == 2,
                "basegame scatter and banner rules mapped for set one");
        var baseScatter = config.baseGame.sets.get(0).symbolInsertions.stream()
                .filter(rule -> rule.symbol().equals(ExpandingWildConfig.SCATTER))
                .findFirst().orElseThrow();
        require(baseScatter.positionWeights().get(0).get(0) == 1
                        && baseScatter.positionWeights().get(1).get(0) == 0
                        && baseScatter.positionWeights().get(2).get(0) == 1,
                "heat-map grid is mapped to reel-major weights");
        require(config.freeGame.sets.get(0).bannerMultiplierWeights.size() > 0,
                "freegame set zero multiplier table mapped");
        require(config.paytable.getAwards().size() == 30, "paytable mapped");
        require(config.paytable.getPayout(ExpandingWildConfig.T1, 5) == 20.0,
                "symbol ID and paytable payout mapped");
        verifySimulationAndReport(parPath);
        System.out.println("ExpandingWildParParser mapped PAR workbook " + parPath);
    }

    private static void verifySimulationAndReport(Path parPath) throws Exception {
        SimConfig simulation = new SimConfig();
        simulation.gameId = "expanding-wild";
        simulation.parWorkbookPath = parPath.toAbsolutePath().normalize();
        simulation.rounds = 2_000;
        simulation.seed = 73_981;
        simulation.usePreviousSeed = true;
        simulation.threads = 2;
        simulation.partitions = 4;
        simulation.maxSavedGameplays = 2;
        var result = new SimulationRunner(simulation,
                GameFactory.create(simulation.gameId, simulation.parWorkbookPath)).run();
        require(result.getMaxWinMultiplier() == 100.0,
                "simulation result preserves the PAR-defined max-win setting");
        require(result.getTotalGameStats().getWinDist().values().stream()
                        .mapToLong(Long::longValue).sum() == simulation.rounds,
                "PAR-backed Monte Carlo completes configured rounds");

        Path reports = Files.createTempDirectory("javasim-par-report-");
        try {
            new Print(result, reports).printToConsole(simulation,
                    new PrintWriter(new StringWriter()));
            Path reportFolder;
            try (var reportFolders = Files.list(reports)) {
                reportFolder = reportFolders.findFirst().orElseThrow();
            }
            String stats = Files.readString(reportFolder.resolve("simulation_stats.txt"));
            require(stats.contains("Max Win: 100.00× round stake"),
                    "simulation report prints the PAR-defined max win");
            require(stats.contains("Base SD: ") && stats.contains("Free SD: "),
                    "simulation report prints shared per-mode deviations");
            require(Files.isRegularFile(reportFolder.resolve(parPath.getFileName())),
                    "simulation report includes selected PAR workbook");
            var savedWorkbook = ExcelWorkbookReader.read(reportFolder.resolve(parPath.getFileName()));
            require(savedWorkbook.sheets().keySet().equals(java.util.Set.of("Config", "Reels")),
                    "saved report workbook contains only Config and Reels sheets");
            new ExpandingWildParParser().parse(reportFolder.resolve(parPath.getFileName()));
            require(Files.isRegularFile(reportFolder.resolve("config.html")),
                    "simulation report includes HTML config view");
            Path savedGameplays = reportFolder.resolve("saved_gameplays.csv");
            require(Files.isRegularFile(savedGameplays),
                    "simulation report includes replay records");
            ReplayerMain.replay(savedGameplays, 1,
                    new java.io.PrintStream(java.io.OutputStream.nullOutputStream()));
        } finally {
            try (var entries = Files.walk(reports)) {
                entries.sorted(Comparator.reverseOrder()).forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (Exception exception) {
                        throw new RuntimeException(exception);
                    }
                });
            }
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
