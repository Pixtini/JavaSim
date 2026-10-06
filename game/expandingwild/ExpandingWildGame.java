package game.expandingwild;

import game.GameSession;
import game.ExhaustiveReelGame;
import game.expandingwild.basegame.ExpandingWildBaseGame;
import GameModuleFramework.probability.WeightedTable;
import game.expandingwild.config.ExpandingWildConfig;
import game.expandingwild.config.ExpandingWildConfigAdapter;
import java.util.Random;
import java.util.List;

/** Five-reel expanding wild game module. */
public final class ExpandingWildGame implements ExhaustiveReelGame {
    private final ExpandingWildConfig config;

    public ExpandingWildGame(ExpandingWildConfig config) {
        this.config = config;
        validateConfig();
    }

    @Override
    public GameSession createSession(Random random) {
        return new ExpandingWildGameSession(config, random);
    }

    /** Generic simulation stats currently classify spin outcomes as loss or win. */
    @Override
    public int[] getPaytable() {
        return new int[] {0, 0};
    }

    @Override
    public List<String> getAwardLabels() {
        return config.paytable.getAwards().stream()
                .map(award -> award.symbol().id() + " " + award.matchingSymbols() + "oak")
                .toList();
    }

    @Override
    public int getBaseGameSetCount() {
        return config.baseGame.sets.size();
    }

    @Override
    public int getFreeGameSetCount() {
        return config.freeGame.sets.size();
    }

    @Override
    public double getMaxWinMultiplier() {
        return config.maxWinMultiplier;
    }

    @Override
    public int getReelCount() {
        return config.baseGame.sets.get(0).reelStrips.size();
    }

    @Override
    public int getStopCount(int reelIndex) {
        return getStopCount(0, reelIndex);
    }

    @Override
    public int getReelSetCount() {
        return config.baseGame.sets.size();
    }

    @Override
    public int getStopCount(int setIndex, int reelIndex) {
        return config.baseGame.sets.get(setIndex).reelStrips.get(reelIndex).size();
    }

    @Override
    public long getReelSetWeight(int setIndex) {
        return config.baseGame.setSelectionWeights.stream()
                .filter(entry -> entry.value() == setIndex)
                .mapToLong(WeightedTable.Entry::weight)
                .sum();
    }

    @Override
    public StopEvaluator createStopEvaluator() {
        return createStopEvaluator(0);
    }

    @Override
    public StopEvaluator createStopEvaluator(int setIndex) {
        ExpandingWildBaseGame baseGame = new ExpandingWildBaseGame(config, new Random(0));
        return (stops, stake) -> baseGame.spinAtStops(setIndex, stops, stake);
    }

    private void validateConfig() {
        if (config.reelCount <= 0 || config.visibleRows <= 0) {
            throw new IllegalArgumentException("Each expanding wild set must define its 5-reel grid");
        }
        validateMode(config.baseGame, true);
        validateMode(config.freeGame, false);
        if (config.paylines.isEmpty()) {
            throw new IllegalArgumentException("Expanding wild config must define paylines");
        }
        for (var payline : config.paylines) {
            if (payline.getReelCount() != config.reelCount) {
                throw new IllegalArgumentException("Each payline must span every reel");
            }
            for (int row : payline.getRowsByReel()) {
                if (row >= config.visibleRows) {
                    throw new IllegalArgumentException("Payline row exceeds the visible reel height");
                }
            }
        }
        if (config.freeGamesAwarded < 0
                || !Double.isFinite(config.maxWinMultiplier)
                || config.maxWinMultiplier <= 0.0) {
            throw new IllegalArgumentException("Expanding wild feature settings are invalid");
        }
    }

    private void validateMode(ExpandingWildConfig.SpinModeConfig mode, boolean baseGame) {
        if (mode.sets.size() != 2) {
            throw new IllegalArgumentException("Basegame and freegame must each define two sets");
        }
        new WeightedTable<>(mode.setSelectionWeights);
        boolean[] selected = new boolean[mode.sets.size()];
        for (var entry : mode.setSelectionWeights) {
            if (entry.value() < 0 || entry.value() >= mode.sets.size()) {
                throw new IllegalArgumentException("Set selection references an unknown set");
            }
            selected[entry.value()] = true;
        }
        for (int setIndex = 0; setIndex < mode.sets.size(); setIndex++) {
            var set = mode.sets.get(setIndex);
            if (!selected[setIndex] || set.reelStrips.size() != config.reelCount) {
                throw new IllegalArgumentException("Every spin set must be selectable and define every reel");
            }
            ExpandingWildConfigAdapter.toReelStrips(set);
            if (!set.bannerMultiplierWeights.isEmpty()) {
                new WeightedTable<>(set.bannerMultiplierWeights);
            } else if (!baseGame && setIndex == 1) {
                throw new IllegalArgumentException("Freegame set 1 must define banner multipliers");
            }
            for (var entry : set.bannerMultiplierWeights) {
                if (entry.value() < 1) {
                    throw new IllegalArgumentException("Banner multiplier values must be positive");
                }
            }
            boolean hasBanner = containsSymbol(set, ExpandingWildConfig.BANNER);
            boolean hasScatter = containsSymbol(set, ExpandingWildConfig.SCATTER);
            boolean hasStaticWild = containsSymbol(set, ExpandingWildConfig.WILD);
            if (setIndex == 0 && (hasBanner || hasStaticWild || (!baseGame && hasScatter))) {
                throw new IllegalArgumentException("Set 0 must contain no wilds and no freegame scatters");
            }
            if (setIndex == 1 && (!hasBanner || hasScatter || hasStaticWild)) {
                throw new IllegalArgumentException("Set 1 must contain wild banners and no scatters");
            }
            if (baseGame && setIndex == 0 && !hasScatter) {
                throw new IllegalArgumentException("Basegame set 0 must contain scatters");
            }
            for (int reelIndex = 0; reelIndex < set.reelStrips.size(); reelIndex++) {
                List<Integer> reel = set.reelStrips.get(reelIndex);
                boolean reelHasBanner = reel.contains(10);
                if ((setIndex == 1) != reelHasBanner) {
                    throw new IllegalArgumentException(
                            "Only wild-reel sets may contain banners, and each of their reels must have one");
                }
                boolean reelHasScatter = reel.contains(12);
                boolean scatterEligible = false;
                if (baseGame && setIndex == 0) {
                    for (int configuredReel : config.scatterReels) {
                        scatterEligible |= configuredReel == reelIndex;
                    }
                }
                if (reelHasScatter != scatterEligible) {
                    throw new IllegalArgumentException(
                            "Scatters must appear only in eligible reels of basegame set zero");
                }
            }
        }
    }

    private boolean containsSymbol(ExpandingWildConfig.SpinSetConfig set,
            GameModuleFramework.symbols.Symbol symbol) {
        return set.reelStrips.stream().flatMap(List::stream)
                .map(ExpandingWildConfig::fromId).anyMatch(symbol::equals);
    }
}
