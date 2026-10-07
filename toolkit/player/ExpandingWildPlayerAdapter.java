package toolkit.player;

import GameModuleFramework.reels.ReelGrid;
import GameModuleFramework.symbols.Symbol;
import game.GameSession;
import game.expandingwild.ExpandingWildGame;
import game.expandingwild.config.ExpandingWildConfig;
import game.expandingwild.model.ExpandingWildLineWin;
import game.expandingwild.model.ExpandingWildSpinResult;
import game.model.GameRoundResult;
import game.model.SpinResult;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Converts the current expanding-wild game outcomes to the generic player display model. */
public final class ExpandingWildPlayerAdapter implements PlayerGameAdapter {
    private final ExpandingWildConfig config;
    private final GameSession session;
    private final List<PlayerDisplayData.SymbolCell> symbolPalette;

    public ExpandingWildPlayerAdapter(Random random) {
        this(new ExpandingWildConfig(), random);
    }

    public ExpandingWildPlayerAdapter(ExpandingWildConfig config, Random random) {
        this.config = config;
        this.session = new ExpandingWildGame(config).createSession(random);
        this.symbolPalette = List.of(
                cell("T1"), cell("T2"), cell("T3"), cell("T4"), cell("T5"),
                cell("L1"), cell("L2"), cell("L3"), cell("L4"), cell("L5"),
                cell("W"), cell("S"));
    }

    @Override
    public String gameName() {
        return "Expanding Wild";
    }

    @Override
    public PlayerDisplayData.Round spin(double stake) {
        GameRoundResult result = session.playRound(
                stake, config.maxWinMultiplier * stake, false);
        return adaptRound(result);
    }

    /** Converts a previously calculated game round for presentation or playback. */
    public PlayerDisplayData.Round adaptRound(GameRoundResult result) {
        PlayerDisplayData.Spin base = adapt(result.getBaseGameResult(), "Basegame");
        List<PlayerDisplayData.Spin> freeSpins = result.getFreeGameResults().stream()
                .map(spin -> adapt(spin, "Freegame"))
                .toList();
        boolean triggered = result.getBaseGameResult().hasFreeSpin();
        return new PlayerDisplayData.Round(base, freeSpins, triggered,
                triggered ? config.freeGamesAwarded : 0);
    }

    @Override
    public List<PlayerDisplayData.SymbolCell> symbolPalette() {
        return symbolPalette;
    }

    private PlayerDisplayData.Spin adapt(SpinResult result, String mode) {
        if (!(result instanceof ExpandingWildSpinResult expandingResult)) {
            throw new IllegalStateException("Expanding Wild returned an unsupported spin result");
        }
        ReelGrid grid = expandingResult.getExpandedGrid();
        List<List<PlayerDisplayData.SymbolCell>> finalReels = adaptGrid(grid);
        List<List<PlayerDisplayData.SymbolCell>> stoppedReels = adaptGrid(
                expandingResult.getStoppedGrid());
        List<Integer> expansionReels = expandingResult.getExpandedBannerReels().stream()
                .map(reel -> reel - 1)
                .sorted()
                .toList();
        List<PlayerDisplayData.Win> wins = expandingResult.getLineWins().stream()
                .map(this::adaptWin)
                .toList();
        String bannerDetails = expandingResult.getExpandedBannerReels().isEmpty()
                ? "none" : expandingResult.getExpandedBannerReels().toString();
        String multiplierDetails = expandingResult.getBannerMultipliersByReel().isEmpty()
                ? "none" : expandingResult.getBannerMultipliersByReel().toString();
        String details = String.format(java.util.Locale.ROOT,
                "Set %d  •  scatters %d  •  expanded reels %s  •  banner multipliers %s",
                result.getSetIndex(), expandingResult.getScatterCount(),
                bannerDetails, multiplierDetails);
        return new PlayerDisplayData.Spin(mode, finalReels, stoppedReels, expansionReels,
                result.getWin(), wins, details);
    }

    private List<List<PlayerDisplayData.SymbolCell>> adaptGrid(ReelGrid grid) {
        List<List<PlayerDisplayData.SymbolCell>> reels = new ArrayList<>();
        for (int reel = 0; reel < grid.getReelCount(); reel++) {
            List<PlayerDisplayData.SymbolCell> rows = new ArrayList<>();
            for (int row = 0; row < grid.getHeight(); row++) {
                rows.add(adaptSymbol(grid.getSymbol(reel, row)));
            }
            reels.add(rows);
        }
        return List.copyOf(reels);
    }

    private PlayerDisplayData.Win adaptWin(ExpandingWildLineWin lineWin) {
        var payline = config.paylines.get(lineWin.paylineNumber() - 1);
        List<PlayerDisplayData.Position> positions = new ArrayList<>();
        for (int reel = 0; reel < lineWin.matchingReels(); reel++) {
            positions.add(new PlayerDisplayData.Position(reel, payline.getRow(reel)));
        }
        String description = lineWin.matchingReels() + "oak " + lineWin.symbol().id();
        if (lineWin.multiplier() > 1.0) {
            description += String.format(java.util.Locale.ROOT,
                    " (%.1fx line multiplier)", lineWin.multiplier());
        }
        return new PlayerDisplayData.Win(description, lineWin.totalWin(), positions);
    }

    private PlayerDisplayData.SymbolCell adaptSymbol(Symbol symbol) {
        String id = symbol.id();
        String label = switch (id) {
            case "WILD" -> "W";
            case "SCATTER" -> "S";
            case "BANNER" -> "B";
            default -> id;
        };
        return cell(label);
    }

    private static PlayerDisplayData.SymbolCell cell(String label) {
        return new PlayerDisplayData.SymbolCell(label, label);
    }
}
