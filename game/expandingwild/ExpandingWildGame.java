package game.expandingwild;

import game.GameSession;
import game.ExhaustiveReelGame;
import game.expandingwild.basegame.ExpandingWildBaseGame;
import GameModuleFramework.probability.WeightedTable;
import game.expandingwild.config.ExpandingWildConfig;
import game.expandingwild.config.ExpandingWildConfigAdapter;
import java.util.List;
import java.util.Random;

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
            for (List<Integer> reel : set.reelStrips) {
                if (reel.stream().anyMatch(id -> id < 0 || id > 9)) {
                    throw new IllegalArgumentException(
                            "Reel strips may contain only the ten regular paying symbols");
                }
            }
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
            validateInsertions(set, baseGame, setIndex);
        }
    }

    private void validateInsertions(ExpandingWildConfig.SpinSetConfig set,
            boolean baseGame, int setIndex) {
        int scatterRules = 0;
        int bannerRules = 0;
        for (var rule : set.symbolInsertions) {
            boolean scatterRule = rule.symbol().equals(ExpandingWildConfig.SCATTER);
            boolean bannerRule = rule.symbol().equals(ExpandingWildConfig.BANNER);
            if (!scatterRule && !bannerRule) {
                throw new IllegalArgumentException("Expanding Wild supports scatter and banner insertions only");
            }
            if (scatterRule) {
                scatterRules++;
                if (!baseGame || setIndex != 0) {
                    throw new IllegalArgumentException("Scatters can be inserted only in basegame set zero");
                }
            }
            if (bannerRule) {
                bannerRules++;
                if (setIndex != 1) {
                    throw new IllegalArgumentException("Banners can be inserted only in set one");
                }
            }
            if (rule.maxPerReel() != 1
                    || rule.positionWeights().size() != config.reelCount
                    || rule.positionWeights().stream().anyMatch(
                            reel -> reel.size() != config.visibleRows)
                    || rule.countWeights().stream().anyMatch(entry -> entry.value() > 3)) {
                throw new IllegalArgumentException(
                        "Scatter and banner insertion uses at most three symbols and one per reel");
            }
            new WeightedTable<>(rule.countWeights());
            for (int reel = 0; reel < config.reelCount; reel++) {
                boolean eligible = bannerRule;
                for (int scatterReel : config.scatterReels) {
                    eligible |= scatterReel == reel;
                }
                if (!eligible && rule.positionWeights().get(reel).stream().anyMatch(weight -> weight != 0)) {
                    throw new IllegalArgumentException("Insertion heat map enables an ineligible reel");
                }
            }
        }
        if (baseGame && setIndex == 0 && scatterRules != 1) {
            throw new IllegalArgumentException("Basegame set zero must configure one scatter insertion rule");
        }
        if (setIndex == 1 && bannerRules != 1) {
            throw new IllegalArgumentException("Set one must configure one banner insertion rule");
        }
        if (!baseGame && setIndex == 0 && !set.symbolInsertions.isEmpty()) {
            throw new IllegalArgumentException("Freegame set zero does not insert special symbols");
        }
        if ((setIndex == 0 && bannerRules != 0)
                || (baseGame && setIndex == 1 && scatterRules != 0)) {
            throw new IllegalArgumentException("Insertion symbols do not match the spin set");
        }
    }

}
