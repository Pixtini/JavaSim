package game.expandingwild.tests;

import GameModuleFramework.features.AdditiveMultipliers;
import GameModuleFramework.features.ScatterTrigger;
import GameModuleFramework.paylines.Payline;
import GameModuleFramework.paylines.PaylineEvaluator;
import GameModuleFramework.paylines.PaylineWinCalculator;
import GameModuleFramework.paylines.LineWinSelectionPolicy;
import GameModuleFramework.paylines.Paytable;
import GameModuleFramework.probability.WeightedTable;
import GameModuleFramework.reels.ReelGrid;
import GameModuleFramework.reels.ReelStrip;
import GameModuleFramework.reels.WildExpansion;
import GameModuleFramework.symbols.Symbol;
import game.GameFactory;
import game.expandingwild.ExpandingWildWinCalculator;
import game.expandingwild.basegame.ExpandingWildBaseGame;
import game.expandingwild.config.ExpandingWildConfig;
import game.expandingwild.config.ExpandingWildConfigAdapter;
import game.expandingwild.features.ExpandingWildFreeGame;
import game.expandingwild.model.ExpandingWildLineWin;
import game.expandingwild.model.ExpandingWildSpinResult;
import game.model.GameRoundResult;
import game.model.SpinResult;
import simulation.config.SimConfig;
import simulation.engine.SimulationRunner;
import simulation.result.SimulationResult;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

public final class ExpandingWildGameTests {
    private static int assertions;

    public static void main(String[] args) {
        testSymbolIdMapping();
        testWeightedTableDrawAndValidation();
        testScatterTrigger();
        testReelWindowWrapsAroundStrip();
        testPaylineRequiresUnbrokenRun();
        testGenericPaylineWinSelectionAndAwards();
        testBannerExpandsAndMultipliersAdd();
        testPaytableUsesTotalRoundStake();
        testFreeGameAssignsBannerMultipliers();
        testScatterTriggerAndScatterFreeFreegame();
        testGameRunsThroughSimulator();
        System.out.println("Passed " + assertions + " expanding wild assertions.");
    }

    private static void testSymbolIdMapping() {
        check(ExpandingWildConfig.fromId(0).equals(ExpandingWildConfig.T1),
                "symbol ID zero maps to top symbol one");
        check(ExpandingWildConfig.fromId(4).equals(ExpandingWildConfig.T5),
                "symbol ID four maps to top symbol five");
        check(ExpandingWildConfig.fromId(5).equals(ExpandingWildConfig.L1),
                "symbol ID five maps to low symbol one");
        check(ExpandingWildConfig.fromId(9).equals(ExpandingWildConfig.L5),
                "symbol ID nine maps to low symbol five");
        check(ExpandingWildConfig.fromId(10).equals(ExpandingWildConfig.BANNER)
                        && ExpandingWildConfig.fromId(11).equals(ExpandingWildConfig.WILD)
                        && ExpandingWildConfig.fromId(12).equals(ExpandingWildConfig.SCATTER),
                "special symbol IDs map to banner, wild, and scatter");
    }

    private static void testWeightedTableDrawAndValidation() {
        WeightedTable<Integer> table = new WeightedTable<>(List.of(
                new WeightedTable.Entry<>(2, 1),
                new WeightedTable.Entry<>(10, 3)));
        check(table.getTotalWeight() == 4, "weighted table totals its entry weights");
        check(table.draw(new FixedRandom(0)) == 2,
                "weighted table selects the first entry within its interval");
        check(table.draw(new FixedRandom(1)) == 10,
                "weighted table selects the next entry after the first interval");
        check(table.draw(new FixedRandom(3)) == 10,
                "weighted table includes the final ticket in the last interval");

        boolean rejectedZeroWeight = false;
        try {
            new WeightedTable<>(List.of(new WeightedTable.Entry<>(1, 0)));
        } catch (IllegalArgumentException expected) {
            rejectedZeroWeight = true;
        }
        check(rejectedZeroWeight, "weighted table rejects zero weights");
    }

