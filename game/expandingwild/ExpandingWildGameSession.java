package game.expandingwild;

import game.GameSession;
import game.expandingwild.basegame.ExpandingWildBaseGame;
import game.expandingwild.config.ExpandingWildConfig;
import game.expandingwild.features.ExpandingWildFreeGame;
import game.model.GameRoundResult;
import game.model.SpinResult;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Owns one partition-local game session, using only its supplied random stream. */
public final class ExpandingWildGameSession implements GameSession {
    private final ExpandingWildConfig config;
    private final ExpandingWildBaseGame baseGame;
    private final ExpandingWildFreeGame freeGame;

    public ExpandingWildGameSession(ExpandingWildConfig config, Random random) {
        this.config = config;
        this.baseGame = new ExpandingWildBaseGame(config, random);
        this.freeGame = new ExpandingWildFreeGame(config, random);
    }

    @Override
    public GameRoundResult playRound() {
        var baseResult = baseGame.spin();
        List<SpinResult> freeResults = new ArrayList<>();
        if (baseResult.hasFreeSpin()) {
            for (int freeGameIndex = 0; freeGameIndex < config.freeGamesAwarded; freeGameIndex++) {
                freeResults.add(freeGame.spin());
            }
        }
        return new GameRoundResult(baseResult, freeResults);
    }
}
