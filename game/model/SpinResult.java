package game.model;

import java.util.List;

public class SpinResult {

    private final double win;
    private final int winSize;
    private final boolean freeSpinFlag;
    private final List<String> awards;

    public SpinResult(double win, int winSize, boolean freeSpinFlag) {
        this(win, winSize, freeSpinFlag, List.of());
    }

    public SpinResult(double win, int winSize, boolean freeSpinFlag, List<String> awards) {
        this.win = win;
        this.winSize = winSize;
        this.freeSpinFlag = freeSpinFlag;
        this.awards = List.copyOf(awards);
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
}
