package game.model;

import java.util.List;
import java.util.Optional;

public class SpinResult {

    private final double win;
    private final int winSize;
    private final boolean freeSpinFlag;
    private final List<String> awards;
    private final int setIndex;
    private final ReplayEvent replayEvent;

    public SpinResult(double win, int winSize, boolean freeSpinFlag) {
        this(win, winSize, freeSpinFlag, List.of());
    }

    public SpinResult(double win, int winSize, boolean freeSpinFlag, List<String> awards) {
        this(win, winSize, freeSpinFlag, awards, -1);
    }

    public SpinResult(double win, int winSize, boolean freeSpinFlag,
            List<String> awards, int setIndex) {
        this(win, winSize, freeSpinFlag, awards, setIndex, null);
    }

    public SpinResult(double win, int winSize, boolean freeSpinFlag,
            List<String> awards, int setIndex, ReplayEvent replayEvent) {
        this.win = win;
        this.winSize = winSize;
        this.freeSpinFlag = freeSpinFlag;
        this.awards = List.copyOf(awards);
        this.setIndex = setIndex;
        this.replayEvent = replayEvent;
    }

    public double getWin() {
        return win;
    }

    public int getWinSize() {
        return winSize;
    }

    public boolean hasFreeSpin() {
        return freeSpinFlag;
    }

    /** One label per award occurrence on this spin. Repeated labels are separate awards. */
    public List<String> getAwards() {
        return awards;
    }

    /** Selected reel-set index, or -1 when the game has no set selection. */
    public int getSetIndex() {
        return setIndex;
    }

    public Optional<ReplayEvent> getReplayEvent() {
        return Optional.ofNullable(replayEvent);
    }

    /** Returns this outcome limited to the supplied payout amount. */
    public SpinResult cappedAt(double maximumWin) {
        if (win <= maximumWin) {
            return this;
        }
        return new SpinResult(maximumWin, maximumWin > 0.0 ? winSize : 0,
                freeSpinFlag, awards, setIndex,
                replayEvent == null ? null : replayEvent.withWin(maximumWin));
    }
}
