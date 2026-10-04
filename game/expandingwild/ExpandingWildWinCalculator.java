package game.expandingwild;

import GameModuleFramework.features.AdditiveMultipliers;
import GameModuleFramework.paylines.LineWin;
import GameModuleFramework.paylines.Payline;
import GameModuleFramework.paylines.PaylineEvaluator;
import GameModuleFramework.paylines.Paytable;
import GameModuleFramework.reels.ReelGrid;
import GameModuleFramework.symbols.Symbol;
import game.expandingwild.model.ExpandingWildLineWin;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Applies the game's one-win-per-line choice and freegame banner multipliers. */
public final class ExpandingWildWinCalculator {
    private final Paytable paytable;
    private final List<Payline> paylines;
    private final Symbol wildSymbol;

    public ExpandingWildWinCalculator(Paytable paytable, List<Payline> paylines, Symbol wildSymbol) {
        this.paytable = paytable;
        this.paylines = List.copyOf(paylines);
        this.wildSymbol = wildSymbol;
    }

    public List<ExpandingWildLineWin> calculate(ReelGrid grid,
            Map<Integer, Integer> bannerMultipliersByReel) {
        List<ExpandingWildLineWin> wins = new ArrayList<>();
        for (int index = 0; index < paylines.size(); index++) {
            LineWin bestCandidate = null;
            double bestTotalWin = 0.0;
            double bestMultiplier = 1.0;

            for (LineWin candidate : PaylineEvaluator.candidates(
                    grid, paylines.get(index), paytable, wildSymbol)) {
                List<Integer> activeMultipliers = new ArrayList<>();
                for (int reelIndex : candidate.reelIndexes()) {
                    Integer multiplier = bannerMultipliersByReel.get(reelIndex + 1);
                    if (multiplier != null) {
                        activeMultipliers.add(multiplier);
                    }
                }
                double multiplier = AdditiveMultipliers.combine(activeMultipliers);
                double totalWin = candidate.baseWin() * multiplier;
                if (totalWin > bestTotalWin) {
                    bestCandidate = candidate;
                    bestTotalWin = totalWin;
                    bestMultiplier = multiplier;
                }
            }

            if (bestCandidate != null) {
                wins.add(new ExpandingWildLineWin(index + 1,
                        bestCandidate.symbol(),
                        bestCandidate.matchingReels(),
                        bestCandidate.baseWin(),
                        bestMultiplier,
                        bestTotalWin));
            }
        }
        return List.copyOf(wins);
    }
}
