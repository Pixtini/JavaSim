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
    public int getReelCount() {
        return config.baseReelStrips.size();
    }

    @Override
    public int getStopCount(int reelIndex) {
        return config.baseReelStrips.get(reelIndex).size();
    }

    @Override
    public StopEvaluator createStopEvaluator() {
        ExpandingWildBaseGame baseGame = new ExpandingWildBaseGame(config, new Random(0));
        return baseGame::spinAtStops;
    }

    private void validateConfig() {
        if (config.reelCount <= 0 || config.visibleRows <= 0
                || config.baseReelStrips.size() != config.reelCount) {
            throw new IllegalArgumentException("Expanding wild config must define its 5-reel grid");
        }
        ExpandingWildConfigAdapter.toBaseReelStrips(config); // Validates configured symbol IDs.
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
        if (config.freeGamesAwarded < 0) {
            throw new IllegalArgumentException("Expanding wild feature settings are invalid");
        }
        new WeightedTable<>(config.bannerMultiplierWeights);
        for (var entry : config.bannerMultiplierWeights) {
            if (entry.value() < 1) {
                throw new IllegalArgumentException("Banner multiplier values must be positive");
            }
        }
    }
}
