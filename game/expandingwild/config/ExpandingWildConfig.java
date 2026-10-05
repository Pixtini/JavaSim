package game.expandingwild.config;

import GameModuleFramework.paylines.Payline;
import GameModuleFramework.paylines.Paytable;
import GameModuleFramework.probability.WeightedTable;
import GameModuleFramework.symbols.Symbol;
import java.util.List;
import java.util.Map;

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
    public int freeGamesAwarded = 5;
    /** One row per multiplier: the value followed by its relative draw weight. */
    public List<WeightedTable.Entry<Integer>> bannerMultiplierWeights = List.of(
            new WeightedTable.Entry<>(2, 1000),
            new WeightedTable.Entry<>(3, 1000),
            new WeightedTable.Entry<>(4, 1),
            new WeightedTable.Entry<>(5, 1),
            new WeightedTable.Entry<>(6, 1),
            new WeightedTable.Entry<>(7, 1),
            new WeightedTable.Entry<>(8, 1),
            new WeightedTable.Entry<>(9, 1),
            new WeightedTable.Entry<>(10, 1));
    public final int freeGameTriggerScatterCount = 3;
    public final int[] scatterReels = {0, 2, 4};

    /** Resolves a spreadsheet reel-symbol ID to its game symbol. */
    public static Symbol fromId(int id) {
        Symbol symbol = SYMBOLS_BY_ID.get(id);
        if (symbol == null) {
            throw new IllegalArgumentException("Unknown expanding-wild symbol ID: " + id);
        }
        return symbol;
    }

    /**
     * Reel strips as compact numeric symbol IDs. All reels use the same 60-stop
     * sequence; the scatter stop is retained only on reels 1, 3, and 5.
     */
    public List<List<Integer>> baseReelStrips = List.of(
            List.of(10, 1, 2, 3, 4, 5, 6, 7, 8, 9,
                    0, 1, 2, 3, 4, 5, 6, 7, 8, 9,
                    0, 1, 2, 3, 4, 5, 6, 7, 8, 9,
                    0, 1, 2, 3, 4, 12, 6, 7, 8, 9,
                    0, 1, 2, 3, 4, 5, 6, 7, 8, 9,
                    0, 1, 2, 3, 4, 5, 6, 7, 8, 9),
            List.of(10, 1, 2, 3, 4, 5, 6, 7, 8, 9,
                    0, 1, 2, 3, 4, 5, 6, 7, 8, 9,
                    0, 1, 2, 3, 4, 5, 6, 7, 8, 9,
                    0, 1, 2, 3, 4, 5, 6, 7, 8, 9,
                    0, 1, 2, 3, 4, 5, 6, 7, 8, 9,
                    0, 1, 2, 3, 4, 5, 6, 7, 8, 9),
            List.of(10, 1, 2, 3, 4, 5, 6, 7, 8, 9,
                    0, 1, 2, 3, 4, 5, 6, 7, 8, 9,
                    0, 1, 2, 3, 4, 5, 6, 7, 8, 9,
                    0, 1, 2, 3, 4, 12, 6, 7, 8, 9,
                    0, 1, 2, 3, 4, 5, 6, 7, 8, 9,
                    0, 1, 2, 3, 4, 5, 6, 7, 8, 9),
            List.of(10, 1, 2, 3, 4, 5, 6, 7, 8, 9,
                    0, 1, 2, 3, 4, 5, 6, 7, 8, 9,
                    0, 1, 2, 3, 4, 5, 6, 7, 8, 9,
                    0, 1, 2, 3, 4, 5, 6, 7, 8, 9,
                    0, 1, 2, 3, 4, 5, 6, 7, 8, 9,
                    0, 1, 2, 3, 4, 5, 6, 7, 8, 9),
            List.of(10, 1, 2, 3, 4, 5, 6, 7, 8, 9,
                    0, 1, 2, 3, 4, 5, 6, 7, 8, 9,
                    0, 1, 2, 3, 4, 5, 6, 7, 8, 9,
                    0, 1, 2, 3, 4, 12, 6, 7, 8, 9,
                    0, 1, 2, 3, 4, 5, 6, 7, 8, 9,
                    0, 1, 2, 3, 4, 5, 6, 7, 8, 9));

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
