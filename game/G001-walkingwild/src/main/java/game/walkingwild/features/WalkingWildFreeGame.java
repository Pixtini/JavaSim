package game.walkingwild.features;

import GameModuleFramework.probability.WeightedTable;
import GameModuleFramework.reels.ReelGrid;
import GameModuleFramework.reels.ReelStrip;
import game.model.SpinResult;
import game.walkingwild.WalkingWildPlacement;
import game.walkingwild.WalkingWildWinEvaluator;
import game.walkingwild.config.WalkingWildConfig;
import game.walkingwild.model.WalkingWildSpin;
import game.walkingwild.model.WalkingWildReplayPayload;
import game.walkingwild.model.WildPosition;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Runs one Freegame spin, landing new wilds and relocating persisted wilds. */
public final class WalkingWildFreeGame {
    private final WalkingWildConfig config;
    private final Random random;
    private final WeightedTable<Integer> setSelector;
    private final List<List<ReelStrip>> reelsBySet;
    private final WalkingWildWinEvaluator winEvaluator;

    public WalkingWildFreeGame(WalkingWildConfig config, Random random) {
        this.config = config;
        this.random = random;
        setSelector = new WeightedTable<>(config.freeSetWeights());
        reelsBySet = config.freeSets().stream().map(set -> toReelStrips(set.reelStrips())).toList();
        winEvaluator = new WalkingWildWinEvaluator(config);
    }

    public WalkingWildSpin spin(double baseStakeUnit, List<WildPosition> persistedWilds) {
        return spin(baseStakeUnit, persistedWilds, false);
    }

    public WalkingWildSpin spin(double baseStakeUnit, List<WildPosition> persistedWilds,
            boolean captureReplay) {
        if (persistedWilds.size() > 5) throw new IllegalArgumentException("At most five wilds may persist");
        int setIndex = setSelector.draw(random);
        var set = config.freeSets().get(setIndex);
        List<WeightedTable.Entry<Integer>> addedCountWeights = set.addedWildCounts().get(persistedWilds.size());
        if (addedCountWeights == null) {
            throw new IllegalStateException("PAR has no Freegame wild count table for " + persistedWilds.size()
                    + " persisted wilds");
        }
        int newWildCount = new WeightedTable<>(addedCountWeights).draw(random);
        if (persistedWilds.size() + newWildCount > 5) {
            throw new IllegalStateException("PAR requested more than five active Freegame wilds");
        }

        ReelGrid.SpinOutcome stopped = ReelGrid.spinWithStops(
                reelsBySet.get(setIndex), config.visibleRows(), random);
        WalkingWildPlacement.Result landed = WalkingWildPlacement.place(
                stopped.grid(), set.wildLocations(), List.of(), newWildCount, random);
        WalkingWildPlacement.Result moved = WalkingWildPlacement.place(
                landed.grid(), set.wildLocations(), landed.placements(), persistedWilds.size(), random);
        List<WildPosition> nextWilds = new ArrayList<>(landed.placements());
        nextWilds.addAll(moved.placements());
        var lineWins = winEvaluator.calculateLineWins(moved.grid(), baseStakeUnit);
        double win = lineWins.stream().mapToDouble(game.walkingwild.model.WalkingWildLineWin::totalWin)
                .sum();
        WalkingWildReplayPayload replay = captureReplay
                ? new WalkingWildReplayPayload(setIndex, 0, stopped.stops(), List.of(),
                        landed.placements(), moved.placements())
                : null;
        return new WalkingWildSpin(win, false, setIndex, moved.grid(), 0, nextWilds,
                lineWins, captureReplay ? "walkingwild-free" : null, replay);
    }

    public WalkingWildSpin replay(WalkingWildReplayPayload replay, double baseStakeUnit,
            List<WildPosition> persistedWilds) {
        if (replay.scatterCount() != 0 || !replay.scatterInsertions().isEmpty()) {
            throw new IllegalArgumentException("Freegame replay cannot contain scatter insertions");
        }
        int setIndex = replay.setIndex();
        if (setIndex < 0 || setIndex >= config.freeSets().size()) {
            throw new IllegalArgumentException("G001 replay references an unknown Freegame set");
        }
        var set = config.freeSets().get(setIndex);
        List<WeightedTable.Entry<Integer>> counts = set.addedWildCounts().get(persistedWilds.size());
        if (counts == null || counts.stream().noneMatch(entry ->
                entry.value() == replay.newWildPositions().size())) {
            throw new IllegalArgumentException("Recorded G001 new-wild count is not configured");
        }
        if (replay.movedWildPositions().size() != persistedWilds.size()
                || persistedWilds.size() + replay.newWildPositions().size() > 5) {
            throw new IllegalArgumentException("Recorded G001 wild movement count is invalid");
        }
        ReelGrid stopped = ReelGrid.atStops(reelsBySet.get(setIndex), config.visibleRows(),
                replay.reelStops());
        WalkingWildPlacement.Result landed = WalkingWildPlacement.placeRecorded(
                stopped, set.wildLocations(), List.of(), replay.newWildPositions());
        WalkingWildPlacement.Result moved = WalkingWildPlacement.placeRecorded(
                landed.grid(), set.wildLocations(), landed.placements(), replay.movedWildPositions());
        List<WildPosition> nextWilds = new ArrayList<>(landed.placements());
        nextWilds.addAll(moved.placements());
        var lineWins = winEvaluator.calculateLineWins(moved.grid(), baseStakeUnit);
        double win = lineWins.stream().mapToDouble(
                game.walkingwild.model.WalkingWildLineWin::totalWin).sum();
        return new WalkingWildSpin(win, false, setIndex, moved.grid(), 0, nextWilds,
                lineWins, "walkingwild-free", replay);
    }

    private static List<ReelStrip> toReelStrips(List<List<Integer>> idsByReel) {
        List<ReelStrip> strips = new ArrayList<>(idsByReel.size());
        for (List<Integer> ids : idsByReel) {
            strips.add(new ReelStrip(ids.stream().map(WalkingWildConfig::symbol).toList()));
        }
        return List.copyOf(strips);
    }
}
