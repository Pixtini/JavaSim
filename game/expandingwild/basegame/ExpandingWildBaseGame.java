package game.expandingwild.basegame;

import GameModuleFramework.reels.ReelGrid;
import GameModuleFramework.reels.ReelStrip;
import GameModuleFramework.reels.WildExpansion;
import GameModuleFramework.features.ScatterTrigger;
import game.expandingwild.ExpandingWildWinCalculator;
import game.expandingwild.config.ExpandingWildConfig;
import game.expandingwild.config.ExpandingWildConfigAdapter;
import game.expandingwild.model.ExpandingWildSpinResult;
import java.util.Map;
import java.util.List;
import java.util.Random;

/** Executes one expanding wild basegame spin. */
public final class ExpandingWildBaseGame {
    private final ExpandingWildConfig config;
    private final Random random;
    private final List<ReelStrip> reelStrips;
    private final ScatterTrigger scatterTrigger;
    private final ExpandingWildWinCalculator winCalculator;

    public ExpandingWildBaseGame(ExpandingWildConfig config, Random random) {
        this.config = config;
        this.random = random;
        this.reelStrips = ExpandingWildConfigAdapter.toBaseReelStrips(config);
        this.scatterTrigger = new ScatterTrigger(
                ExpandingWildConfig.SCATTER,
                config.freeGameTriggerScatterCount,
                config.scatterReels);
        this.winCalculator = new ExpandingWildWinCalculator(
                config.paytable, config.paylines, ExpandingWildConfig.WILD);
    }

    public ExpandingWildSpinResult spin() {
        return spin(1.0);
    }

    public ExpandingWildSpinResult spin(double totalRoundStake) {
        ReelGrid stoppedGrid = ReelGrid.spin(
                reelStrips, config.visibleRows, random);
        ScatterTrigger.Result scatterResult = scatterTrigger.evaluate(stoppedGrid);
        WildExpansion.Result expansion = WildExpansion.expandColumns(
                stoppedGrid, ExpandingWildConfig.BANNER, ExpandingWildConfig.WILD);
        var lineWins = winCalculator.calculate(expansion.grid(), Map.of(), totalRoundStake);

        return new ExpandingWildSpinResult(
                stoppedGrid,
                expansion.grid(),
                expansion.expandedReels(),
                scatterResult.scatterCount(),
                lineWins,
                Map.of(),
                scatterResult.triggered(),
                false);
    }

}
