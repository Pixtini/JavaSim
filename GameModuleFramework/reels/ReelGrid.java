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
        return spinWithStops(strips, height, random).grid();
    }

    /** Spins every reel and also returns the stop indices consumed from the RNG. */
    public static SpinOutcome spinWithStops(List<ReelStrip> strips, int height, Random random) {
        List<List<Symbol>> stoppedReels = new ArrayList<>(strips.size());
        int[] stops = new int[strips.size()];
        for (int reel = 0; reel < strips.size(); reel++) {
            ReelStrip strip = strips.get(reel);
            stops[reel] = random.nextInt(strip.getSymbols().size());
            stoppedReels.add(strip.windowAt(stops[reel], height));
        }
        return new SpinOutcome(new ReelGrid(stoppedReels), stops);
    }

    /** Builds a grid from an exact zero-based stop index for each reel. */
    public static ReelGrid atStops(List<ReelStrip> strips, int height, int[] stops) {
        if (strips.size() != stops.length) {
            throw new IllegalArgumentException("One stop index is required for each reel");
        }
        List<List<Symbol>> stoppedReels = new ArrayList<>(strips.size());
        for (int reel = 0; reel < strips.size(); reel++) {
            stoppedReels.add(strips.get(reel).windowAt(stops[reel], height));
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

    public record SpinOutcome(ReelGrid grid, int[] stops) {
        public SpinOutcome {
            stops = stops.clone();
        }

        @Override
        public int[] stops() {
            return stops.clone();
        }
    }
}
