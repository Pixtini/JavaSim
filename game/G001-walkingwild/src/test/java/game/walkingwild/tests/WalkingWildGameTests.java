package game.walkingwild.tests;

import GameModuleFramework.paylines.Payline;
import GameModuleFramework.paylines.PaylineWinCalculator;
import GameModuleFramework.paylines.LineWinSelectionPolicy;
import GameModuleFramework.reels.ReelGrid;
import GameModuleFramework.symbols.Symbol;
import game.Game;
import game.GameFactory;
import game.ExhaustiveReelGame;
import game.model.GameRoundResult;
import game.model.SpinResult;
import game.walkingwild.WalkingWildPlacement;
import game.walkingwild.WalkingWildWinEvaluator;
import game.walkingwild.WalkingWildGame;
import game.walkingwild.basegame.WalkingWildBaseGame;
import game.walkingwild.config.WalkingWildConfig;
import game.walkingwild.config.WalkingWildParParser;
import game.walkingwild.features.WalkingWildFreeGame;
import game.walkingwild.model.WalkingWildSpin;
import game.walkingwild.model.WildPosition;
import toolkit.player.PlayerDisplayData;
import toolkit.player.WalkingWildPlayerAdapter;
import toolkit.replay.ReplayerMain;
import toolkit.viewer.WinFinder;
import toolkit.viewer.WinScreenPrinter;
import simulation.config.SimConfig;
import simulation.engine.SimulationRunner;
import simulation.reporting.Print;
import simulation.result.SimulationResult;
import toolkit.par.ParWorkbookModes;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Deterministic rule, parser, mode-cost, and report checks for G001. */
public final class WalkingWildGameTests {
    private static int assertions;
    private static final Path PAR = Path.of("game", "G001-walkingwild", "source",
            "G001-walkingWildsPAR.xlsx");

    private WalkingWildGameTests() {}

    public static void main(String[] args) throws Exception {
        testModeParser();
        testPaylinePayoutAndHighestSelection();
        testWildWeightExclusionsAreTemporary();
        testFreegameWildPersistenceAndDeterminism();
        testScatterInsertionAndFreegameAwards();
        testReplayAndPlayerAdapter();
        testSeededReportsByMode();
        System.out.println("Passed " + assertions + " G001 assertions.");
    }

    private static void testModeParser() throws Exception {
        WalkingWildParParser parser = new WalkingWildParParser();
        WalkingWildConfig normal = parser.parse(PAR, "Normal");
        WalkingWildConfig bonusBuy = parser.parse(PAR, "BB1");
        check(normal.mode().equals("Normal"), "normal parser selects Normal pair");
        check(bonusBuy.mode().equals("BB1"), "bonus parser selects BB1 pair");
        check(normal.reelCount() == 6 && normal.visibleRows() == 4, "parser reads 6x4 screen");
        check(normal.paylines().size() == 20, "parser reads all paylines");
        check(normal.costMultiplier() == 1.0, "Normal cost is one base stake");
        check(bonusBuy.costMultiplier() == 100.0, "BB1 cost is 100 base stakes");
        check(normal.maxWinMultiplier() == 100.0 && bonusBuy.maxWinMultiplier() == 100.0,
                "both modes load the configured cap");
        check(normal.scatterFreeGames().equals(java.util.Map.of(4, 5, 5, 8, 6, 12)),
                "scatter map reads 4, 5, and 6 scatter awards");
        check(normal.baseSets().size() == 2 && normal.freeSets().size() == 1,
                "parser loads BG sets 0 and 1 and FG set 0");
        check(ParWorkbookModes.select(PAR, "BB1").configSheet().equals("BB1-Config"),
                "mode selection resolves only the matching Config sheet");
        Game g001 = GameFactory.create("G001-walkingwild", PAR, "Normal");
        check(g001 instanceof ExhaustiveReelGame,
                "G001 is available to the full-reelset tool like Expanding Wild");
        ExhaustiveReelGame reelGame = (ExhaustiveReelGame) g001;
        check(reelGame.getReelSetCount() == 2 && reelGame.getReelSetWeight(1) > 0,
                "full-reelset capability exposes selectable Basegame sets and weights");
        check(reelGame.createStopEvaluator(0).evaluate(new int[6], 1.0).getSetIndex() == 0,
                "full-reelset evaluator resolves a G001 Basegame stop combination");
        expectIllegalArgument(() -> parser.parse(PAR, "missing"), "unavailable mode is rejected");
    }

