package game.expandingwild;

import GameModuleFramework.features.AdditiveMultipliers;
import GameModuleFramework.paylines.EvaluatedLineWin;
import GameModuleFramework.paylines.LineWin;
import GameModuleFramework.paylines.LineWinSelectionPolicy;
import GameModuleFramework.paylines.Payline;
import GameModuleFramework.paylines.PaylineWinCalculator;
import GameModuleFramework.paylines.Paytable;
import GameModuleFramework.reels.ReelGrid;
import GameModuleFramework.symbols.Symbol;
import game.expandingwild.model.ExpandingWildLineWin;
import java.util.List;
import java.util.Map;

/** Adapts GMF line awards to this game's structured result type and banner rules. */
public final class ExpandingWildWinCalculator {
    private final PaylineWinCalculator calculator;

    public ExpandingWildWinCalculator(Paytable paytable, List<Payline> paylines, Symbol wildSymbol) {
        this.calculator = new PaylineWinCalculator(
                paytable, paylines, wildSymbol, LineWinSelectionPolicy.highestPayout());
    }

    public List<ExpandingWildLineWin> calculate(ReelGrid grid,
            Map<Integer, Integer> bannerMultipliersByReel) {
        return calculate(grid, bannerMultipliersByReel, 1.0);
    }

    public List<ExpandingWildLineWin> calculate(ReelGrid grid,
            Map<Integer, Integer> bannerMultipliersByReel, double totalRoundStake) {
        return calculator.calculate(grid, totalRoundStake,
                candidate -> multiplierFor(candidate, bannerMultipliersByReel))
                .stream().map(ExpandingWildWinCalculator::adapt).toList();
    }

    private static double multiplierFor(LineWin candidate,
            Map<Integer, Integer> bannerMultipliersByReel) {
        return AdditiveMultipliers.combine(candidate.reelIndexes().stream()
                .map(bannerMultipliersByReel::get)
                .filter(java.util.Objects::nonNull)
                .toList());
    }

    private static ExpandingWildLineWin adapt(EvaluatedLineWin evaluated) {
        LineWin candidate = evaluated.candidate();
        return new ExpandingWildLineWin(evaluated.paylineNumber(), candidate.symbol(),
                candidate.matchingReels(), evaluated.baseWin(), evaluated.multiplier(),
                evaluated.totalWin());
    }
}
