package game.expandingwild.model;

import GameModuleFramework.symbols.Symbol;
import java.util.List;

/** One selected payline win, including its game-specific banner multiplier. */
public record ExpandingWildLineWin(int paylineNumber, Symbol symbol,
        int matchingReels, double baseWin, double multiplier, double totalWin,
        List<Symbol> symbolsOnLine) {

    public ExpandingWildLineWin {
        symbolsOnLine = List.copyOf(symbolsOnLine);
    }

    public String awardLabel() {
        return symbol.id() + " " + matchingReels + "oak";
    }
}
