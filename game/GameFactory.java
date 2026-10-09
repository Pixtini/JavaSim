package game;

import game.proxy.BasicProxyConfig;
import game.proxy.BasicProxyGame;
import game.expandingwild.ExpandingWildGame;
import game.expandingwild.config.ExpandingWildParParser;
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

    /** Constructs a PAR-backed game and rejects workbook formats unsupported by that game. */
    public static Game create(String gameId, Path parWorkbookPath) throws IOException {
        if (gameId == null) {
            throw new IllegalArgumentException("Game ID must not be null");
        }
        if (parWorkbookPath == null) {
            throw new IllegalArgumentException("Select a PAR workbook before starting a simulation");
        }
        return switch (gameId) {
            case "expanding-wild" -> new ExpandingWildGame(
                    new ExpandingWildParParser().parse(parWorkbookPath));
            default -> throw new IllegalArgumentException(
                    "No PAR workbook parser is registered for game '" + gameId + "'");
        };
    }
}
