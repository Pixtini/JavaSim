package GameModuleFramework.features;

import GameModuleFramework.reels.ReelGrid;
import GameModuleFramework.symbols.Symbol;

/** Counts scatter symbols on a configured set of reels, with an optional cap. */
public final class ScatterCounter {
    private ScatterCounter() {
    }

    public static int count(ReelGrid grid, Symbol scatter, int maximum, int... reelIndexes) {
        if (maximum < 0) {
            throw new IllegalArgumentException("Maximum scatter count cannot be negative");
        }
        int count = 0;
        for (int reel : reelIndexes) {
            for (int row = 0; row < grid.getHeight(); row++) {
                if (grid.getSymbol(reel, row).equals(scatter) && ++count >= maximum) {
                    return maximum;
                }
            }
        }
        return count;
    }
}
