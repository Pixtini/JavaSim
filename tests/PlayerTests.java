import java.util.List;
import java.util.Random;
import toolkit.player.ExpandingWildPlayerAdapter;
import toolkit.player.PlayerDisplayData;

/** Focused checks for player adapters and game-neutral screen geometry. */
public final class PlayerTests {
    private static int assertions;

    private PlayerTests() {}

    public static void main(String[] args) {
        testExpandingWildAdapterProducesScreenAndWins();
        testDisplayModelSupportsOtherGridShapes();
        System.out.println("Passed " + assertions + " player assertions.");
    }

    private static void testExpandingWildAdapterProducesScreenAndWins() {
        ExpandingWildPlayerAdapter adapter = new ExpandingWildPlayerAdapter(new Random(73));
        PlayerDisplayData.Round round = adapter.spin(1.0);
        PlayerDisplayData.Spin base = round.baseGame();

        check(base.reelCount() == 5 && base.rowCount() == 5,
                "expanding-wild adapter returns its configured 5x5 viewport");
        check(base.totalWin() >= 0.0, "spin amount is non-negative");
        check(round.featureSpins().size() <= round.awardedFreeGames(),
                "displayed feature outcomes do not exceed the award");
        check(adapter.symbolPalette().stream().map(PlayerDisplayData.SymbolCell::style)
                        .distinct().count() == adapter.symbolPalette().size(),
                "each prototype symbol has its own presentation color");
        for (PlayerDisplayData.Win win : base.wins()) {
            check(!win.positions().isEmpty(), "line wins include highlight positions");
            check(win.positions().stream().allMatch(position -> position.reel() < base.reelCount()
                    && position.row() < base.rowCount()), "highlight cells fit the screen");
        }
    }

    private static void testDisplayModelSupportsOtherGridShapes() {
        var symbol = new PlayerDisplayData.SymbolCell("A", "high");
        var spin = new PlayerDisplayData.Spin("Basegame",
                List.of(List.of(symbol, symbol, symbol),
                        List.of(symbol, symbol, symbol),
                        List.of(symbol, symbol, symbol)),
                1.0, List.of(new PlayerDisplayData.Win("3oak A", 1.0,
                        List.of(new PlayerDisplayData.Position(0, 1),
                                new PlayerDisplayData.Position(1, 1),
                                new PlayerDisplayData.Position(2, 1)))));
        check(spin.reelCount() == 3 && spin.rowCount() == 3,
                "display model accepts a 3x3 game grid");
        check(spin.wins().get(0).positions().size() == 3,
                "display model supports arbitrary positional win overlays");
    }

    private static void check(boolean condition, String message) {
        assertions++;
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
