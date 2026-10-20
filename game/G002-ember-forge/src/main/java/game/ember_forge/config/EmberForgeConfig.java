package game.ember_forge.config;

import GameModuleFramework.paylines.Payline;
import GameModuleFramework.paylines.Paytable;
import GameModuleFramework.reels.ReelStrip;
import GameModuleFramework.symbols.Symbol;
import java.nio.file.Path;
import java.util.*;

/** Immutable, mode-isolated inputs for the small Ember Forge prototype. */
public record EmberForgeConfig(String mode, double costMultiplier, int triggerDenominator,
        int freeSpins, int maxHeat, double maxWinMultiplier,
        List<List<Double>> payouts, List<List<Integer>> lineRows,
        List<List<Integer>> baseReels, List<List<Integer>> entryReels,
        List<List<Integer>> freeReels) {
    public static final String ID = "G002-ember-forge";
    public static final Path DEFAULT_PAR = Path.of("game", ID, "source", "PAR.xlsx");
    public static final List<String> NAMES = List.of(
            "COAL", "COPPER", "IRON", "SILVER", "GOLD", "EMBER", "WILD", "SCATTER");
    public static final Symbol WILD = symbol(7);
    public static final Symbol SCATTER = symbol(8);

    public EmberForgeConfig {
        if (!List.of("Normal", "EM1", "BB1").contains(mode))
            throw new IllegalArgumentException("G002 mode must be Normal, EM1 or BB1");
        if (!Double.isFinite(costMultiplier) || costMultiplier <= 0
                || (mode.equals("Normal") && costMultiplier != 1)
                || (mode.equals("EM1") && costMultiplier != 2))
            throw new IllegalArgumentException("Invalid G002 purchase cost");
        if (triggerDenominator <= 4 || freeSpins != 8 || maxHeat != 5
                || !Double.isFinite(maxWinMultiplier) || maxWinMultiplier <= 0)
            throw new IllegalArgumentException("G002 requires denominator >4, eight spins and maximum heat five");
        payouts = copy(payouts); lineRows = copy(lineRows);
        baseReels = copy(baseReels); entryReels = copy(entryReels); freeReels = copy(freeReels);
        if (payouts.size() != 6 || lineRows.size() != 10)
            throw new IllegalArgumentException("G002 requires six paying regulars and ten lines");
        double highest = 0;
        for (var row : payouts) {
            if (row.size() != 3) throw new IllegalArgumentException("Define 3/4/5oak awards");
            double previous = 0;
            for (double value : row) {
                if (!Double.isFinite(value) || value <= 0 || value < previous
                        || Math.abs(value * 20 - Math.rint(value * 20)) > 1e-8)
                    throw new IllegalArgumentException("Paytable must be nondecreasing positive multiples of 0.05");
                previous = value; highest = Math.max(highest, value);
            }
        }
        for (var line : lineRows)
            if (line.size() != 5 || line.stream().anyMatch(r -> r < 0 || r > 2))
                throw new IllegalArgumentException("Paylines must contain five rows in 0..2");
        if (new HashSet<>(lineRows).size() != lineRows.size())
            throw new IllegalArgumentException("Duplicate G002 paylines");
        checkReels(baseReels, false); checkReels(freeReels, false); checkReels(entryReels, true);
        for (int reel = 0; reel < 5; reel++) {
            var strip = entryReels.get(reel);
            int regular = reel + 1;
            boolean scatterReel = reel % 2 == 0;
            if (strip.size() != 3 || strip.stream().anyMatch(id -> id != regular && id != 8)
                    || Collections.frequency(strip, 8) != (scatterReel ? 1 : 0))
                throw new IllegalArgumentException("Entry set needs distinct regular per reel and scatters on reels 1/3/5");
        }
        // Every FG is bounded by ten highest-paying lines and heat sum 1+2+3+4+5+5+5+5.
        if (maxWinMultiplier + 1e-8 < highest * lineRows.size() * 30)
            throw new IllegalArgumentException("G002 cap must cover its exact-analysis maximum bound");
    }

    private static <T> List<List<T>> copy(List<List<T>> values) {
        return values.stream().map(List::copyOf).toList();
    }
    private static void checkReels(List<List<Integer>> reels, boolean entry) {
        if (reels.size() != 5) throw new IllegalArgumentException("G002 needs five reels per set");
        for (var strip : reels)
            if (strip.isEmpty() || strip.stream().anyMatch(id -> id < 1 || id > (entry ? 8 : 7)))
                throw new IllegalArgumentException("Invalid strip or natural scatter outside entry set");
    }
    public static Symbol symbol(int id) {
        if (id < 1 || id > NAMES.size()) throw new IllegalArgumentException("Unknown G002 symbol ID " + id);
        return new Symbol(NAMES.get(id - 1));
    }
    public static int symbolId(Symbol symbol) {
        int index = NAMES.indexOf(symbol.id());
        if (index < 0) throw new IllegalArgumentException("Unknown G002 symbol " + symbol);
        return index + 1;
    }
    public int triggerWeight() {
        return mode.equals("BB1") ? triggerDenominator : mode.equals("EM1") ? 4 : 1;
    }
    public double triggerProbability() { return (double) triggerWeight() / triggerDenominator; }
    public List<Payline> paylines() {
        return lineRows.stream().map(rows -> new Payline(rows.stream().mapToInt(i -> i).toArray())).toList();
    }
    public Paytable paytable() {
        Map<Symbol, Map<Integer, Double>> values = new LinkedHashMap<>();
        for (int id = 1; id <= 6; id++) {
            var row = payouts.get(id - 1);
            values.put(symbol(id), Map.of(3, row.get(0), 4, row.get(1), 5, row.get(2)));
        }
        return new Paytable(values);
    }
    public List<ReelStrip> strips(List<List<Integer>> values) {
        return values.stream().map(reel -> new ReelStrip(reel.stream().map(EmberForgeConfig::symbol).toList())).toList();
    }
}
