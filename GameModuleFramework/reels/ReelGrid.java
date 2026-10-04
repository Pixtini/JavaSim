package GameModuleFramework.reels;

import GameModuleFramework.symbols.Symbol;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;

/** Immutable stopped grid, indexed by reel then row. */
public final class ReelGrid {
    private final List<List<Symbol>> reels;
    private final int height;

    public ReelGrid(List<List<Symbol>> reels) {
        if (reels.isEmpty() || reels.get(0).isEmpty()) {
            throw new IllegalArgumentException("A reel grid must have reels and rows");
        }
        this.height = reels.get(0).size();
        List<List<Symbol>> copiedReels = new ArrayList<>(reels.size());
        for (List<Symbol> reel : reels) {
            if (reel.size() != height) {
                throw new IllegalArgumentException("All reels must have the same visible height");
            }
            reel.forEach(symbol -> Objects.requireNonNull(symbol, "grid symbol"));
            copiedReels.add(List.copyOf(reel));
        }
        this.reels = List.copyOf(copiedReels);
    }

    public static ReelGrid spin(List<ReelStrip> strips, int height, Random random) {
        List<List<Symbol>> stoppedReels = new ArrayList<>(strips.size());
        for (ReelStrip strip : strips) {
            stoppedReels.add(strip.spinWindow(random, height));
        }
        return new ReelGrid(stoppedReels);
    }

    public int getReelCount() {
        return reels.size();
    }

    public int getHeight() {
        return height;
    }

    public Symbol getSymbol(int reel, int row) {
        return reels.get(reel).get(row);
    }

    public boolean reelContains(int reel, Symbol symbol) {
        return reels.get(reel).contains(symbol);
    }

    public List<Symbol> getReel(int reel) {
        return reels.get(reel);
    }
}
