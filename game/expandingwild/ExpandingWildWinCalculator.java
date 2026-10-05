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
import java.util.stream.IntStream;

/** Adapts GMF line awards to this game's structured result type and banner rules. */
public final class ExpandingWildWinCalculator {
    private final PaylineWinCalculator calculator;
    private final List<Payline> paylines;

    public ExpandingWildWinCalculator(Paytable paytable, List<Payline> paylines, Symbol wildSymbol) {
        this.paylines = List.copyOf(paylines);
        this.calculator = new PaylineWinCalculator(
                paytable, this.paylines, wildSymbol, LineWinSelectionPolicy.highestPayout());
    }

    public List<ExpandingWildLineWin> calculate(ReelGrid grid,
            Map<Integer, Integer> bannerMultipliersByReel) {
        return calculate(grid, bannerMultipliersByReel, 1.0);
    }

    public List<ExpandingWildLineWin> calculate(ReelGrid grid,
            Map<Integer, Integer> bannerMultipliersByReel, double totalRoundStake) {
        return calculator.calculate(grid, totalRoundStake,
                candidate -> multiplierFor(candidate, bannerMultipliersByReel))
                .stream().map(evaluated -> adapt(evaluated, grid)).toList();
    }

    private static double multiplierFor(LineWin candidate,
            Map<Integer, Integer> bannerMultipliersByReel) {
        return AdditiveMultipliers.combine(candidate.reelIndexes().stream()
                .map(bannerMultipliersByReel::get)
                .filter(java.util.Objects::nonNull)
                .toList());
    }

    private ExpandingWildLineWin adapt(EvaluatedLineWin evaluated, ReelGrid grid) {
        LineWin candidate = evaluated.candidate();
        Payline payline = paylines.get(evaluated.paylineNumber() - 1);
        List<Symbol> symbolsOnLine = IntStream.range(0, payline.getReelCount())
                .mapToObj(reel -> grid.getSymbol(reel, payline.getRow(reel)))
                .toList();
        return new ExpandingWildLineWin(evaluated.paylineNumber(), candidate.symbol(),
                candidate.matchingReels(), evaluated.baseWin(), evaluated.multiplier(),
                evaluated.totalWin(), symbolsOnLine);
    }
}
