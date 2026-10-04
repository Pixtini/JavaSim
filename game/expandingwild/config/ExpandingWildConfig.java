package game.expandingwild.config;

import GameModuleFramework.paylines.Payline;
import GameModuleFramework.paylines.Paytable;
import GameModuleFramework.probability.WeightedTable;
import GameModuleFramework.reels.ReelStrip;
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
    public int freeGamesAwarded = 8;
    /** One row per multiplier: the value followed by its relative draw weight. */
    public List<WeightedTable.Entry<Integer>> bannerMultiplierWeights = List.of(
            new WeightedTable.Entry<>(2, 1),
            new WeightedTable.Entry<>(3, 1),
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

    /** Reel strips as compact numeric symbol IDs, suitable for spreadsheet editing. */
    public List<List<Integer>> baseReelStrips = List.of(
            List.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 2, 3, 4, 5, 12),
            List.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 2, 3, 4, 5, 6),
            List.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 3, 4, 5, 6, 12),
            List.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 4, 5, 6, 7, 8),
            List.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 6, 7, 8, 9, 12));

    /** Converts the spreadsheet-friendly ID strips into the common symbol-based reel type. */
    public List<ReelStrip> getBaseReelStrips() {
        return baseReelStrips.stream()
                .map(ids -> new ReelStrip(ids.stream().map(ExpandingWildConfig::fromId).toList()))
                .toList();
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

    public Paytable paytable = new Paytable(Map.ofEntries(
            payout(0, 2.0, 5.0, 12.0),
            payout(1, 2.0, 6.0, 15.0),
            payout(2, 2.5, 7.0, 18.0),
            payout(3, 3.0, 8.0, 20.0),
            payout(4, 3.0, 9.0, 24.0),
            payout(5, 3.5, 10.0, 28.0),
            payout(6, 4.0, 12.0, 32.0),
            payout(7, 4.0, 14.0, 36.0),
            payout(8, 5.0, 16.0, 40.0),
            payout(9, 5.0, 18.0, 45.0)));

    private static Map.Entry<Symbol, Map<Integer, Double>> payout(
            int symbolId, double three, double four, double five) {
        return Map.entry(fromId(symbolId), Map.of(3, three, 4, four, 5, five));
    }
}
