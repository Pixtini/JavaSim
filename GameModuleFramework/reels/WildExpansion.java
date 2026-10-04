package GameModuleFramework.reels;

import GameModuleFramework.symbols.Symbol;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Expands any visible feature symbol into a full-height wild reel. */
public final class WildExpansion {
    private WildExpansion() {
    }

    public static Result expandColumns(ReelGrid grid, Symbol featureSymbol, Symbol wildSymbol) {
        Set<Integer> expandedReels = new LinkedHashSet<>();
        for (int reel = 0; reel < grid.getReelCount(); reel++) {
            if (grid.reelContains(reel, featureSymbol)) {
                expandedReels.add(reel);
            }
        }

        List<List<Symbol>> transformed = new ArrayList<>(grid.getReelCount());
        for (int reel = 0; reel < grid.getReelCount(); reel++) {
            if (expandedReels.contains(reel)) {
                transformed.add(java.util.Collections.nCopies(grid.getHeight(), wildSymbol));
            } else {
                transformed.add(grid.getReel(reel));
            }
        }
        return new Result(new ReelGrid(transformed), expandedReels);
    }

    public record Result(ReelGrid grid, Set<Integer> expandedReels) {
        public Result {
            expandedReels = Set.copyOf(expandedReels);
        }
    }
}
