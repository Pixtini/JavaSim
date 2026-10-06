package toolkit.viewer;

import GameModuleFramework.reels.ReelGrid;
import GameModuleFramework.symbols.Symbol;
import game.expandingwild.model.ExpandingWildLineWin;
import game.expandingwild.model.ExpandingWildSpinResult;
import game.model.SpinResult;
import game.model.GameRoundResult;
import simulation.replay.SavedGameplay;
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

    /** Prints every recalculated screen in a saved round and its validation summary. */
    public static void printReplay(SavedGameplay saved, GameRoundResult replayed,
            boolean matches, PrintStream output) {
        output.printf("Replay %d (%s)%n", saved.id(), saved.gameId());
        printReplaySpin("Basegame", replayed.getBaseGameResult(), saved.stake(), output);
        for (int index = 0; index < replayed.getFreeGameResults().size(); index++) {
            printReplaySpin("Freegame spin " + (index + 1),
                    replayed.getFreeGameResults().get(index), saved.stake(), output);
        }
        double featureWin = replayed.getFreeGameResults().stream()
                .mapToDouble(SpinResult::getWin).sum();
        double totalWin = replayed.getBaseGameResult().getWin() + featureWin;
        output.printf(Locale.ROOT, "Recalculated total win: %.4f (%.4fx stake)%n",
                totalWin, totalWin / saved.stake());
        output.printf(Locale.ROOT,
                "Recorded total/basegame/feature wins: %.4f / %.4f / %.4f%n",
                saved.totalWin(), saved.baseGameWin(), saved.featureGameWin());
        output.println("Replay validation: " + (matches ? "PASSED" : "FAILED"));
    }

    private static void printReplaySpin(String label, SpinResult spin,
            double stake, PrintStream output) {
        output.printf(Locale.ROOT, "%s (set %d): %.4f (%.4fx stake)%n",
                label, spin.getSetIndex(), spin.getWin(), spin.getWin() / stake);
        if (spin instanceof ExpandingWildSpinResult expanding) {
            printGrid(expanding.getExpandedGrid(), output);
            for (ExpandingWildLineWin lineWin : expanding.getLineWins()) {
                printLineWin(lineWin, stake, output);
            }
        } else if (!spin.getAwards().isEmpty()) {
            output.println("Awards: " + spin.getAwards());
        }
    }

    private static void printGrid(ReelGrid grid, PrintStream output) {
        output.println("Screen:");
        output.print("|");
        for (int reel = 0; reel < grid.getReelCount(); reel++) {
            output.printf(" %-3s |", "R" + (reel + 1));
        }
        output.println();
        for (int row = 0; row < grid.getHeight(); row++) {
            output.print("|");
            for (int reel = 0; reel < grid.getReelCount(); reel++) {
                output.printf(" %-3s |", symbolLabel(grid.getSymbol(reel, row)));
            }
            output.println();
        }
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
