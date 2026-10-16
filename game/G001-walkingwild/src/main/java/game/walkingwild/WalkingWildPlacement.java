package game.walkingwild;

import GameModuleFramework.reels.ReelGrid;
import GameModuleFramework.symbols.Symbol;
import game.walkingwild.config.WalkingWildConfig;
import game.walkingwild.model.WildPosition;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/** Game-specific weighted wild placement with occupied-cell weights zeroed per sequence. */
public final class WalkingWildPlacement {
    private WalkingWildPlacement() {}

    public static Result place(ReelGrid source, WalkingWildConfig.HeatMap heatMap,
            List<WildPosition> initiallyOccupied, int count, Random random) {
        if (count < 0 || initiallyOccupied.size() + count > 5) {
            throw new IllegalArgumentException("G001 Freegame placement cannot exceed five active wilds");
        }
        if (heatMap.weights().size() != source.getReelCount()
                || heatMap.weights().stream().anyMatch(rows -> rows.size() != source.getHeight())) {
            throw new IllegalArgumentException("Wild heat map does not match the visible screen");
        }
        List<List<Symbol>> reels = new ArrayList<>(source.getReelCount());
        for (int reel = 0; reel < source.getReelCount(); reel++) {
            reels.add(new ArrayList<>(source.getReel(reel)));
        }
        long[][] temporaryWeights = new long[source.getReelCount()][source.getHeight()];
        Set<WildPosition> occupied = new HashSet<>();
        for (int reel = 0; reel < source.getReelCount(); reel++) {
            for (int row = 0; row < source.getHeight(); row++) {
                temporaryWeights[reel][row] = heatMap.weights().get(reel).get(row);
            }
        }
        for (WildPosition position : initiallyOccupied) {
            validatePosition(position, source);
            if (!occupied.add(position)) throw new IllegalArgumentException("Wild positions cannot overlap");
            if (!source.getSymbol(position.reel(), position.row()).equals(WalkingWildConfig.WILD)) {
                throw new IllegalArgumentException("Initially occupied position does not contain a wild");
            }
            temporaryWeights[position.reel()][position.row()] = 0;
        }

        List<WildPosition> placed = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            long totalWeight = 0;
            for (long[] reelWeights : temporaryWeights) {
                for (long weight : reelWeights) totalWeight = Math.addExact(totalWeight, weight);
            }
            if (totalWeight <= 0) {
                throw new IllegalStateException("G001 wild heat map has no eligible unoccupied positions");
            }
            long ticket = totalWeight <= Integer.MAX_VALUE
                    ? random.nextInt((int) totalWeight) : random.nextLong(totalWeight);
            WildPosition selected = null;
            for (int reel = 0; reel < source.getReelCount() && selected == null; reel++) {
                for (int row = 0; row < source.getHeight(); row++) {
                    long weight = temporaryWeights[reel][row];
                    if (ticket < weight) {
                        selected = new WildPosition(reel, row);
                        break;
                    }
                    ticket -= weight;
                }
            }
            if (selected == null) throw new IllegalStateException("Wild location draw exceeded its weight range");
            temporaryWeights[selected.reel()][selected.row()] = 0;
            reels.get(selected.reel()).set(selected.row(), WalkingWildConfig.WILD);
            occupied.add(selected);
            placed.add(selected);
        }
        return new Result(new ReelGrid(reels), List.copyOf(placed));
    }

    /** Applies recorded wild destinations while rechecking heat-map eligibility and collisions. */
    public static Result placeRecorded(ReelGrid source, WalkingWildConfig.HeatMap heatMap,
            List<WildPosition> initiallyOccupied, List<WildPosition> destinations) {
        if (initiallyOccupied.size() + destinations.size() > 5) {
            throw new IllegalArgumentException("G001 Freegame replay exceeds five active wilds");
        }
        if (heatMap.weights().size() != source.getReelCount()
                || heatMap.weights().stream().anyMatch(rows -> rows.size() != source.getHeight())) {
            throw new IllegalArgumentException("Wild heat map does not match the visible screen");
        }
        List<List<Symbol>> reels = new ArrayList<>(source.getReelCount());
        for (int reel = 0; reel < source.getReelCount(); reel++) {
            reels.add(new ArrayList<>(source.getReel(reel)));
        }
        Set<WildPosition> occupied = new HashSet<>();
        for (WildPosition position : initiallyOccupied) {
            validatePosition(position, source);
            if (!occupied.add(position)
                    || !source.getSymbol(position.reel(), position.row()).equals(WalkingWildConfig.WILD)) {
                throw new IllegalArgumentException("Recorded G001 occupied position is invalid");
            }
        }
        List<WildPosition> applied = new ArrayList<>();
        for (WildPosition position : destinations) {
            validatePosition(position, source);
            if (heatMap.weights().get(position.reel()).get(position.row()) <= 0
                    || !occupied.add(position)) {
                throw new IllegalArgumentException("Recorded G001 wild destination is ineligible");
            }
            reels.get(position.reel()).set(position.row(), WalkingWildConfig.WILD);
            applied.add(position);
        }
        return new Result(new ReelGrid(reels), List.copyOf(applied));
    }

    private static void validatePosition(WildPosition position, ReelGrid grid) {
        if (position.reel() >= grid.getReelCount() || position.row() >= grid.getHeight()) {
            throw new IllegalArgumentException("Wild position is outside the visible screen");
        }
    }

    public record Result(ReelGrid grid, List<WildPosition> placements) {
        public Result { placements = List.copyOf(placements); }
    }
}
