package GameModuleFramework.paylines;

/** A left-to-right line expressed as one visible row per reel. */
public final class Payline {
    private final int[] rowsByReel;

    public Payline(int... rowsByReel) {
        if (rowsByReel.length == 0) {
            throw new IllegalArgumentException("A payline must visit at least one reel");
        }
        this.rowsByReel = rowsByReel.clone();
        for (int row : this.rowsByReel) {
            if (row < 0) {
                throw new IllegalArgumentException("Payline rows cannot be negative");
            }
        }
    }

    public int getReelCount() {
        return rowsByReel.length;
    }

    public int getRow(int reel) {
        return rowsByReel[reel];
    }

    public int[] getRowsByReel() {
        return rowsByReel.clone();
    }
}