    private static void testScatterTrigger() {
        Symbol scatter = ExpandingWildConfig.SCATTER;
        Symbol filler = ExpandingWildConfig.T1;
        ScatterTrigger trigger = new ScatterTrigger(scatter, 2, 0, 2);
        ReelGrid triggeredGrid = new ReelGrid(List.of(
                List.of(scatter, filler),
                List.of(scatter, filler),
                List.of(scatter, filler)));
        ScatterTrigger.Result triggered = trigger.evaluate(triggeredGrid);
        check(triggered.scatterCount() == 2 && triggered.triggered(),
                "scatter trigger counts only configured reels and caps at its threshold");

        ReelGrid untriggeredGrid = new ReelGrid(List.of(
                List.of(scatter, filler),
                List.of(scatter, filler),
                List.of(filler, filler)));
        ScatterTrigger.Result untriggered = trigger.evaluate(untriggeredGrid);
        check(untriggered.scatterCount() == 1 && !untriggered.triggered(),
                "scatter trigger reports a below-threshold count without triggering");
    }

    private static void testReelWindowWrapsAroundStrip() {
        Symbol a = new Symbol("TEST_A");
        Symbol b = new Symbol("TEST_B");
        ReelStrip strip = new ReelStrip(List.of(a, b));

        List<Symbol> window = strip.spinWindow(new FixedRandom(1), 5);

        check(window.equals(List.of(b, a, b, a, b)), "reel window wraps around the strip");
    }

    private static void testPaylineRequiresUnbrokenRun() {
        Symbol a = new Symbol("PAY_A");
        Symbol b = new Symbol("PAY_B");
        Symbol wild = new Symbol("PAY_WILD");
        Paytable paytable = new Paytable(Map.of(a, Map.of(3, 10.0, 4, 20.0, 5, 30.0)));
        Payline flat = new Payline(2, 2, 2, 2, 2);

        ReelGrid unbroken = rowGrid(List.of(a, a, a, b, b), 5, b);
        var win = PaylineEvaluator.candidates(unbroken, flat, paytable, wild);
        check(win.size() == 1, "three matching symbols pay when consecutive from reel one");
        check(win.get(0).matchingReels() == 3, "three-of-a-kind stops at first mismatch");
        check(win.get(0).baseWin() == 10.0, "three-of-a-kind uses the three-symbol pay");

        ReelGrid broken = rowGrid(List.of(a, a, b, a, a), 5, b);
        check(PaylineEvaluator.candidates(broken, flat, paytable, wild).isEmpty(),
                "symbols after a mismatch do not form a win");

        Paytable wildPrefixPaytable = new Paytable(Map.of(
                a, Map.of(3, 20.0),
                b, Map.of(3, 10.0, 4, 15.0)));
        ReelGrid wildPrefix = rowGrid(List.of(wild, wild, wild, b, b), 5, b);
        var wildPrefixWins = PaylineEvaluator.candidates(
                wildPrefix, flat, wildPrefixPaytable, wild);
        check(wildPrefixWins.stream().anyMatch(candidate ->
                        candidate.symbol().equals(a) && candidate.matchingReels() == 3),
                "leading wilds can form their own shorter paying combination");
    }

    private static void testGenericPaylineWinSelectionAndAwards() {
        Symbol first = new Symbol("SELECT_A");
        Symbol second = new Symbol("SELECT_C");
        Symbol wild = new Symbol("SELECT_WILD");
        Paytable paytable = new Paytable(Map.of(
                first, Map.of(3, 25.0),
                second, Map.of(5, 20.0)));
        PaylineWinCalculator calculator = new PaylineWinCalculator(
                paytable, List.of(new Payline(2, 2, 2, 2, 2)), wild,
                LineWinSelectionPolicy.highestPayout());

        var wins = calculator.calculate(rowGrid(
                List.of(wild, wild, wild, second, second), 5, second), 2.0, candidate -> 1.0);

        check(wins.size() == 1 && wins.get(0).candidate().symbol().equals(first),
                "GMF selects the highest paying candidate on a line");
        check(wins.get(0).baseWin() == 50.0 && wins.get(0).totalWin() == 50.0,
                "GMF scales candidate payouts by total round stake");
        check(paytable.getAwards().size() == 2
                        && paytable.getAwards().get(0).matchingSymbols() == 3
                        && paytable.getAwards().get(1).matchingSymbols() == 5,
                "paytable exposes stable award categories for game reporting");
    }

