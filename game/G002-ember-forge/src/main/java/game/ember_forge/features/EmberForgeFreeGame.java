package game.ember_forge.features;

import GameModuleFramework.reels.*;
import game.ember_forge.EmberForgeEvaluator;
import game.ember_forge.config.EmberForgeConfig;
import game.ember_forge.model.EmberForgeSpin;
import java.util.*;

/** One eight-spin feature; wins increase heat for the following spin, losses preserve it. */
public final class EmberForgeFreeGame {
    private final List<ReelStrip> reels;
    private final EmberForgeEvaluator evaluator;
    public EmberForgeFreeGame(EmberForgeConfig config) {
        reels = config.strips(config.freeReels());
        evaluator = new EmberForgeEvaluator(config);
    }
    public EmberForgeSpin spin(Random random, double stake, int heat, boolean capture) {
        var stopped = ReelGrid.spinWithStops(reels, 3, random);
        return evaluator.evaluate(stopped.grid(), stake, 0, true, heat, stopped.stops(), capture);
    }
    public EmberForgeSpin evaluate(int[] stops, double stake, int heat, boolean capture) {
        if (heat < 1 || heat > 5) throw new IllegalArgumentException("Invalid G002 heat");
        return evaluator.evaluate(ReelGrid.atStops(reels, 3, stops), stake, 0,
                true, heat, stops, capture);
    }
}
