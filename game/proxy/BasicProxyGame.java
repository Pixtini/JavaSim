package game.proxy;

import config.GameConfig;
import game.Game;
import game.GameSession;
import java.util.Random;

/** Probability-based example game, separate from the simulation engine. */
public final class BasicProxyGame implements Game {
    private final GameConfig gameConfig;

    public BasicProxyGame(GameConfig gameConfig) {
        this.gameConfig = gameConfig;
    }

    @Override
    public GameSession createSession(Random random) {
        return new BasicProxyGameSession(gameConfig, random);
    }

    @Override
    public int[] getPaytable() {
        return gameConfig.paytable.clone();
    }
}