    private static void testBannerExpandsAndMultipliersAdd() {
        Symbol a = ExpandingWildConfig.fromId(0);
        List<List<Symbol>> reels = new ArrayList<>();
        reels.add(List.of(ExpandingWildConfig.BANNER, a, a, a, a));
        reels.add(List.of(ExpandingWildConfig.BANNER, a, a, a, a));
        for (int reel = 2; reel < 5; reel++) {
            reels.add(List.of(a, a, a, a, a));
        }

        WildExpansion.Result expanded = WildExpansion.expandColumns(
                new ReelGrid(reels), ExpandingWildConfig.BANNER, ExpandingWildConfig.WILD);
        check(expanded.expandedReels().equals(java.util.Set.of(0, 1)),
                "visible banners expand their full reels");
        for (int row = 0; row < 5; row++) {
            check(expanded.grid().getSymbol(0, row).equals(ExpandingWildConfig.WILD),
                    "expanded banner fills the reel height");
        }

        Paytable paytable = new Paytable(Map.of(a, Map.of(3, 10.0, 4, 20.0, 5, 30.0)));
        ExpandingWildWinCalculator calculator = new ExpandingWildWinCalculator(
                paytable, List.of(new Payline(1, 1, 1, 1, 1)), ExpandingWildConfig.WILD);
        List<ExpandingWildLineWin> wins = calculator.calculate(
                expanded.grid(), Map.of(1, 2, 2, 3));

        check(wins.size() == 1, "expanded wilds complete a five-of-a-kind line");
        check(wins.get(0).multiplier() == 5.0, "banner multipliers add to five times");
        check(wins.get(0).totalWin() == 150.0, "line payout uses the additive multiplier");
        check(AdditiveMultipliers.combine(List.of()) == 1.0,
                "no active banner multiplier leaves the line at one times");
    }

    private static void testPaytableUsesTotalRoundStake() {
        ExpandingWildConfig config = deterministicConfig();
        ExpandingWildSpinResult baseResult = new ExpandingWildBaseGame(
                config, new FixedRandom(0)).spin(2.5);
        check(baseResult.getWin() == 75.0,
                "base paytable multiplier scales by total round stake");

        ExpandingWildSpinResult freeResult = new ExpandingWildFreeGame(
                config, new FixedRandom(0)).spin(2.5);
        check(freeResult.getWin() == 75.0,
                "freegame paytable multiplier uses the triggering round stake");

        GameRoundResult roundResult = new game.expandingwild.ExpandingWildGameSession(
                config, new FixedRandom(0)).playRound(2.5);
        check(roundResult.getBaseGameResult().getWin() == 75.0
                        && roundResult.getFreeGameResults().stream()
                                .allMatch(spin -> spin.getWin() == 75.0),
                "a round passes the same total stake into its triggered free spins");
    }

    private static void testScatterTriggerAndScatterFreeFreegame() {
        ExpandingWildConfig config = deterministicConfig();
        ExpandingWildSpinResult baseResult = new ExpandingWildBaseGame(
                config, new FixedRandom(0)).spin();

        check(baseResult.getScatterCount() == 3, "scatters on reels one, three, and five are counted");
        check(baseResult.hasFreeSpin(), "three visible scatters trigger the feature game");
        check(baseResult.getWin() == 30.0, "base line win is calculated from its five-symbol pay");
        check(baseResult.getAwards().equals(List.of("T1 5oak")),
                "base spin exposes its per-symbol line award to simulator stats");

        ExpandingWildSpinResult freeResult = new ExpandingWildFreeGame(
                config, new FixedRandom(0)).spin();
        check(freeResult.getScatterCount() == 0, "freegame reels contain no scatters");
        check(!freeResult.hasFreeSpin(), "freegame spin cannot retrigger freegames");
        check(freeResult.isFreeGameSpin(), "freegame result identifies its mode");

        GameRoundResult fullRound = new game.expandingwild.ExpandingWildGameSession(
                config, new FixedRandom(0)).playRound();
        check(fullRound.getFreeGameResults().size() == 2,
                "basegame feature award plays configured number of free spins");
    }