    private static void testPaylinePayoutAndHighestSelection() throws Exception {
        WalkingWildConfig config = new WalkingWildParParser().parse(PAR, "Normal");
        List<List<Symbol>> reels = new ArrayList<>();
        List<Symbol> lineSymbols = List.of(new Symbol("T1"), new Symbol("T1"),
                WalkingWildConfig.WILD, new Symbol("T1"), new Symbol("T1"), new Symbol("T1"));
        for (Symbol symbol : lineSymbols) reels.add(List.of(symbol, symbol, symbol, symbol));
        ReelGrid grid = new ReelGrid(reels);
        PaylineWinCalculator calculator = new PaylineWinCalculator(config.paytable(),
                List.of(new Payline(0, 0, 0, 0, 0, 0)), WalkingWildConfig.WILD,
                LineWinSelectionPolicy.highestPayout());
        var winningLine = calculator.calculate(grid, 2.0, ignored -> 1.0);
        check(winningLine.size() == 1, "one highest award is selected for the line");
        check(winningLine.get(0).totalWin() == 80.0,
                "six of a kind with wild substitution pays 40 times the base stake");

        List<List<Symbol>> allWildReels = new ArrayList<>();
        for (int reel = 0; reel < 6; reel++) allWildReels.add(List.of(
                WalkingWildConfig.WILD, WalkingWildConfig.WILD,
                WalkingWildConfig.WILD, WalkingWildConfig.WILD));
        var allWild = calculator.calculate(new ReelGrid(allWildReels), 1.0, ignored -> 1.0);
        check(allWild.size() == 1 && allWild.get(0).totalWin() == 40.0,
                "all-wild line pays the highest configured all-wild award once");
    }

    private static void testWildWeightExclusionsAreTemporary() {
        List<List<Long>> weights = new ArrayList<>();
        for (int reel = 0; reel < 6; reel++) {
            List<Long> rows = new ArrayList<>();
            for (int row = 0; row < 4; row++) rows.add(reel < 2 && row == 0 ? 1L : 0L);
            weights.add(rows);
        }
        WalkingWildConfig.HeatMap locations = new WalkingWildConfig.HeatMap(weights, Integer.MAX_VALUE);
        ReelGrid blank = regularGrid();
        var first = WalkingWildPlacement.place(blank, locations, List.of(), 1, new Random(1));
        var second = WalkingWildPlacement.place(first.grid(), locations, first.placements(),
                1, new Random(1));
        check(first.placements().size() == 1 && second.placements().size() == 1,
                "weight table places the requested wilds");
        check(!first.placements().get(0).equals(second.placements().get(0)),
                "a cell assigned earlier in the sequence has zero weight for later placements");
        check(weights.get(0).get(0) == 1L && weights.get(1).get(0) == 1L,
                "temporary exclusions do not mutate PAR heatmap weights");
    }

    private static void testFreegameWildPersistenceAndDeterminism() throws Exception {
        WalkingWildConfig config = new WalkingWildParParser().parse(PAR, "Normal");
        WalkingWildFreeGame firstEngine = new WalkingWildFreeGame(config, new Random(8585));
        WalkingWildFreeGame secondEngine = new WalkingWildFreeGame(config, new Random(8585));
        List<WildPosition> previous = List.of(new WildPosition(0, 0), new WildPosition(1, 1));
        WalkingWildSpin first = firstEngine.spin(1.0, previous);
        WalkingWildSpin second = secondEngine.spin(1.0, previous);
        check(first.grid().getReelCount() == 6 && first.grid().getHeight() == 4,
                "Freegame spin returns the configured screen");
        check(first.wildPositions().size() >= 2 && first.wildPositions().size() <= 5,
                "Freegame table preserves existing wilds and respects the five-wild cap");
        check(first.wildPositions().stream().distinct().count() == first.wildPositions().size(),
                "new and persisted wild destinations do not overlap");
        check(first.wildPositions().equals(second.wildPositions())
                        && first.result().getWin() == second.result().getWin(),
                "Freegame outcomes are repeatable for the same seed and input state");
        for (WildPosition position : first.wildPositions()) {
            check(first.grid().getSymbol(position.reel(), position.row()).equals(WalkingWildConfig.WILD),
                    "every returned persistent wild occupies the returned grid");
        }
    }

