package game.walkingwild.basegame;

import GameModuleFramework.features.ScatterCounter;
import GameModuleFramework.features.SymbolInsertion;
import GameModuleFramework.probability.WeightedTable;
import GameModuleFramework.reels.ReelGrid;
import GameModuleFramework.reels.ReelStrip;
import GameModuleFramework.symbols.Symbol;
import game.model.SpinResult;
import game.walkingwild.WalkingWildWinEvaluator;
import game.walkingwild.config.WalkingWildConfig;
import game.walkingwild.model.WalkingWildReplayPayload;
import game.walkingwild.model.WalkingWildSpin;
import game.walkingwild.model.WildPosition;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

/** Runs set selection, reels, configured scatter insertion, and line wins for one Basegame spin. */
public final class WalkingWildBaseGame {
    private static final Set<Symbol> PROTECTED_SYMBOLS = Set.of(
            WalkingWildConfig.WILD, WalkingWildConfig.SCATTER);
    private final WalkingWildConfig config;
    private final Random random;
    private final WeightedTable<Integer> setSelector;
    private final List<List<ReelStrip>> reelsBySet;
    private final List<SymbolInsertion.Rule> scatterRules;
    private final WalkingWildWinEvaluator winEvaluator;

    public WalkingWildBaseGame(WalkingWildConfig config, Random random) {
        this.config = config;
        this.random = random;
        this.setSelector = new WeightedTable<>(config.baseSetWeights());
        this.reelsBySet = config.baseSets().stream().map(set -> toReelStrips(set.reelStrips())).toList();
        this.scatterRules = config.baseSets().stream().map(set -> new SymbolInsertion.Rule(
                WalkingWildConfig.SCATTER, set.scatterCounts(), set.scatterLocations().weights(),
                set.scatterLocations().maxPerReel(), PROTECTED_SYMBOLS)).toList();
        this.winEvaluator = new WalkingWildWinEvaluator(config);
    }

    public WalkingWildSpin spin(double baseStakeUnit) {
        return spin(baseStakeUnit, false);
    }

    public WalkingWildSpin spin(double baseStakeUnit, boolean captureReplay) {
        int setIndex = setSelector.draw(random);
        ReelGrid.SpinOutcome stopped = ReelGrid.spinWithStops(
                reelsBySet.get(setIndex), config.visibleRows(), random);
        SymbolInsertion.Result insertion = SymbolInsertion.apply(
                stopped.grid(), List.of(scatterRules.get(setIndex)), random);
        ReelGrid screen = insertion.grid();
        int[] reelIndexes = new int[config.reelCount()];
        for (int reel = 0; reel < reelIndexes.length; reel++) reelIndexes[reel] = reel;
        int scatterCount = ScatterCounter.count(screen, WalkingWildConfig.SCATTER,
                config.reelCount() * config.visibleRows(), reelIndexes);
        int freeGames = freeGamesFor(scatterCount);
        var lineWins = winEvaluator.calculateLineWins(screen, baseStakeUnit);
        double win = lineWins.stream().mapToDouble(game.walkingwild.model.WalkingWildLineWin::totalWin)
                .sum();
        WalkingWildReplayPayload replay = captureReplay
                ? new WalkingWildReplayPayload(setIndex, scatterCount, stopped.stops(),
                        insertion.placements().stream().map(placement ->
                                new WildPosition(placement.reel(), placement.row())).toList(),
                        List.of(), List.of())
                : null;
        return new WalkingWildSpin(win, freeGames > 0, setIndex, screen, scatterCount,
                List.of(), lineWins, captureReplay ? "walkingwild-base" : null, replay);
    }

    public WalkingWildSpin replay(WalkingWildReplayPayload replay, double baseStakeUnit) {
        if (!replay.newWildPositions().isEmpty() || !replay.movedWildPositions().isEmpty()) {
            throw new IllegalArgumentException("Basegame replay cannot contain Freegame wild placements");
        }
        int setIndex = replay.setIndex();
        if (setIndex < 0 || setIndex >= config.baseSets().size()) {
            throw new IllegalArgumentException("G001 replay references an unknown Basegame set");
        }
        ReelGrid stopped = ReelGrid.atStops(reelsBySet.get(setIndex), config.visibleRows(),
                replay.reelStops());
        List<SymbolInsertion.Placement> insertions = replay.scatterInsertions().stream()
                .map(position -> new SymbolInsertion.Placement(WalkingWildConfig.SCATTER,
                        position.reel(), position.row())).toList();
        ReelGrid screen = SymbolInsertion.applyRecorded(stopped,
                List.of(scatterRules.get(setIndex)), insertions).grid();
        int scatterCount = scatterCount(screen);
        if (scatterCount != replay.scatterCount()) {
            throw new IllegalArgumentException("G001 replay scatter count does not match its insertions");
        }
        List<game.walkingwild.model.WalkingWildLineWin> lineWins = winEvaluator
                .calculateLineWins(screen, baseStakeUnit);
        double win = lineWins.stream().mapToDouble(
                game.walkingwild.model.WalkingWildLineWin::totalWin).sum();
        return new WalkingWildSpin(win, freeGamesFor(scatterCount) > 0, setIndex,
                screen, scatterCount, List.of(), lineWins, "walkingwild-base", replay);
    }

    private int scatterCount(ReelGrid screen) {
        int[] reels = new int[config.reelCount()];
        for (int reel = 0; reel < reels.length; reel++) reels[reel] = reel;
        return ScatterCounter.count(screen, WalkingWildConfig.SCATTER,
                config.reelCount() * config.visibleRows(), reels);
    }

    private int freeGamesFor(int scatterCount) {
        int threshold = config.scatterFreeGames().keySet().stream()
                .filter(value -> value <= scatterCount)
                .mapToInt(Integer::intValue).max().orElse(0);
        return config.scatterFreeGames().getOrDefault(threshold, 0);
    }

    private static List<ReelStrip> toReelStrips(List<List<Integer>> idsByReel) {
        List<ReelStrip> strips = new ArrayList<>(idsByReel.size());
        for (List<Integer> ids : idsByReel) {
            strips.add(new ReelStrip(ids.stream().map(WalkingWildConfig::symbol).toList()));
        }
        return List.copyOf(strips);
    }
}
