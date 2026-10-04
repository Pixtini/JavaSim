package game.expandingwild.model;

import GameModuleFramework.symbols.Symbol;

/** One selected payline win, including its game-specific banner multiplier. */
public record ExpandingWildLineWin(int paylineNumber, Symbol symbol,
        int matchingReels, double baseWin, double multiplier, double totalWin) {

    public String awardLabel() {
        return symbol.id() + " " + matchingReels + "oak";
    }
}