    private static void testScatterInsertionAndFreegameAwards() throws Exception {
        WalkingWildConfig config = new WalkingWildParParser().parse(PAR, "Normal");
        WalkingWildBaseGame baseGame = new WalkingWildBaseGame(config, new Random(1));
        int insertedScatters = 0;
        int freegameTriggers = 0;
        for (int spin = 0; spin < 100_000; spin++) {
            WalkingWildSpin result = baseGame.spin(1.0);
            insertedScatters += result.scatterCount();
            if (result.result().hasFreeSpin()) freegameTriggers++;
        }
        check(insertedScatters > 0, "configured Basegame scatter insertions appear on the screen");
        check(freegameTriggers > 0, "four or more inserted scatters trigger the configured Freegame");
    }

    private static void testReplayAndPlayerAdapter() throws Exception {
        WalkingWildConfig config = new WalkingWildParParser().parse(PAR, "Normal");
        Game game = new WalkingWildGame(config);
        var session = game.createSession(new Random(1234));
        GameRoundResult captured = null;
        for (int round = 0; round < 20_000; round++) {
            GameRoundResult candidate = session.playRound(1.0, 100.0, true);
            if (!candidate.getFreeGameResults().isEmpty()) {
                captured = candidate;
                break;
            }
        }
        check(captured != null, "seeded captured rounds include a triggered Freegame");
        var replayed = game.createSession(new Random(99)).replayRound(
                1.0, 100.0, captured.getReplayEvents());
        double capturedFeatureWin = captured.getFreeGameResults().stream()
                .mapToDouble(SpinResult::getWin).sum();
        double replayedFeatureWin = replayed.getFreeGameResults().stream()
                .mapToDouble(SpinResult::getWin).sum();
        check(Math.abs(captured.getBaseGameResult().getWin()
                        - replayed.getBaseGameResult().getWin()) < 1e-9
                        && Math.abs(capturedFeatureWin - replayedFeatureWin) < 1e-9,
                "G001 replay recalculates Basegame and Freegame wins from recorded screens");
        boolean sameScreens = screenEquals(((WalkingWildSpin) captured.getBaseGameResult()).grid(),
                ((WalkingWildSpin) replayed.getBaseGameResult()).grid());
        for (int index = 0; index < captured.getFreeGameResults().size(); index++) {
            sameScreens &= screenEquals(
                    ((WalkingWildSpin) captured.getFreeGameResults().get(index)).grid(),
                    ((WalkingWildSpin) replayed.getFreeGameResults().get(index)).grid());
        }
        check(sameScreens, "G001 replay reconstructs the same reel screens and wild placements");
        WalkingWildPlayerAdapter adapter = new WalkingWildPlayerAdapter(config, new Random(3));
        PlayerDisplayData.Round display = adapter.adaptRound(replayed);
        check(display.baseGame().reelCount() == 6 && display.baseGame().rowCount() == 4,
                "player adapter exposes the 6x4 G001 screen");
        check(display.featureSpins().size() == replayed.getFreeGameResults().size()
                        && !display.featureSpins().isEmpty(),
                "player adapter exposes triggered Freegame spins");

        Path replayFolder = Files.createTempDirectory("g001-replay-");
        Files.copy(PAR, replayFolder.resolve(PAR.getFileName()), StandardCopyOption.REPLACE_EXISTING);
        Path csv = replayFolder.resolve("saved_gameplays.csv");
        simulation.replay.SavedGameplayCsv.write(csv, List.of(new simulation.replay.SavedGameplay(
                1, "G001-walkingwild", "Normal", 1.0,
                captured.getBaseGameResult().getWin() + capturedFeatureWin,
                captured.getBaseGameResult().getWin(), capturedFeatureWin,
                captured.getReplayEvents())));
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ReplayerMain.replay(csv, 1, new PrintStream(output));
        check(output.toString(java.nio.charset.StandardCharsets.UTF_8).contains("Replay validation: PASSED"),
                "saved G001 replay passes the report replayer validation");

        WinFinder.Result found = new WinFinder(game, new Random(456), 1.0, 1_000).find();
        check(found.foundWin() && found.winningSpin() instanceof WalkingWildSpin,
                "win viewer finds a detailed G001 winning screen");
        ByteArrayOutputStream viewerOutput = new ByteArrayOutputStream();
        WinScreenPrinter.print(found, 1.0, new PrintStream(viewerOutput));
        check(viewerOutput.toString(java.nio.charset.StandardCharsets.UTF_8).contains("Screen:")
                        && viewerOutput.toString(java.nio.charset.StandardCharsets.UTF_8).contains("Line "),
                "win viewer prints the G001 screen and line details");
    }

