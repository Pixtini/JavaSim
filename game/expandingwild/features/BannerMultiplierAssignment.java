package game.expandingwild.features;

import GameModuleFramework.probability.WeightedTable;
import GameModuleFramework.reels.WildExpansion;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Random;

/** Assigns one configured multiplier to each expanded reel in deterministic reel order. */
public final class BannerMultiplierAssignment {
    private BannerMultiplierAssignment() {}

    public static Map<Integer, Integer> assign(WildExpansion.Result expansion,
            WeightedTable<Integer> multiplierTable, Random random) {
        List<Integer> expandedReels = expansion.expandedReels().stream()
                .sorted()
                .toList();
        Map<Integer, Integer> multipliers = new LinkedHashMap<>();
        boolean fixedValue = multiplierTable.getEntries().size() == 1;
        for (int reelIndex : expandedReels) {
            int multiplier = fixedValue
                    ? multiplierTable.getEntries().get(0).value()
                    : multiplierTable.draw(random);
            multipliers.put(reelIndex, multiplier);
        }
        return Map.copyOf(multipliers);
    }

    /** Reuses recorded multiplier choices without drawing from the current weight table. */
    public static Map<Integer, Integer> useRecordedChoices(
            WildExpansion.Result expansion, Map<Integer, Integer> recordedChoices) {
        Set<Integer> expectedReels = Set.copyOf(expansion.expandedReels());
        if (!expectedReels.equals(recordedChoices.keySet())) {
            throw new IllegalArgumentException(
                    "Recorded banner multipliers do not match the expanded reels");
        }
        if (recordedChoices.values().stream().anyMatch(value -> value == null || value < 1)) {
            throw new IllegalArgumentException("Recorded banner multipliers must be positive");
        }
        return Map.copyOf(recordedChoices);
    }
}
