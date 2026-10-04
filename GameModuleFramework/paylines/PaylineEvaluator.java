package GameModuleFramework.paylines;

import GameModuleFramework.reels.ReelGrid;
import GameModuleFramework.symbols.Symbol;
import java.util.ArrayList;
import java.util.List;

/** Finds unbroken left-to-right paytable matches for one payline. */
public final class PaylineEvaluator {
    private PaylineEvaluator() {
    }

    public static List<LineWin> candidates(ReelGrid grid, Payline payline,
            Paytable paytable, Symbol wildSymbol) {
        if (payline.getReelCount() != grid.getReelCount()) {
            throw new IllegalArgumentException("Payline width must match the reel grid");
        }

        List<LineWin> candidates = new ArrayList<>();
        int leadingWilds = 0;
        while (leadingWilds < grid.getReelCount()
                && grid.getSymbol(leadingWilds, payline.getRow(leadingWilds)).equals(wildSymbol)) {
            leadingWilds++;
        }

        if (leadingWilds == grid.getReelCount()) {
            for (Symbol payingSymbol : paytable.getSymbols()) {
                addCandidate(candidates, payingSymbol, grid.getReelCount(), paytable);
            }
            return List.copyOf(candidates);
        }

        Symbol firstRegularSymbol = grid.getSymbol(
                leadingWilds, payline.getRow(leadingWilds));
        int matchingReels = leadingWilds + 1;
        while (matchingReels < grid.getReelCount()) {
            Symbol visible = grid.getSymbol(matchingReels, payline.getRow(matchingReels));
            if (!visible.equals(firstRegularSymbol) && !visible.equals(wildSymbol)) {
                break;
            }
            matchingReels++;
        }

        addCandidate(candidates, firstRegularSymbol, matchingReels, paytable);

        // A leading run of wilds can itself make a shorter win for another symbol.
        if (leadingWilds >= 3) {
            for (Symbol payingSymbol : paytable.getSymbols()) {
                if (!payingSymbol.equals(firstRegularSymbol)) {
                    addCandidate(candidates, payingSymbol, leadingWilds, paytable);
                }
            }
        }
        return List.copyOf(candidates);
    }

    private static void addCandidate(List<LineWin> candidates, Symbol symbol,
            int matchingReels, Paytable paytable) {
        double baseWin = paytable.getPayout(symbol, matchingReels);
        if (baseWin <= 0.0) {
            return;
        }
        List<Integer> reelIndexes = new ArrayList<>(matchingReels);
        for (int reel = 0; reel < matchingReels; reel++) {
            reelIndexes.add(reel);
        }
        candidates.add(new LineWin(symbol, matchingReels, baseWin, reelIndexes));
    }
}
