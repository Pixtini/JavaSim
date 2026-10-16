package game.walkingwild.config;

import GameModuleFramework.paylines.Payline;
import GameModuleFramework.paylines.Paytable;
import GameModuleFramework.probability.WeightedTable;
import GameModuleFramework.symbols.Symbol;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Immutable mathematical configuration parsed from one G001 PAR mode. */
public record WalkingWildConfig(
        String mode,
        int reelCount,
        int visibleRows,
        double maxWinMultiplier,
        double costMultiplier,
        Paytable paytable,
        List<Payline> paylines,
        Map<Integer, Integer> scatterFreeGames,
        List<WeightedTable.Entry<Integer>> baseSetWeights,
        List<BaseSet> baseSets,
        List<WeightedTable.Entry<Integer>> freeSetWeights,
        List<FreeSet> freeSets) {

    public static final Symbol WILD = new Symbol("WILD");
    public static final Symbol SCATTER = new Symbol("SCATTER");
    private static final List<Symbol> SYMBOLS = List.of(
            WILD, new Symbol("T1"), new Symbol("T2"), new Symbol("T3"),
            new Symbol("T4"), new Symbol("L1"), new Symbol("L2"),
            new Symbol("L3"), new Symbol("L4"), SCATTER);

    public WalkingWildConfig {
        if (mode == null || mode.isBlank() || reelCount != 6 || visibleRows != 4) {
            throw new IllegalArgumentException("G001 requires a mode and a 6x4 screen");
        }
        if (!Double.isFinite(maxWinMultiplier) || maxWinMultiplier <= 0.0
                || !Double.isFinite(costMultiplier) || costMultiplier <= 0.0) {
            throw new IllegalArgumentException("G001 max win and cost must be positive and finite");
        }
        paylines = List.copyOf(paylines);
        scatterFreeGames = Map.copyOf(scatterFreeGames);
        baseSetWeights = List.copyOf(baseSetWeights);
        baseSets = List.copyOf(baseSets);
        freeSetWeights = List.copyOf(freeSetWeights);
        freeSets = List.copyOf(freeSets);
        validateSets(baseSetWeights, baseSets.stream().map(BaseSet::index).collect(java.util.stream.Collectors.toSet()));
        validateSets(freeSetWeights, freeSets.stream().map(FreeSet::index).collect(java.util.stream.Collectors.toSet()));
        if (paylines.isEmpty() || scatterFreeGames.isEmpty()) {
            throw new IllegalArgumentException("G001 requires paylines and scatter awards");
        }
        for (Payline payline : paylines) {
            if (payline.getReelCount() != reelCount) {
                throw new IllegalArgumentException("Every G001 payline must span all six reels");
            }
            for (int row : payline.getRowsByReel()) {
                if (row >= visibleRows) throw new IllegalArgumentException("Payline row is outside the screen");
            }
        }
    }

    public static Symbol symbol(int id) {
        if (id < 0 || id >= SYMBOLS.size()) {
            throw new IllegalArgumentException("Unknown G001 symbol ID: " + id);
        }
        return SYMBOLS.get(id);
    }

    public static int symbolId(String name) {
        for (int id = 0; id < SYMBOLS.size(); id++) {
            if (SYMBOLS.get(id).id().equalsIgnoreCase(name)) return id;
        }
        throw new IllegalArgumentException("Unknown G001 symbol: " + name);
    }

    private static void validateSets(List<WeightedTable.Entry<Integer>> weights, Set<Integer> ids) {
        new WeightedTable<>(weights);
        if (ids.isEmpty()) throw new IllegalArgumentException("G001 must define at least one reel set");
        for (WeightedTable.Entry<Integer> entry : weights) {
            if (!ids.contains(entry.value())) {
                throw new IllegalArgumentException("Set selector references missing set " + entry.value());
            }
        }
    }

    public record BaseSet(int index, List<List<Integer>> reelStrips,
            List<WeightedTable.Entry<Integer>> scatterCounts, HeatMap scatterLocations) {
        public BaseSet {
            reelStrips = copyReels(reelStrips);
            scatterCounts = List.copyOf(scatterCounts);
            new WeightedTable<>(scatterCounts);
            if (scatterLocations.maxPerReel() != 1) {
                throw new IllegalArgumentException("G001 scatter placement is limited to one per reel");
            }
        }
    }

    public record FreeSet(int index, List<List<Integer>> reelStrips,
            Map<Integer, List<WeightedTable.Entry<Integer>>> addedWildCounts,
            HeatMap wildLocations) {
        public FreeSet {
            reelStrips = copyReels(reelStrips);
            addedWildCounts = Map.copyOf(addedWildCounts);
            addedWildCounts.forEach((present, entries) -> {
                new WeightedTable<>(entries);
                int maximumAdded = entries.stream().mapToInt(WeightedTable.Entry::value).max().orElse(0);
                if (present + maximumAdded > 5) {
                    throw new IllegalArgumentException("G001 PAR can request more than five active wilds");
                }
            });
            if (wildLocations.maxPerReel() != Integer.MAX_VALUE) {
                throw new IllegalArgumentException("G001 Freegame wild heat map cannot limit reel placements");
            }
        }
    }

    public record HeatMap(List<List<Long>> weights, int maxPerReel) {
        public HeatMap {
            weights = weights.stream().map(List::copyOf).toList();
            if (weights.size() != 6 || weights.stream().anyMatch(row -> row.size() != 4)
                    || weights.stream().flatMap(List::stream).anyMatch(weight -> weight < 0)
                    || weights.stream().flatMap(List::stream).noneMatch(weight -> weight > 0)
                    || maxPerReel <= 0) {
                throw new IllegalArgumentException("G001 heat map must define positive weights for a 6x4 grid");
            }
        }
    }

    private static List<List<Integer>> copyReels(List<List<Integer>> reels) {
        List<List<Integer>> copied = reels.stream().map(List::copyOf).toList();
        if (copied.size() != 6 || copied.stream().anyMatch(List::isEmpty)) {
            throw new IllegalArgumentException("G001 reel sets must define six non-empty strips");
        }
        return copied;
    }
}
