package toolkit.player;

import game.ember_forge.EmberForgeGame;
import game.ember_forge.config.EmberForgeConfig;
import game.ember_forge.model.EmberForgeSpin;
import game.model.*;
import java.util.*;

/** Presents actual G002 session results, including the current and next Freegame Heat. */
public final class EmberForgePlayerAdapter implements PlayerGameAdapter {
    private final EmberForgeConfig config;
    private final game.GameSession session;
    private final List<PlayerDisplayData.SymbolCell> palette;
    public EmberForgePlayerAdapter(EmberForgeConfig config, Random random) {
        this.config = config;
        session = new EmberForgeGame(config).createSession(random);
        palette = java.util.stream.IntStream.rangeClosed(1, 8).mapToObj(this::cell).toList();
    }
    @Override public String gameName() { return "Ember Forge"; }
    @Override public String modeName() { return config.mode(); }
    @Override public double costMultiplier() { return config.costMultiplier(); }
    @Override public List<PlayerDisplayData.SymbolCell> symbolPalette() { return palette; }
    @Override public PlayerDisplayData.Round spin(double stake) {
        return adaptRound(session.playRound(stake, stake * config.maxWinMultiplier()));
    }
    @Override public PlayerDisplayData.Round adaptRound(GameRoundResult round) {
        return new PlayerDisplayData.Round(adapt(round.getBaseGameResult(), false),
                round.getFreeGameResults().stream().map(s -> adapt(s, true)).toList(),
                round.getBaseGameResult().hasFreeSpin(),
                round.getBaseGameResult().hasFreeSpin() ? config.freeSpins() : 0);
    }
    private PlayerDisplayData.Spin adapt(SpinResult result, boolean free) {
        if (!(result instanceof EmberForgeSpin spin))
            throw new IllegalArgumentException("Expected detailed Ember Forge outcome");
        List<List<PlayerDisplayData.SymbolCell>> grid = new ArrayList<>();
        for (int reel = 0; reel < 5; reel++) {
            List<PlayerDisplayData.SymbolCell> rows = new ArrayList<>();
            for (int row = 0; row < 3; row++)
                rows.add(cell(EmberForgeConfig.symbolId(spin.grid().getSymbol(reel, row))));
            grid.add(rows);
        }
        var wins = spin.lineWins().stream().map(w -> {
            var positions = new ArrayList<PlayerDisplayData.Position>();
            var rows = config.lineRows().get(w.paylineNumber() - 1);
            for (int reel = 0; reel < w.candidate().matchingReels(); reel++)
                positions.add(new PlayerDisplayData.Position(reel, rows.get(reel)));
            return new PlayerDisplayData.Win("Line " + w.paylineNumber() + " · "
                    + w.candidate().symbol().id() + " · " + spin.heat() + "x Heat",
                    w.totalWin(), positions);
        }).toList();
        String detail = free ? "Heat " + spin.heat() + "x → next " + spin.nextHeat()
                + "x · wins raise Heat, maximum 5x"
                : spin.hasFreeSpin() ? "Three Scatters · eight Forge Freegames start at Heat 1x"
                : "Ten lines · three Scatters award eight Forge Freegames";
        return new PlayerDisplayData.Spin(free ? "Forge Freegame · Heat " + spin.heat() + "x" : "Basegame",
                grid, spin.getWin(), wins, detail);
    }
    private PlayerDisplayData.SymbolCell cell(int id) {
        String style = switch (id) {
            case 1 -> "L1"; case 2 -> "L2"; case 3 -> "L3"; case 4 -> "T4";
            case 5 -> "T3"; case 6 -> "T2"; case 7 -> "W"; case 8 -> "S";
            default -> throw new IllegalArgumentException("Invalid symbol");
        };
        String label = switch (id) {
            case 1 -> "C"; case 2 -> "Cu"; case 3 -> "Fe"; case 4 -> "Ag";
            case 5 -> "Au"; case 6 -> "Em"; case 7 -> "W"; case 8 -> "S";
            default -> throw new IllegalArgumentException("Invalid symbol");
        };
        return new PlayerDisplayData.SymbolCell(label, style);
    }
}
