package game.model;

public class SpinResult {

    private final double win;
    private final int winSize;
    private final boolean freeSpinFlag;

    public SpinResult(double win, int winSize, boolean freeSpinFlag) {
        this.win = win;
        this.winSize = winSize;
        this.freeSpinFlag = freeSpinFlag;
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
}
