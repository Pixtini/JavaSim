package game.walkingwild.model;

import GameModuleFramework.reels.ReelGrid;
import game.model.ReplayEvent;
import game.model.SpinResult;
import java.util.ArrayList;
import java.util.List;

/** Detailed G001 spin outcome while retaining the simulator's shared spin contract. */
public final class WalkingWildSpin extends SpinResult {
    private final ReelGrid grid;
    private final int scatterCount;
    private final List<WildPosition> wildPositions;
    private final List<WalkingWildLineWin> lineWins;

    public WalkingWildSpin(double win, boolean triggersFreeGames, int setIndex,
            ReelGrid grid, int scatterCount, List<WildPosition> wildPositions,
            List<WalkingWildLineWin> lineWins, String replayType,
            WalkingWildReplayPayload replayPayload) {
        this(win, triggersFreeGames, setIndex, grid, scatterCount, wildPositions,
                lineWins, replayType == null ? null : new ReplayEvent(replayType, win,
                        replayPayload.toPayload()));
    }

    private WalkingWildSpin(double win, boolean triggersFreeGames, int setIndex,
            ReelGrid grid, int scatterCount, List<WildPosition> wildPositions,
            List<WalkingWildLineWin> lineWins, ReplayEvent replayEvent) {
        super(win, win > 0.0 ? 1 : 0, triggersFreeGames, List.of(), setIndex, replayEvent);
        this.grid = grid;
        this.scatterCount = scatterCount;
        this.wildPositions = List.copyOf(wildPositions);
        this.lineWins = List.copyOf(lineWins);
    }

    /** Keeps the old game-specific wrapper accessor convenient for Basegame and Freegame code. */
    public WalkingWildSpin result() { return this; }
    public ReelGrid grid() { return grid; }
    public int scatterCount() { return scatterCount; }
    public List<WildPosition> wildPositions() { return wildPositions; }
    public List<WalkingWildLineWin> lineWins() { return lineWins; }

    @Override
    public WalkingWildSpin cappedAt(double maximumWin) {
        if (getWin() <= maximumWin) return this;
        double remaining = maximumWin;
        List<WalkingWildLineWin> clippedWins = new ArrayList<>();
        for (WalkingWildLineWin lineWin : lineWins) {
            if (remaining <= 0.0) break;
            double clipped = Math.min(remaining, lineWin.totalWin());
            clippedWins.add(new WalkingWildLineWin(lineWin.paylineNumber(), lineWin.symbol(),
                    lineWin.matchingReels(), clipped, lineWin.rowsByReel()));
            remaining -= clipped;
        }
        ReplayEvent event = getReplayEvent().map(value -> value.withWin(maximumWin)).orElse(null);
        return new WalkingWildSpin(maximumWin, hasFreeSpin(), getSetIndex(), grid,
                scatterCount, wildPositions, clippedWins, event);
    }
}
