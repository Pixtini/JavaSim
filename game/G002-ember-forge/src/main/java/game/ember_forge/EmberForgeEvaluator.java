package game.ember_forge;

import GameModuleFramework.paylines.LineWinSelectionPolicy;
import GameModuleFramework.paylines.PaylineWinCalculator;
import GameModuleFramework.reels.ReelGrid;
import game.ember_forge.config.EmberForgeConfig;
import game.ember_forge.model.EmberForgeSpin;
import game.model.ReplayEvent;
import java.util.Arrays;
import java.util.stream.Collectors;

/** Uses shared highest-payout line mathematics; owns only G002 result/heat adaptation. */
public final class EmberForgeEvaluator {
    private final EmberForgeConfig config;
    private final PaylineWinCalculator calculator;
    public EmberForgeEvaluator(EmberForgeConfig config) {
        this.config = config;
        calculator = new PaylineWinCalculator(config.paytable(), config.paylines(),
                EmberForgeConfig.WILD, LineWinSelectionPolicy.highestPayout());
    }
    public EmberForgeSpin evaluate(ReelGrid grid, double stake, int set, boolean free,
            int heat, int[] stops, boolean capture) {
        var wins = calculator.calculate(grid, stake, ignored -> heat);
        double total = wins.stream().mapToDouble(w -> w.totalWin()).sum();
        int next = free && total > 0 ? Math.min(config.maxHeat(), heat + 1) : heat;
        boolean trigger = !free && set == 1;
        ReplayEvent event = capture ? new ReplayEvent(free ? "emberforge-free" : "emberforge-base",
                total, "v1;" + set + ";" + Arrays.stream(stops).mapToObj(Integer::toString)
                        .collect(Collectors.joining(","))) : null;
        return new EmberForgeSpin(total, trigger, set, grid, wins, heat, next, event);
    }
}
