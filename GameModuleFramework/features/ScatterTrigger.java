package GameModuleFramework.features;

import GameModuleFramework.reels.ReelGrid;
import GameModuleFramework.symbols.Symbol;
import java.util.Arrays;
import java.util.Objects;

/** Counts a configured scatter symbol and reports whether its threshold is met. */
public final class ScatterTrigger {
    private final Symbol scatterSymbol;
    private final int requiredCount;
    private final int[] reelIndexes;

    public ScatterTrigger(Symbol scatterSymbol, int requiredCount, int... reelIndexes) {
        this.scatterSymbol = Objects.requireNonNull(scatterSymbol, "scatterSymbol");
        if (requiredCount <= 0) {
            throw new IllegalArgumentException("Required scatter count must be positive");
        }
        if (reelIndexes.length == 0) {
            throw new IllegalArgumentException("At least one scatter reel must be configured");
        }
        if (Arrays.stream(reelIndexes).anyMatch(index -> index < 0)) {
            throw new IllegalArgumentException("Scatter reel indexes cannot be negative");
        }
        if (Arrays.stream(reelIndexes).distinct().count() != reelIndexes.length) {
            throw new IllegalArgumentException("Scatter reel indexes must be unique");
        }
        this.requiredCount = requiredCount;
        this.reelIndexes = reelIndexes.clone();
    }

    /** Counts eligible scatters up to the trigger threshold and reports trigger state. */
    public Result evaluate(ReelGrid grid) {
        Objects.requireNonNull(grid, "grid");
        for (int reelIndex : reelIndexes) {
            if (reelIndex >= grid.getReelCount()) {
                throw new IllegalArgumentException(
                        "Scatter reel index " + reelIndex + " is outside the grid");
            }
        }
        int count = ScatterCounter.count(grid, scatterSymbol, requiredCount, reelIndexes);
        return new Result(count, count >= requiredCount);
    }

    public record Result(int scatterCount, boolean triggered) {
    }
}
