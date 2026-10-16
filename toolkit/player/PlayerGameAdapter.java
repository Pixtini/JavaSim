package toolkit.player;

import java.util.List;
import game.model.GameRoundResult;

/** Adapts a game's normal play session into data a generic player can display. */
public interface PlayerGameAdapter {
    String gameName();

    default String modeName() { return "Normal"; }

    default double costMultiplier() { return 1.0; }

    PlayerDisplayData.Round spin(double stake);

    default PlayerDisplayData.Round adaptRound(GameRoundResult result) {
        throw new UnsupportedOperationException("This player adapter cannot display recorded rounds");
    }

    List<PlayerDisplayData.SymbolCell> symbolPalette();
}
