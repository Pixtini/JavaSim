import java.util.List;
import java.util.Random;
import GameModuleFramework.probability.WeightedTable;
import GameModuleFramework.features.SymbolInsertion;
import game.expandingwild.ExpandingWildGame;
import game.expandingwild.config.ExpandingWildConfig;
import toolkit.player.ExpandingWildPlayerAdapter;
import toolkit.player.PlayerDisplayData;

/** Focused checks for player adapters and game-neutral screen geometry. */
public final class PlayerTests {
    private static int assertions;

    private PlayerTests() {}

    public static void main(String[] args) {
        testExpandingWildAdapterProducesScreenAndWins();
        testCalculatedRoundCanBeAdaptedForReplay();
        testWildExpansionFollowsStoppedReelScreen();
        testDisplayModelSupportsOtherGridShapes();
        System.out.println("Passed " + assertions + " player assertions.");
    }

    private static void testCalculatedRoundCanBeAdaptedForReplay() {
        ExpandingWildConfig config = new ExpandingWildConfig();
        var calculated = new ExpandingWildGame(config).createSession(new Random(911))
                .playRound(1.0, config.maxWinMultiplier, false);
        ExpandingWildPlayerAdapter adapter = new ExpandingWildPlayerAdapter(config, new Random(2));
        PlayerDisplayData.Round displayed = adapter.adaptRound(calculated);
        double expectedWin = calculated.getBaseGameResult().getWin()
                + calculated.getFreeGameResults().stream()
                        .mapToDouble(game.model.SpinResult::getWin).sum();

        check(Math.abs(displayed.totalWin() - expectedWin) < 1e-9,
                "viewer round adaptation preserves its calculated total win");
        check(displayed.featureSpins().size() == calculated.getFreeGameResults().size(),
                "viewer round adaptation preserves all feature screens");
        check(displayed.baseGame().reelCount() == 5 && displayed.baseGame().rowCount() == 5,
                "viewer round adaptation preserves the basegame screen");
    }

    private static void testWildExpansionFollowsStoppedReelScreen() {
        ExpandingWildConfig config = new ExpandingWildConfig();
        config.baseGame.setSelectionWeights = List.of(
                new WeightedTable.Entry<>(0, 1), new WeightedTable.Entry<>(1, 1));
        config.baseGame.sets.get(1).reelStrips = List.of(
                List.of(0), List.of(0), List.of(0), List.of(0), List.of(0));
        config.baseGame.sets.get(1).symbolInsertions = List.of(
                new SymbolInsertion.Rule(ExpandingWildConfig.BANNER,
                        List.of(new WeightedTable.Entry<>(1, 1)),
                        List.of(List.of(1L, 0L, 0L, 0L, 0L),
                                List.of(0L, 0L, 0L, 0L, 0L),
                                List.of(0L, 0L, 0L, 0L, 0L),
                                List.of(0L, 0L, 0L, 0L, 0L),
                                List.of(0L, 0L, 0L, 0L, 0L)),
                        1, java.util.Set.of(ExpandingWildConfig.SCATTER,
                                ExpandingWildConfig.BANNER, ExpandingWildConfig.WILD)));
        ExpandingWildPlayerAdapter adapter = new ExpandingWildPlayerAdapter(config, new Random(19));

        PlayerDisplayData.Spin spin = null;
        for (int attempt = 0; attempt < 30; attempt++) {
            spin = adapter.spin(1.0).baseGame();
            if (!spin.expandingReels().isEmpty()) {
                break;
            }
        }
        check(spin != null && !spin.expandingReels().isEmpty(),
                "player adapter identifies reels that need wild expansion");
        for (int reel : spin.expandingReels()) {
            check(spin.stoppedReels().get(reel).stream()
                            .anyMatch(symbol -> symbol.label().equals("B")),
                    "stopped screen shows the banner before expansion");
            check(spin.reels().get(reel).stream()
                            .allMatch(symbol -> symbol.label().equals("W")),
                    "expanded screen shows wilds after the banner reveal");
        }
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
