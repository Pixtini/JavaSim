package game.walkingwild;

import game.Game;
import game.ExhaustiveReelGame;
import game.GameSession;
import game.model.SpinResult;
import game.walkingwild.config.WalkingWildConfig;
import GameModuleFramework.reels.ReelGrid;
import GameModuleFramework.reels.ReelStrip;
import java.util.Random;
import java.util.List;

/** G001 game module, configured from one selected mode in its PAR workbook. */
public final class WalkingWildGame implements ExhaustiveReelGame {
    private final WalkingWildConfig config;

    public WalkingWildGame(WalkingWildConfig config) {
        this.config = config;
    }

    @Override
    public GameSession createSession(Random random) {
        return new WalkingWildGameSession(config, random);
    }

    @Override public int[] getPaytable() { return new int[] {0, 0}; }
    @Override public int getBaseGameSetCount() { return config.baseSets().size(); }
    @Override public int getFreeGameSetCount() { return config.freeSets().size(); }
    @Override public double getMaxWinMultiplier() { return config.maxWinMultiplier(); }
    @Override public double getCostMultiplier() { return config.costMultiplier(); }
    public WalkingWildConfig getConfig() { return config; }

    @Override public int getReelCount() { return config.reelCount(); }

    @Override public int getStopCount(int reelIndex) {
        return getStopCount(0, reelIndex);
    }

    @Override public int getReelSetCount() { return config.baseSets().size(); }

    @Override public int getStopCount(int setIndex, int reelIndex) {
        return config.baseSets().get(setIndex).reelStrips().get(reelIndex).size();
    }

    @Override public long getReelSetWeight(int setIndex) {
        return config.baseSetWeights().stream()
                .filter(entry -> entry.value() == setIndex)
                .mapToLong(GameModuleFramework.probability.WeightedTable.Entry::weight)
                .findFirst().orElse(0L);
    }

    @Override public StopEvaluator createStopEvaluator() { return createStopEvaluator(0); }

    @Override
    public StopEvaluator createStopEvaluator(int setIndex) {
        var set = config.baseSets().get(setIndex);
        List<ReelStrip> strips = set.reelStrips().stream()
                .map(reel -> new ReelStrip(reel.stream().map(WalkingWildConfig::symbol).toList()))
                .toList();
        WalkingWildWinEvaluator evaluator = new WalkingWildWinEvaluator(config);
        return (stops, stake) -> {
            ReelGrid grid = ReelGrid.atStops(strips, config.visibleRows(), stops);
            double win = evaluator.calculateWin(grid, stake);
            return new SpinResult(win, win > 0.0 ? 1 : 0, false, List.of(), setIndex);
        };
    }
}
