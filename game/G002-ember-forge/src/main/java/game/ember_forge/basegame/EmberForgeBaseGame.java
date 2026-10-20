package game.ember_forge.basegame;

import GameModuleFramework.probability.WeightedTable;
import GameModuleFramework.reels.*;
import game.ember_forge.EmberForgeEvaluator;
import game.ember_forge.config.EmberForgeConfig;
import game.ember_forge.model.EmberForgeSpin;
import java.util.*;

/** Selects ordinary or guaranteed three-scatter entry set before drawing reel stops. */
public final class EmberForgeBaseGame {
    private final List<List<ReelStrip>> sets;
    private final WeightedTable<Integer> selector;
    private final EmberForgeEvaluator evaluator;
    public EmberForgeBaseGame(EmberForgeConfig config) {
        sets = List.of(config.strips(config.baseReels()), config.strips(config.entryReels()));
        var entries = new ArrayList<WeightedTable.Entry<Integer>>();
        int trigger = config.triggerWeight();
        if (trigger < config.triggerDenominator())
            entries.add(new WeightedTable.Entry<>(0, config.triggerDenominator() - trigger));
        entries.add(new WeightedTable.Entry<>(1, trigger));
        selector = new WeightedTable<>(entries);
        evaluator = new EmberForgeEvaluator(config);
    }
    public EmberForgeSpin spin(Random random, double stake, boolean capture) {
        int set = selector.draw(random);
        var stopped = ReelGrid.spinWithStops(sets.get(set), 3, random);
        return evaluator.evaluate(stopped.grid(), stake, set, false, 1, stopped.stops(), capture);
    }
    public EmberForgeSpin evaluate(int set, int[] stops, double stake, boolean capture) {
        if (set < 0 || set >= sets.size()) throw new IllegalArgumentException("Invalid G002 BG set");
        return evaluator.evaluate(ReelGrid.atStops(sets.get(set), 3, stops), stake,
                set, false, 1, stops, capture);
    }
}
