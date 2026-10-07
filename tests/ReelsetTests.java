import GameModuleFramework.reels.ReelGrid;
import GameModuleFramework.reels.ReelStrip;
import GameModuleFramework.symbols.Symbol;
import game.ExhaustiveReelGame;
import game.GameFactory;
import game.GameSession;
import game.expandingwild.ExpandingWildGame;
import game.expandingwild.config.ExpandingWildConfig;
import game.model.SpinResult;
import GameModuleFramework.paylines.Payline;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import toolkit.reelset.FullReelsetSimulator;
import toolkit.reelset.ReelsetReportPrinter;
import toolkit.reelset.ReelsetSimulationResult;
import toolkit.progress.ProgressBar;

public final class ReelsetTests {
    private static int assertions;

    public static void main(String[] args) {
        testExactStopGrid();
        testEveryReelCombinationAndAggregates();
        testExpandingWildCombinationCount();
        testLargeRunProgressDisplay();
        testFullReelsetReportsProgress();
        testAwardHitsPrintAsSymbolTable();
        System.out.println("Passed " + assertions + " reelset assertions.");
    }

    private static void testExactStopGrid() {
        Symbol a = new Symbol("STOP_A");
        Symbol b = new Symbol("STOP_B");
        ReelGrid grid = ReelGrid.atStops(List.of(new ReelStrip(List.of(a, b))), 3,
                new int[] {1});

        check(grid.getSymbol(0, 0).equals(b)
                        && grid.getSymbol(0, 1).equals(a)
                        && grid.getSymbol(0, 2).equals(b),
                "exact stops create a wrapped visible reel window");
    }

    private static void testEveryReelCombinationAndAggregates() {
        ExhaustiveReelGame game = new SmallExhaustiveGame();
        ReelsetSimulationResult singleWorker = new FullReelsetSimulator()
                .simulate(game, 2.0, 1);
        ReelsetSimulationResult multipleWorkers = new FullReelsetSimulator()
                .simulate(game, 2.0, 3);

        check(singleWorker.combinations() == 6,
                "simulator visits the product of each reel's stop count");
        check(singleWorker.featureTriggers() == 2,
                "simulator counts feature triggers without playing the feature");
        check(singleWorker.awardHits().equals(Map.of("TEST 3oak", 3L)),
                "simulator aggregates line award hits");
        check(singleWorker.totalWinnings() == 12.0 && singleWorker.totalStake() == 12.0,
                "simulator totals every combination's stake and payout");
        check(singleWorker.rtp() == 1.0,
                "simulator calculates full-cycle RTP");
        check(singleWorker.equals(multipleWorkers),
                "parallel partitions preserve exhaustive aggregate results");
    }

    private static void testExpandingWildCombinationCount() {
        ExhaustiveReelGame game = (ExhaustiveReelGame) GameFactory.create("expanding-wild");
        check(game.getReelCount() == 5, "expanding wild exposes five reels to the tool");
        check(FullReelsetSimulator.countCombinations(game) == 1_555_200_000L,
                "full reelset count includes both equally sized basegame spin sets");
        check(FullReelsetSimulator.countWeightedCombinations(game) == 3_110_400_000L,
                "weighted combination count applies the 3:1 set selector");
        SpinResult featureless = game.createStopEvaluator(0)
                .evaluate(new int[] {0, 0, 0, 0, 0}, 1.0);
        check(!featureless.hasFreeSpin() && featureless.getWin() >= 0.0,
                "full reelset evaluation pays the reel symbols without triggering features");

        ExpandingWildConfig smallConfig = new ExpandingWildConfig();
        smallConfig.baseGame.sets.get(0).reelStrips = List.of(
                List.of(0, 0, 0, 0, 0, 0),
                List.of(0, 0, 0, 0, 0, 0),
                List.of(0, 0, 0, 0, 0, 0),
                List.of(0, 0, 0, 0, 0, 0),
                List.of(0, 0, 0, 0, 0, 0));
        smallConfig.baseGame.sets.get(1).reelStrips = List.of(
                List.of(0, 0, 0, 0, 0, 0), List.of(0, 0, 0, 0, 0, 0),
                List.of(0, 0, 0, 0, 0, 0), List.of(0, 0, 0, 0, 0, 0),
                List.of(0, 0, 0, 0, 0, 0));
        smallConfig.paylines = List.of(new Payline(2, 2, 2, 2, 2));
        ReelsetSimulationResult smallRun = new FullReelsetSimulator().simulate(
                new ExpandingWildGame(smallConfig), 1.0, 2);
        check(smallRun.combinations() == 15_552,
                "Expanding Wild evaluates every stop combination in both small sets");
        check(smallRun.weightedCombinations() == 31_104,
                "Expanding Wild applies the selector weights to exhaustive outcomes");
        check(smallRun.featureTriggers() == 0,
                "full reelset mode ignores scatter-trigger mechanics");
    }

