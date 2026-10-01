package game;

import java.util.Random;

/** Supplies metadata and independent sessions for a game implementation. */
public interface Game {
    /** Returns a new session using the supplied deterministic random stream. */
    GameSession createSession(Random random);

    /** Returns the paytable bucket shape used by the simulation statistics. */
    int[] getPaytable();
}