    private static void testFreeGameAssignsBannerMultipliers() {
        ExpandingWildConfig config = deterministicConfig();
        config.baseReelStrips = List.of(
                List.of(10, 0, 0, 0, 0),
                List.of(10, 0, 0, 0, 0),
                List.of(10, 0, 0, 0, 0),
                List.of(10, 0, 0, 0, 0),
                List.of(10, 0, 0, 0, 0));

        ExpandingWildSpinResult result = new ExpandingWildFreeGame(
                config, new FixedRandom(0)).spin();

        check(result.getBannerMultipliersByReel().size() == 5,
                "each expanded banner reel receives one multiplier");
        check(result.getBannerMultipliersByReel().values().stream().allMatch(value -> value == 2),
                "seeded random draws produce configured inclusive multiplier bounds");
        check(result.getLineWins().get(0).multiplier() == 10.0,
                "five banner multipliers add together on one line");
        check(result.getLineWins().get(0).totalWin() == 300.0,
                "combined banner multiplier applies to the line payout");

        ExpandingWildSpinResult maximumResult = new ExpandingWildFreeGame(
                config, new FixedRandom(8)).spin();
        check(maximumResult.getBannerMultipliersByReel().values().stream()
                        .allMatch(value -> value == 10),
                "random banner multiplier includes the configured upper bound");
        check(maximumResult.getLineWins().get(0).multiplier() == 50.0,
                "five maximum banner multipliers add to fifty times");

        config.bannerMultiplierWeights = List.of(
                new WeightedTable.Entry<>(2, 1),
                new WeightedTable.Entry<>(10, 3));
        ExpandingWildSpinResult lowWeightedResult = new ExpandingWildFreeGame(
                config, new FixedRandom(0)).spin();
        ExpandingWildSpinResult highWeightedResult = new ExpandingWildFreeGame(
                config, new FixedRandom(1)).spin();
        check(lowWeightedResult.getBannerMultipliersByReel().values().stream()
                        .allMatch(value -> value == 2),
                "weighted table selects its first multiplier for draws in its weight range");
        check(highWeightedResult.getBannerMultipliersByReel().values().stream()
                        .allMatch(value -> value == 10),
                "weighted table selects the next multiplier after the first weight range");

    }

