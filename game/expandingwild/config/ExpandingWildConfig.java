package game.expandingwild.config;

import GameModuleFramework.paylines.Payline;
import GameModuleFramework.paylines.Paytable;
import GameModuleFramework.probability.WeightedTable;
import GameModuleFramework.features.SymbolInsertion;
import GameModuleFramework.symbols.Symbol;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Mathematical settings for the expanding wild example game. */
public final class ExpandingWildConfig {
    public static final Symbol T1 = new Symbol("T1");
    public static final Symbol T2 = new Symbol("T2");
    public static final Symbol T3 = new Symbol("T3");
    public static final Symbol T4 = new Symbol("T4");
    public static final Symbol T5 = new Symbol("T5");
    public static final Symbol L1 = new Symbol("L1");
    public static final Symbol L2 = new Symbol("L2");
    public static final Symbol L3 = new Symbol("L3");
    public static final Symbol L4 = new Symbol("L4");
    public static final Symbol L5 = new Symbol("L5");
    public static final Symbol BANNER = new Symbol("BANNER");
    public static final Symbol WILD = new Symbol("WILD");
    public static final Symbol SCATTER = new Symbol("SCATTER");

    private static final Map<Integer, Symbol> SYMBOLS_BY_ID = Map.ofEntries(
            Map.entry(0, T1), Map.entry(1, T2), Map.entry(2, T3),
            Map.entry(3, T4), Map.entry(4, T5), Map.entry(5, L1),
            Map.entry(6, L2), Map.entry(7, L3), Map.entry(8, L4),
            Map.entry(9, L5), Map.entry(10, BANNER), Map.entry(11, WILD),
            Map.entry(12, SCATTER));

    public final int reelCount = 5;
    public final int visibleRows = 5;
    /** Maximum round payout as a multiple of total round stake. */
    public double maxWinMultiplier = 100.0;
    public int freeGamesAwarded = 5;
    public final int freeGameTriggerScatterCount = 3;
    public final int[] scatterReels = {0, 2, 4};

    /** Basegame sets: scatters insert on set 0, banners insert on set 1. */
    public SpinModeConfig baseGame = new SpinModeConfig(
            List.of(
                    new SpinSetConfig(defaultReels(), List.of(),
                            List.of(scatterInsertion())),
                    new SpinSetConfig(defaultReels(), List.of(),
                            List.of(bannerInsertion()))),
            List.of(new WeightedTable.Entry<>(0, 3), new WeightedTable.Entry<>(1, 1)));

    /** Freegame sets: set 0 has no insertions; set 1 inserts banners with weighted multipliers. */
    public SpinModeConfig freeGame = new SpinModeConfig(
            List.of(
                    new SpinSetConfig(defaultReels(), List.of(), List.of()),
                    new SpinSetConfig(defaultReels(), List.of(
                    new WeightedTable.Entry<>(2, 1000),
                    new WeightedTable.Entry<>(3, 1000),
                    new WeightedTable.Entry<>(4, 1),
                    new WeightedTable.Entry<>(5, 1),
                    new WeightedTable.Entry<>(6, 1),
                    new WeightedTable.Entry<>(7, 1),
                    new WeightedTable.Entry<>(8, 1),
                    new WeightedTable.Entry<>(9, 1),
                    new WeightedTable.Entry<>(10, 1)), List.of(bannerInsertion()))),
            List.of(new WeightedTable.Entry<>(0, 3), new WeightedTable.Entry<>(1, 1)));

    /** Resolves a spreadsheet reel-symbol ID to its game symbol. */
    public static Symbol fromId(int id) {
        Symbol symbol = SYMBOLS_BY_ID.get(id);
        if (symbol == null) {
            throw new IllegalArgumentException("Unknown expanding-wild symbol ID: " + id);
        }
        return symbol;
    }

    /** Default 60-stop strips contain only the ten regular paying symbols. */
    private static List<List<Integer>> defaultReels() {
        List<Integer> strip = new java.util.ArrayList<>(60);
        for (int repeat = 0; repeat < 6; repeat++) {
            for (int symbolId = 0; symbolId < 10; symbolId++) {
                strip.add(symbolId);
            }
        }
        List<Integer> immutableStrip = List.copyOf(strip);
        return java.util.Collections.nCopies(5, immutableStrip);
    }

