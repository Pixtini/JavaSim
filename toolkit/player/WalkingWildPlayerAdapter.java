package toolkit.player;

import GameModuleFramework.reels.ReelGrid;
import GameModuleFramework.symbols.Symbol;
import game.GameSession;
import game.model.GameRoundResult;
import game.model.SpinResult;
import game.walkingwild.WalkingWildGame;
import game.walkingwild.config.WalkingWildConfig;
import game.walkingwild.model.WalkingWildLineWin;
import game.walkingwild.model.WalkingWildSpin;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Adapts selected-mode G001 outcomes to the shared prototype-player view model. */
public final class WalkingWildPlayerAdapter implements PlayerGameAdapter {
    private final WalkingWildConfig config;
    private final GameSession session;
    private final List<PlayerDisplayData.SymbolCell> palette = List.of(
            cell("T1"), cell("T2"), cell("T3"), cell("T4"), cell("L1"),
            cell("L2"), cell("L3"), cell("L4"), cell("W"), cell("S"));

    public WalkingWildPlayerAdapter(WalkingWildConfig config, Random random) {
        this.config = config;
        this.session = new WalkingWildGame(config).createSession(random);
    }

    @Override public String gameName() { return "Walking Wild"; }
    @Override public String modeName() { return config.mode(); }
    @Override public double costMultiplier() { return config.costMultiplier(); }

    @Override
    public PlayerDisplayData.Round spin(double stake) {
        return adaptRound(session.playRound(stake, config.maxWinMultiplier() * stake));
    }

    public PlayerDisplayData.Round adaptRound(GameRoundResult result) {
        PlayerDisplayData.Spin base = adapt(result.getBaseGameResult(), "Basegame");
        List<PlayerDisplayData.Spin> freeSpins = result.getFreeGameResults().stream()
                .map(spin -> adapt(spin, "Freegame")).toList();
        boolean triggered = result.getBaseGameResult().hasFreeSpin();
        int awarded = 0;
        if (result.getBaseGameResult() instanceof WalkingWildSpin walking) {
            awarded = config.scatterFreeGames().entrySet().stream()
                    .filter(entry -> entry.getKey() <= walking.scatterCount())
                    .max(java.util.Map.Entry.comparingByKey())
                    .map(java.util.Map.Entry::getValue).orElse(0);
        }
        return new PlayerDisplayData.Round(base, freeSpins, triggered, awarded);
    }

    @Override public List<PlayerDisplayData.SymbolCell> symbolPalette() { return palette; }

    private PlayerDisplayData.Spin adapt(SpinResult result, String mode) {
        if (!(result instanceof WalkingWildSpin walking)) {
            throw new IllegalStateException("G001 returned an unsupported detailed spin result");
        }
        return new PlayerDisplayData.Spin(mode, adaptGrid(walking.grid()), walking.getWin(),
                walking.lineWins().stream().map(this::adaptWin).toList(),
                String.format(java.util.Locale.ROOT,
                        "Set %d  •  scatters %d  •  active wilds %d",
                        walking.getSetIndex(), walking.scatterCount(), walking.wildPositions().size()));
    }

    private PlayerDisplayData.Win adaptWin(WalkingWildLineWin line) {
        List<PlayerDisplayData.Position> positions = new ArrayList<>();
        for (int reel = 0; reel < line.matchingReels(); reel++) {
            positions.add(new PlayerDisplayData.Position(reel, line.rowsByReel().get(reel)));
        }
        return new PlayerDisplayData.Win("Line " + line.paylineNumber() + " · "
                + line.matchingReels() + "oak " + label(line.symbol()), line.totalWin(), positions);
    }

    private List<List<PlayerDisplayData.SymbolCell>> adaptGrid(ReelGrid grid) {
        List<List<PlayerDisplayData.SymbolCell>> reels = new ArrayList<>();
        for (int reel = 0; reel < grid.getReelCount(); reel++) {
            List<PlayerDisplayData.SymbolCell> rows = new ArrayList<>();
            for (int row = 0; row < grid.getHeight(); row++) rows.add(cell(label(grid.getSymbol(reel, row))));
            reels.add(List.copyOf(rows));
        }
        return List.copyOf(reels);
    }

    private static String label(Symbol symbol) {
        return symbol.id().equals("WILD") ? "W" : symbol.id().equals("SCATTER") ? "S" : symbol.id();
    }

    private static PlayerDisplayData.SymbolCell cell(String label) {
        return new PlayerDisplayData.SymbolCell(label, label);
    }
}
