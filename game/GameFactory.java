package game;

import game.proxy.BasicProxyConfig;
import game.proxy.BasicProxyGame;

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
            default -> throw new IllegalArgumentException("Unknown game ID: " + gameId);
        };
    }
}
