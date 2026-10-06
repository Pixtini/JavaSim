package toolkit.player;

import java.util.List;

/** Adapts a game's normal play session into data a generic player can display. */
public interface PlayerGameAdapter {
    String gameName();

    PlayerDisplayData.Round spin(double stake);

    List<PlayerDisplayData.SymbolCell> symbolPalette();
}
