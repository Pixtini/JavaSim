package game.walkingwild.model;

import GameModuleFramework.symbols.Symbol;
import java.util.List;

/** One selected highest-payout award on a G001 payline. */
public record WalkingWildLineWin(int paylineNumber, Symbol symbol, int matchingReels,
        double totalWin, List<Integer> rowsByReel) {
    public WalkingWildLineWin {
        rowsByReel = List.copyOf(rowsByReel);
    }
}
