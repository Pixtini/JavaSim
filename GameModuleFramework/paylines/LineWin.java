package GameModuleFramework.paylines;

import GameModuleFramework.symbols.Symbol;
import java.util.List;

/** Candidate paytable win on the consecutive left-to-right prefix of a line. */
public record LineWin(Symbol symbol, int matchingReels, double baseWin,
        List<Integer> reelIndexes) {
    public LineWin {
        reelIndexes = List.copyOf(reelIndexes);
    }
}
