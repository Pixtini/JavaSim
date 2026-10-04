package game.expandingwild.features;

import GameModuleFramework.reels.ReelGrid;
import GameModuleFramework.reels.ReelStrip;
import GameModuleFramework.reels.WildExpansion;
import GameModuleFramework.probability.WeightedTable;
import game.expandingwild.ExpandingWildWinCalculator;
import game.expandingwild.config.ExpandingWildConfig;
import game.expandingwild.config.ExpandingWildConfigAdapter;
import game.expandingwild.model.ExpandingWildSpinResult;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/** Plays free spins on scatter-free reels with random expanding-banner multipliers. */
public final class ExpandingWildFreeGame {
    private final ExpandingWildConfig config;
    private final Random random;
    private final List<ReelStrip> freeGameReels;
    private final ExpandingWildWinCalculator winCalculator;
    private final WeightedTable<Integer> bannerMultiplierTable;

    public ExpandingWildFreeGame(ExpandingWildConfig config, Random random) {
        this.config = config;
        this.random = random;
        this.freeGameReels = ExpandingWildConfigAdapter.toBaseReelStrips(config).stream()
                .map(reel -> reel.without(ExpandingWildConfig.SCATTER))
                .toList();
        this.bannerMultiplierTable = new WeightedTable<>(config.bannerMultiplierWeights);
        this.winCalculator = new ExpandingWildWinCalculator(
                config.paytable, config.paylines, ExpandingWildConfig.WILD);
    }

    public ExpandingWildSpinResult spin() {
        return spin(1.0);
    }

    public ExpandingWildSpinResult spin(double totalRoundStake) {
        ReelGrid stoppedGrid = ReelGrid.spin(freeGameReels, config.visibleRows, random);
        WildExpansion.Result expansion = WildExpansion.expandColumns(
                stoppedGrid, ExpandingWildConfig.BANNER, ExpandingWildConfig.WILD);
        Map<Integer, Integer> multipliers = assignBannerMultipliers(expansion);
        var lineWins = winCalculator.calculate(expansion.grid(), multipliers, totalRoundStake);

        return new ExpandingWildSpinResult(
                stoppedGrid,
                expansion.grid(),
                expansion.expandedReels(),
                0,
                lineWins,
                multipliers,
                false,
                true);
    }

    private Map<Integer, Integer> assignBannerMultipliers(WildExpansion.Result expansion) {
        List<Integer> expandedReels = new ArrayList<>(expansion.expandedReels());
        expandedReels.sort(Integer::compareTo);

        Map<Integer, Integer> multipliers = new LinkedHashMap<>();
        for (int reelIndex : expandedReels) {
            multipliers.put(reelIndex, bannerMultiplierTable.draw(random));
        }
        return Map.copyOf(multipliers);
    }

}
