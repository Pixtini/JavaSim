package game.ember_forge.model;

import GameModuleFramework.paylines.EvaluatedLineWin;
import GameModuleFramework.reels.ReelGrid;
import game.model.ReplayEvent;
import game.model.SpinResult;
import java.util.*;

/** Detailed screen and heat state; clipping retains visual and accounting consistency. */
public final class EmberForgeSpin extends SpinResult {
    private final ReelGrid grid;
    private final List<EvaluatedLineWin> lineWins;
    private final int heat;
    private final int nextHeat;
    public EmberForgeSpin(double win, boolean trigger, int set, ReelGrid grid,
            List<EvaluatedLineWin> lineWins, int heat, int nextHeat, ReplayEvent event) {
        super(win, win > 0 ? 1 : 0, trigger, List.of(), set, event);
        this.grid = grid; this.lineWins = List.copyOf(lineWins);
        this.heat = heat; this.nextHeat = nextHeat;
    }
    public ReelGrid grid() { return grid; }
    public List<EvaluatedLineWin> lineWins() { return lineWins; }
    public int heat() { return heat; }
    public int nextHeat() { return nextHeat; }
    @Override public EmberForgeSpin cappedAt(double maximum) {
        if (getWin() <= maximum) return this;
        double remaining = maximum;
        List<EvaluatedLineWin> clipped = new ArrayList<>();
        for (var win : lineWins) {
            if (remaining <= 0) break;
            double value = Math.min(remaining, win.totalWin());
            clipped.add(new EvaluatedLineWin(win.paylineNumber(), win.candidate(),
                    value / win.multiplier(), win.multiplier(), value));
            remaining -= value;
        }
        return new EmberForgeSpin(maximum, hasFreeSpin(), getSetIndex(), grid, clipped,
                heat, nextHeat, getReplayEvent().map(e -> e.withWin(maximum)).orElse(null));
    }
}