    private static void testSeededReportsByMode() throws Exception {
        Path reportRoot = Files.createTempDirectory("g001-reports-");
        for (String mode : List.of("Normal", "BB1")) {
            Game game = GameFactory.create("G001-walkingwild", PAR, mode);
            SimConfig sim = new SimConfig();
            sim.gameId = "G001-walkingwild";
            sim.gameMode = mode;
            sim.parWorkbookPath = PAR.toAbsolutePath().normalize();
            sim.rounds = 2_000;
            sim.stake = 1.0;
            sim.seed = 440055L;
            sim.usePreviousSeed = true;
            sim.threads = 2;
            sim.partitions = 16;
            sim.maxSavedGameplays = 10;
            sim.exportReport = true;
            SimulationResult result = new SimulationRunner(sim, game).run();
            double multiplier = mode.equals("BB1") ? 100.0 : 1.0;
            check(sim.modeCostMultiplier == multiplier, mode + " run uses PAR cost");
            check(result.getTotalGameStats().getRtp() >= 0.0,
                    mode + " simulation produces a valid RTP");
            Path modeRoot = reportRoot.resolve(mode);
            StringWriter consoleText = new StringWriter();
            new Print(result, modeRoot).printToConsole(sim, new PrintWriter(consoleText, true));
            check(consoleText.toString().contains("Total staked: £" + (sim.rounds * multiplier)),
                    mode + " report stake reflects the selected mode cost");
            check(consoleText.toString().contains("Mode cost multiplier: " + multiplier + "× base stake"),
                    mode + " report prints the PAR cost multiplier");
            try (var folders = Files.list(modeRoot)) {
                Path report = folders.findFirst().orElseThrow();
                Path copiedWorkbook = Files.list(report).filter(path -> path.toString().endsWith(".xlsx"))
                        .findFirst().orElseThrow();
                check(ParWorkbookModes.availableModes(copiedWorkbook).equals(List.of(mode)),
                        mode + " report workbook contains only its selected pair");
                String html = Files.readString(report.resolve("config.html"));
                check(html.contains("Simulation mode: " + mode), mode + " HTML report identifies its mode");
                String stats = Files.readString(report.resolve("simulation_stats.txt"));
                check(stats.contains("Total staked: £" + (sim.rounds * multiplier)),
                        mode + " stats report records mode-adjusted stake");
                check(!stats.contains("NaN%"), mode + " stats report does not emit undefined set RTP");
                Path replayCsv = report.resolve("saved_gameplays.csv");
                check(Files.isRegularFile(replayCsv), mode + " report includes replay events");
                ByteArrayOutputStream replayOutput = new ByteArrayOutputStream();
                ReplayerMain.replay(replayCsv, 1, new PrintStream(replayOutput));
                check(replayOutput.toString(java.nio.charset.StandardCharsets.UTF_8)
                                .contains("Replay validation: PASSED"),
                        mode + " report replay recalculates successfully");
            }
        }
    }

    private static ReelGrid regularGrid() {
        List<List<Symbol>> reels = new ArrayList<>();
        for (int reel = 0; reel < 6; reel++) reels.add(List.of(
                new Symbol("T1"), new Symbol("T2"), new Symbol("T3"), new Symbol("T4")));
        return new ReelGrid(reels);
    }

    private static boolean screenEquals(ReelGrid left, ReelGrid right) {
        if (left.getReelCount() != right.getReelCount() || left.getHeight() != right.getHeight()) {
            return false;
        }
        for (int reel = 0; reel < left.getReelCount(); reel++) {
            for (int row = 0; row < left.getHeight(); row++) {
                if (!left.getSymbol(reel, row).equals(right.getSymbol(reel, row))) return false;
            }
        }
        return true;
    }

    private static void expectIllegalArgument(ThrowingOperation operation, String message) throws Exception {
        try { operation.run(); }
        catch (IllegalArgumentException expected) { assertions++; return; }
        throw new AssertionError(message);
    }

    private static void check(boolean condition, String message) {
        assertions++;
        if (!condition) throw new AssertionError(message);
    }

    @FunctionalInterface private interface ThrowingOperation { void run() throws Exception; }
}
