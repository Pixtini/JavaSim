package game.ember_forge;

import game.Game;
import game.GameSession;
import game.ember_forge.config.EmberForgeConfig;
import java.util.Random;

/** G002 metadata and independent sessions; simulation concerns remain in the shared runner. */
public final class EmberForgeGame implements Game {
    private final EmberForgeConfig config;
    public EmberForgeGame(EmberForgeConfig config) { this.config = java.util.Objects.requireNonNull(config); }
    @Override public GameSession createSession(Random random) { return new EmberForgeSession(config, random); }
    @Override public int[] getPaytable() { return new int[] {0, 0}; }
    @Override public int getBaseGameSetCount() { return 2; }
    @Override public int getFreeGameSetCount() { return 1; }
    @Override public double getMaxWinMultiplier() { return config.maxWinMultiplier(); }
    @Override public double getCostMultiplier() { return config.costMultiplier(); }
    public EmberForgeConfig config() { return config; }
}
