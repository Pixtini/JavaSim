package game;

import game.proxy.BasicProxyConfig;
import game.proxy.BasicProxyGame;
import game.expandingwild.ExpandingWildGame;
import game.expandingwild.config.ExpandingWildParParser;
import game.walkingwild.WalkingWildGame;
import game.walkingwild.config.WalkingWildParParser;
import java.io.IOException;
import java.nio.file.Path;

/** Selects a game implementation and constructs it with that game's configuration. */
public final class GameFactory {
    private GameFactory() {
    }

    public static Game create(String gameId) {
        if (gameId == null) {
            throw new IllegalArgumentException("Game ID must not be null");
        }

        return switch (gameId) {
            case "basic-proxy" -> new BasicProxyGame(new BasicProxyConfig());
            case "expanding-wild" -> createDefaultExpandingWild();
            case "G001-walkingwild" -> createDefaultWalkingWild();
            default -> throw new IllegalArgumentException("Unknown game ID: " + gameId);
        };
    }

    private static Game createDefaultExpandingWild() {
        try {
            return create("expanding-wild",
                    Path.of("game", "expandingwild", "expandingWildPAR.xlsx"));
        } catch (IOException exception) {
            throw new IllegalStateException("Could not load the default Expanding Wild PAR workbook", exception);
        }
    }

    private static Game createDefaultWalkingWild() {
        try {
            return create("G001-walkingwild",
                    Path.of("game", "G001-walkingwild", "source", "G001-walkingWildsPAR.xlsx"),
                    "Normal");
        } catch (IOException exception) {
            throw new IllegalStateException("Could not load the default G001 walkingWild PAR workbook", exception);
        }
    }

    /** Constructs a PAR-backed game and rejects workbook formats unsupported by that game. */
    public static Game create(String gameId, Path parWorkbookPath) throws IOException {
        return create(gameId, parWorkbookPath, "Normal");
    }

    /** Constructs a PAR-backed game from one isolated workbook mode. */
    public static Game create(String gameId, Path parWorkbookPath, String gameMode) throws IOException {
        if (gameId == null) {
            throw new IllegalArgumentException("Game ID must not be null");
        }
        if (parWorkbookPath == null) {
            throw new IllegalArgumentException("Select a PAR workbook before starting a simulation");
        }
        if (gameMode == null || gameMode.isBlank()) {
            throw new IllegalArgumentException("Simulation mode must not be blank");
        }
        return switch (gameId) {
            case "expanding-wild" -> new ExpandingWildGame(
                    new ExpandingWildParParser().parse(parWorkbookPath, gameMode));
            case "G001-walkingwild" -> new WalkingWildGame(
                    new WalkingWildParParser().parse(parWorkbookPath, gameMode));
            default -> throw new IllegalArgumentException(
                    "No PAR workbook parser is registered for game '" + gameId + "'");
        };
    }
}