    private static void testLargeRunProgressDisplay() {
        ByteArrayOutputStream outputBytes = new ByteArrayOutputStream();
        try (ProgressBar progress = new ProgressBar("Test", 100_000,
                new PrintStream(outputBytes, true, StandardCharsets.UTF_8))) {
            progress.advance(50_000);
            progress.advance(50_000);
        }
        String output = outputBytes.toString(StandardCharsets.UTF_8);
        check(output.contains("Test [===============               ]  50% (50000/100000)"),
                "progress output reports partial large-run completion");
        check(output.contains("Test [==============================] 100% (100000/100000)"),
                "progress output reaches one hundred percent");
    }

    private static void testFullReelsetReportsProgress() {
        ByteArrayOutputStream outputBytes = new ByteArrayOutputStream();
        PrintStream previousError = System.err;
        try (PrintStream capturedError = new PrintStream(
                outputBytes, true, StandardCharsets.UTF_8)) {
            System.setErr(capturedError);
            new FullReelsetSimulator().simulate(new LargeProgressGame(), 1.0, 4);
        } finally {
            System.setErr(previousError);
        }
        String output = outputBytes.toString(StandardCharsets.UTF_8);
        check(output.contains("Full reelset [") && output.contains("100% (100000/100000)"),
                "full reelset runner displays progress through completion");
    }

    private static void testAwardHitsPrintAsSymbolTable() {
        ReelsetSimulationResult result = new ReelsetSimulationResult(
                1, 0, 1.0, 0.0, Map.of(
                        "T1 3oak", 11L, "T1 4oak", 4L, "T1 5oak", 2L,
                        "L1 3oak", 6L, "L1 4oak", 0L, "L1 5oak", 1L));
        ByteArrayOutputStream outputBytes = new ByteArrayOutputStream();
        ReelsetReportPrinter.print(result,
                new PrintStream(outputBytes, true, StandardCharsets.UTF_8));
        String output = outputBytes.toString(StandardCharsets.UTF_8);

        check(output.contains("Symbol |     3oak |     4oak |     5oak"),
                "report uses match lengths as horizontal table columns");
        check(output.contains("L1     |        6 |        0 |        1")
                        && output.contains("T1     |       11 |        4 |        2"),
                "report uses symbols as rows and prints their award hit counts");
    }

    private static void check(boolean condition, String message) {
        assertions++;
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static final class SmallExhaustiveGame implements ExhaustiveReelGame {
        @Override
        public int getReelCount() {
            return 2;
        }

        @Override
        public int getStopCount(int reelIndex) {
            return reelIndex == 0 ? 2 : 3;
        }

        @Override
        public StopEvaluator createStopEvaluator() {
            return (stops, stake) -> {
                boolean win = stops[0] == 1;
                boolean trigger = stops[1] == 2;
                return new SpinResult(win ? stake * 2 : 0.0,
                        win ? 1 : 0, trigger, win ? List.of("TEST 3oak") : List.of());
            };
        }

        @Override
        public GameSession createSession(Random random) {
            throw new UnsupportedOperationException("Not used by exhaustive evaluation");
        }

        @Override
        public int[] getPaytable() {
            return new int[0];
        }
    }

    private static final class LargeProgressGame implements ExhaustiveReelGame {
        @Override
        public int getReelCount() {
            return 1;
        }

        @Override
        public int getStopCount(int reelIndex) {
            return 100_000;
        }

        @Override
        public StopEvaluator createStopEvaluator() {
            return (stops, stake) -> new SpinResult(0.0, 0, false);
        }

        @Override
        public GameSession createSession(Random random) {
            throw new UnsupportedOperationException("Not used by exhaustive evaluation");
        }

        @Override
        public int[] getPaytable() {
            return new int[0];
        }
    }
}