    private static void testGameRunsThroughSimulator() {
        SimConfig simConfig = new SimConfig();
        simConfig.gameId = "expanding-wild";
        simConfig.rounds = 1_000_000;
        simConfig.partitions = 32;
        simConfig.threads = 4;
        simConfig.seed = 4042026L;
        simConfig.usePreviousSeed = true;
        simConfig.exportReport = false;

        SimulationResult result = new SimulationRunner(
                simConfig, GameFactory.create(simConfig.gameId)).run();

        check(result.getBaseGameStats().getRounds() == simConfig.rounds,
                "simulation records each basegame spin once");
        check(result.getFreeGameStats().getFreegameTriggers() > 0,
                "seeded simulation reaches the scatter feature");
        check(result.getFreeGameStats().getRounds()
                        == result.getFreeGameStats().getFreegameTriggers() * 8,
                "simulation records eight free spins per trigger");
        double totalWin = result.getTotalGameStats().getWinDist().entrySet().stream()
                .mapToDouble(entry -> entry.getKey() * entry.getValue())
                .sum();
        check(result.getTotalGameStats().getWinDist().values().stream()
                        .mapToLong(Long::longValue).sum() == simConfig.rounds,
                "total-game distribution records one outcome per base round");
        check(result.getBaseGameStats().getAwardCounts().size() == 30,
                "simulation registers all regular symbol and match-length award categories");
        check(result.getBaseGameStats().getAwardCounts().values().stream()
                        .mapToLong(Long::longValue).sum() > 0,
                "simulation aggregates per-line basegame awards");
        check(result.getFreeGameStats().getAwardCounts().values().stream()
                        .mapToLong(Long::longValue).sum() > 0,
                "simulation aggregates per-line freegame awards");

        SimConfig singleThreadConfig = new SimConfig();
        singleThreadConfig.gameId = simConfig.gameId;
        singleThreadConfig.rounds = simConfig.rounds;
        singleThreadConfig.partitions = simConfig.partitions;
        singleThreadConfig.threads = 1;
        singleThreadConfig.seed = simConfig.seed;
        singleThreadConfig.usePreviousSeed = true;
        singleThreadConfig.exportReport = false;
        SimulationResult singleThreadResult = new SimulationRunner(
                singleThreadConfig, GameFactory.create(singleThreadConfig.gameId)).run();
        check(result.getTotalGameStats().getWinDist().equals(
                        singleThreadResult.getTotalGameStats().getWinDist()),
                "seeded reel game output is stable across thread counts");
        check(result.getFreeGameStats().getWinDist().equals(
                        singleThreadResult.getFreeGameStats().getWinDist()),
                "seeded freegame outcomes are stable across thread counts");
        check(new ExpandingWildConfig().paylines.size() == 15,
                "default game config defines fifteen paylines");
        int symbolCount = 0;
        for (Symbol ignored : new ExpandingWildConfig().paytable.getSymbols()) {
            symbolCount++;
        }
        check(symbolCount == 10, "default paytable defines ten regular symbols");
        ExpandingWildConfig defaultConfig = new ExpandingWildConfig();
        for (int reelIndex : defaultConfig.scatterReels) {
            ReelStrip strip = ExpandingWildConfigAdapter.toBaseReelStrips(defaultConfig)
                    .get(reelIndex);
            for (int stop = 0; stop < strip.getSymbols().size(); stop++) {
                List<Symbol> window = strip.spinWindow(new FixedRandom(stop), 5);
                check(!(window.contains(ExpandingWildConfig.BANNER)
                                && window.contains(ExpandingWildConfig.SCATTER)),
                        "default strips keep scatters visible when banners do not cover them");
            }
        }
        System.out.printf("Seeded expanding-wild run: %d rounds, %d freegame triggers, "
                        + "%d free spins, base win %.2f, freegame win %.2f, "
                        + "total win %.2f, RTP %.2f%%%n",
                simConfig.rounds,
                result.getFreeGameStats().getFreegameTriggers(),
                result.getFreeGameStats().getRounds(),
                result.getBaseGameStats().getTotalWinnings(),
                result.getFreeGameStats().getTotalWinnings(),
                totalWin,
                totalWin / (simConfig.rounds * simConfig.stake) * 100.0);
    }

    private static ExpandingWildConfig deterministicConfig() {
        ExpandingWildConfig config = new ExpandingWildConfig();
        config.freeGamesAwarded = 2;
        config.baseReelStrips = List.of(
                List.of(12, 0, 0, 0, 0),
                List.of(0, 0, 0, 0, 0),
                List.of(12, 0, 0, 0, 0),
                List.of(0, 0, 0, 0, 0),
                List.of(12, 0, 0, 0, 0));
        config.paylines = List.of(new Payline(1, 1, 1, 1, 1));
        config.paytable = new Paytable(
                Map.of(ExpandingWildConfig.fromId(0), Map.of(3, 10.0, 4, 20.0, 5, 30.0)));
        return config;
    }

    private static ReelGrid rowGrid(List<Symbol> row, int height, Symbol filler) {
        List<List<Symbol>> reels = new ArrayList<>();
        for (Symbol symbol : row) {
            List<Symbol> cells = new ArrayList<>(height);
            for (int visibleRow = 0; visibleRow < height; visibleRow++) {
                cells.add(visibleRow == 2 ? symbol : filler);
            }
            reels.add(cells);
        }
        return new ReelGrid(reels);
    }

    private static void check(boolean condition, String message) {
        assertions++;
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static final class FixedRandom extends Random {
        private final int value;

        private FixedRandom(int value) {
            this.value = value;
        }

        @Override
        public int nextInt(int bound) {
            return Math.floorMod(value, bound);
        }
    }
}
