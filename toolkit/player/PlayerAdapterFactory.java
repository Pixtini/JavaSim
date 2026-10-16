package toolkit.player;

import game.expandingwild.config.ExpandingWildParParser;
import game.walkingwild.config.WalkingWildParParser;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Random;

/** Builds a player adapter from one selected game and PAR mode. */
public final class PlayerAdapterFactory {
    private PlayerAdapterFactory() {}

    public static PlayerGameAdapter create(String gameId, Path parWorkbook, String mode,
            Random random) throws IOException {
        return switch (gameId) {
            case "expanding-wild" -> new ExpandingWildPlayerAdapter(
                    new ExpandingWildParParser().parse(parWorkbook, mode), random, mode);
            case "G001-walkingwild" -> new WalkingWildPlayerAdapter(
                    new WalkingWildParParser().parse(parWorkbook, mode), random);
            default -> throw new IllegalArgumentException(
                    "The player does not support game '" + gameId + "'");
        };
    }
}
