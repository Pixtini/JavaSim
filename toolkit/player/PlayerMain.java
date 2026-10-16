package toolkit.player;

import javax.swing.SwingUtilities;

/** Standalone launch point for the game launcher and balancing player. */
public final class PlayerMain {
    private PlayerMain() {}

    public static void main(String[] args) {
        SwingUtilities.invokeLater(PrototypePlayerFrame::open);
    }
}
