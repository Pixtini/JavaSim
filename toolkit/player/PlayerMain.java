package toolkit.player;

import javax.swing.SwingUtilities;

/** Standalone launch point for the balancing prototype player. */
public final class PlayerMain {
    private PlayerMain() {}

    public static void main(String[] args) {
        SwingUtilities.invokeLater(PrototypePlayerFrame::open);
    }
}
