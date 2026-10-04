package game;

import java.util.Random;
import java.util.List;

/** Supplies metadata and independent sessions for a game implementation. */
public interface Game {
    /** Returns a new session using the supplied deterministic random stream. */
    GameSession createSession(Random random);

    /** Returns the paytable bucket shape used by the simulation statistics. */
    int[] getPaytable();

    /** Returns optional named award categories for multi-award games. */
    default List<String> getAwardLabels() {
        return List.of();
    }
}