    private static SymbolInsertion.Rule scatterInsertion() {
        return new SymbolInsertion.Rule(SCATTER, insertionCountWeights(),
                positionHeatMap(List.of(0, 2, 4)), 1,
                Set.of(SCATTER, BANNER, WILD));
    }

    private static SymbolInsertion.Rule bannerInsertion() {
        return new SymbolInsertion.Rule(BANNER, insertionCountWeights(),
                positionHeatMap(List.of(0, 1, 2, 3, 4)), 1,
                Set.of(SCATTER, BANNER, WILD));
    }

    private static List<WeightedTable.Entry<Integer>> insertionCountWeights() {
        return List.of(new WeightedTable.Entry<>(0, 90),
                new WeightedTable.Entry<>(1, 6),
                new WeightedTable.Entry<>(2, 3),
                new WeightedTable.Entry<>(3, 1));
    }

    /** Position heat maps are reel-major and weight all allowed rows equally by default. */
    private static List<List<Long>> positionHeatMap(List<Integer> eligibleReels) {
        List<List<Long>> weights = new java.util.ArrayList<>(5);
        for (int reel = 0; reel < 5; reel++) {
            long weight = eligibleReels.contains(reel) ? 1L : 0L;
            weights.add(java.util.Collections.nCopies(5, weight));
        }
        return List.copyOf(weights);
    }

    /** One mode's ordered set list and the weights used to select a set on every spin. */
    public static final class SpinModeConfig {
        public List<SpinSetConfig> sets;
        public List<WeightedTable.Entry<Integer>> setSelectionWeights;

        public SpinModeConfig(List<SpinSetConfig> sets,
                List<WeightedTable.Entry<Integer>> setSelectionWeights) {
            this.sets = sets;
            this.setSelectionWeights = setSelectionWeights;
        }
    }

    /** One mode-specific reel set, multiplier weights, and random insertion rules. */
    public static final class SpinSetConfig {
        public List<List<Integer>> reelStrips;
        public List<WeightedTable.Entry<Integer>> bannerMultiplierWeights;
        public List<SymbolInsertion.Rule> symbolInsertions;

        public SpinSetConfig(List<List<Integer>> reelStrips,
                List<WeightedTable.Entry<Integer>> bannerMultiplierWeights) {
            this(reelStrips, bannerMultiplierWeights, List.of());
        }

        public SpinSetConfig(List<List<Integer>> reelStrips,
                List<WeightedTable.Entry<Integer>> bannerMultiplierWeights,
                List<SymbolInsertion.Rule> symbolInsertions) {
            this.reelStrips = reelStrips;
            this.bannerMultiplierWeights = bannerMultiplierWeights;
            this.symbolInsertions = symbolInsertions;
        }
    }

    public List<Payline> paylines = List.of(
            new Payline(2, 2, 2, 2, 2),
            new Payline(0, 0, 0, 0, 0),
            new Payline(4, 4, 4, 4, 4),
            new Payline(1, 1, 1, 1, 1),
            new Payline(3, 3, 3, 3, 3),
            new Payline(0, 1, 2, 3, 4),
            new Payline(4, 3, 2, 1, 0),
            new Payline(1, 0, 1, 2, 1),
            new Payline(3, 4, 3, 2, 3),
            new Payline(2, 1, 0, 1, 2),
            new Payline(2, 3, 4, 3, 2),
            new Payline(0, 0, 1, 2, 3),
            new Payline(4, 4, 3, 2, 1),
            new Payline(1, 2, 3, 2, 1),
            new Payline(3, 2, 1, 2, 3));

    /** Payout values are multipliers of total round stake, applied per winning line. */
    public Paytable paytable = new Paytable(Map.ofEntries(
            payout(0, 5.0, 10.0, 20.0),
            payout(1, 2.0, 3.0, 5.0),
            payout(2, 1.0, 2.0, 3.0),
            payout(3, 0.5, 1.0, 2.0),
            payout(4, 0.2, 0.5, 1.0),
            payout(5, 0.1, 0.2, 0.5),
            payout(6, 0.1, 0.2, 0.5),
            payout(7, 0.1, 0.2, 0.5),
            payout(8, 0.1, 0.2, 0.5),
            payout(9, 0.1, 0.2, 0.5)));

    private static Map.Entry<Symbol, Map<Integer, Double>> payout(
            int symbolId, double three, double four, double five) {
        return Map.entry(fromId(symbolId), Map.of(3, three, 4, four, 5, five));
    }
}
