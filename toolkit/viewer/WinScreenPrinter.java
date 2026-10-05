package toolkit.viewer;

import GameModuleFramework.reels.ReelGrid;
import GameModuleFramework.symbols.Symbol;
import game.expandingwild.model.ExpandingWildLineWin;
import game.expandingwild.model.ExpandingWildSpinResult;
import game.model.SpinResult;
import java.io.PrintStream;
import java.util.Locale;

/** Prints a found spin using available game-specific detail or the shared result. */
public final class WinScreenPrinter {
    private WinScreenPrinter() {}

    public static void print(WinFinder.Result result, double stake, PrintStream output) {
        SpinResult winningSpin = result.winningSpin();
        output.printf("Winning screen found after %d spins (%s)%n",
                result.spinsPlayed(), result.freeGameWin() ? "freegame" : "basegame");
        output.printf(Locale.ROOT, "Total win: %.2f (%.2fx stake)%n",
                winningSpin.getWin(), winningSpin.getWin() / stake);

        if (winningSpin instanceof ExpandingWildSpinResult expandingWildResult) {
            printGrid(expandingWildResult.getExpandedGrid(), output);
            for (ExpandingWildLineWin lineWin : expandingWildResult.getLineWins()) {
                printLineWin(lineWin, stake, output);
            }
        } else if (!winningSpin.getAwards().isEmpty()) {
            output.println("Awards: " + winningSpin.getAwards());
        }
    }

    private static void printGrid(ReelGrid grid, PrintStream output) {
        output.println("Screen:");
        StringBuilder border = new StringBuilder("+");
        for (int reel = 0; reel < grid.getReelCount(); reel++) {
            border.append("-----+");
        }
        output.println(border);
        for (int row = 0; row < grid.getHeight(); row++) {
            output.print("|");
            for (int reel = 0; reel < grid.getReelCount(); reel++) {
                output.printf(" %-3s |", symbolLabel(grid.getSymbol(reel, row)));
            }
            output.println();
        }
        output.println(border);
    }

    private static void printLineWin(ExpandingWildLineWin lineWin,
            double stake, PrintStream output) {
        String symbols = lineWin.symbolsOnLine().stream()
                .map(WinScreenPrinter::symbolLabel)
                .collect(java.util.stream.Collectors.joining(" "));
        output.printf(Locale.ROOT,
                "Line %d: %s = %.2f (%.2fx stake; line multiplier %.2fx)%n",
                lineWin.paylineNumber(), symbols, lineWin.totalWin(),
                lineWin.totalWin() / stake, lineWin.multiplier());
    }

    private static String symbolLabel(Symbol symbol) {
        return switch (symbol.id()) {
            case "WILD" -> "W";
            case "SCATTER" -> "S";
            case "BANNER" -> "B";
            default -> symbol.id();
        };
    }
}
