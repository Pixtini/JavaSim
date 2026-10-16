package game.walkingwild;

import GameModuleFramework.paylines.LineWinSelectionPolicy;
import GameModuleFramework.paylines.PaylineWinCalculator;
import GameModuleFramework.reels.ReelGrid;
import game.walkingwild.config.WalkingWildConfig;
import java.util.List;
import game.walkingwild.model.WalkingWildLineWin;

/** Applies the PAR paytable and highest-payout-per-line rule to a stopped screen. */
public final class WalkingWildWinEvaluator {
    private final PaylineWinCalculator calculator;
    private final WalkingWildConfig config;

    public WalkingWildWinEvaluator(WalkingWildConfig config) {
        this.config = config;
        calculator = new PaylineWinCalculator(config.paytable(), config.paylines(),
                WalkingWildConfig.WILD, LineWinSelectionPolicy.highestPayout());
    }

    public double calculateWin(ReelGrid grid, double baseStakeUnit) {
        return calculateLineWins(grid, baseStakeUnit).stream()
                .mapToDouble(WalkingWildLineWin::totalWin).sum();
    }

    public List<WalkingWildLineWin> calculateLineWins(ReelGrid grid, double baseStakeUnit) {
        return calculator.calculate(grid, baseStakeUnit, ignored -> 1.0).stream()
                .map(win -> {
                    var payline = config.paylines().get(win.paylineNumber() - 1);
                    List<Integer> rows = new java.util.ArrayList<>();
                    for (int reel = 0; reel < win.candidate().matchingReels(); reel++) {
                        rows.add(payline.getRow(reel));
                    }
                    return new WalkingWildLineWin(win.paylineNumber(), win.candidate().symbol(),
                            win.candidate().matchingReels(), win.totalWin(), rows);
                }).toList();
    }
}
