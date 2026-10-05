import GameModuleFramework.paylines.Payline;
import GameModuleFramework.reels.ReelGrid;
import GameModuleFramework.symbols.Symbol;
import game.Game;
import game.GameSession;
import game.expandingwild.model.ExpandingWildLineWin;
import game.expandingwild.model.ExpandingWildSpinResult;
import game.model.GameRoundResult;
import game.model.SpinResult;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import toolkit.viewer.WinFinder;
import toolkit.viewer.WinScreenPrinter;

public final class ViewerTests {
    private static int assertions;

    public static void main(String[] args) {
        testFindsFirstWinAcrossRoundsAndFreegames();
        testStopsAtSpinLimit();
        testPrintsExpandingWildScreenAndLineSymbols();
        System.out.println("Passed " + assertions + " viewer assertions.");
    }

    private static void testFindsFirstWinAcrossRoundsAndFreegames() {
        SequenceGame game = new SequenceGame(List.of(
                round(new SpinResult(0, 0, false)),
                round(new SpinResult(0, 0, false),
                        new SpinResult(0, 0, false), new SpinResult(3, 1, false))));

        WinFinder.Result result = new WinFinder(game, new Random(1), 1.0, 100).find();

        check(result.foundWin(), "viewer finds a winning screen");
        check(result.spinsPlayed() == 4, "viewer counts base and free spins to the win");
        check(result.freeGameWin(), "viewer identifies a winning freegame screen");
        check(result.winningSpin().getWin() == 3.0, "viewer returns the winning spin");
    }

    private static void testStopsAtSpinLimit() {
        SequenceGame game = new SequenceGame(List.of(
                round(new SpinResult(0, 0, false)),
                round(new SpinResult(0, 0, false)),
                round(new SpinResult(5, 1, false))));

        WinFinder.Result result = new WinFinder(game, new Random(2), 1.0, 2).find();

        check(!result.foundWin(), "viewer returns no-win when limit expires first");
        check(result.spinsPlayed() == 2, "viewer respects the configured spin limit");
    }

    private static void testPrintsExpandingWildScreenAndLineSymbols() {
        List<Symbol> rowSymbols = List.of(
                new Symbol("T1"), new Symbol("T1"), new Symbol("T1"),
                new Symbol("WILD"), new Symbol("L4"));
        List<List<Symbol>> reels = new ArrayList<>();
        for (Symbol rowSymbol : rowSymbols) {
            reels.add(List.of(rowSymbol, rowSymbol, rowSymbol, rowSymbol, rowSymbol));
        }
        ReelGrid grid = new ReelGrid(reels);
        ExpandingWildLineWin lineWin = new ExpandingWildLineWin(
                1, new Symbol("T1"), 4, 5.0, 2.0, 10.0, rowSymbols);
        ExpandingWildSpinResult spin = new ExpandingWildSpinResult(
                grid, grid, java.util.Set.of(), 0, List.of(lineWin),
                java.util.Map.of(), false, false);
        WinFinder.Result result = new WinFinder.Result(
                7, null, spin, false);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();

        WinScreenPrinter.print(result, 1.0,
                new PrintStream(bytes, true, StandardCharsets.UTF_8));
        String output = bytes.toString(StandardCharsets.UTF_8);

        check(output.contains("Winning screen found after 7 spins"),
                "viewer output reports spins to the win");
        check(output.contains("| T1  | T1  | T1  | W   | L4  |"),
                "viewer prints the expanded grid in readable rows");
        check(output.contains("T1 T1 T1 W L4 = 10.00"),
                "viewer prints the symbols and payout on the winning line");
        check(output.contains("line multiplier 2.00x"),
                "viewer prints the line multiplier");
    }

    private static GameRoundResult round(SpinResult base, SpinResult... freeGames) {
        return new GameRoundResult(base, List.of(freeGames));
    }

    private static void check(boolean condition, String message) {
        assertions++;
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static final class SequenceGame implements Game {
        private final List<GameRoundResult> rounds;

        private SequenceGame(List<GameRoundResult> rounds) {
            this.rounds = rounds;
        }

        @Override
        public GameSession createSession(Random random) {
            return new GameSession() {
                private int index;

                @Override
                public GameRoundResult playRound() {
                    return rounds.get(Math.min(index++, rounds.size() - 1));
                }
            };
        }

        @Override
        public int[] getPaytable() {
            return new int[0];
        }
    }
}
