package game.proxy;

import config.GameConfig;
import game.GameSession;
import java.util.ArrayList;
import java.util.Random;
import model.GameRoundResult;
import model.SpinResult;

/** Runs one base spin and, when triggered, the proxy game's free spins. */
final class BasicProxyGameSession implements GameSession {
    private final GameConfig gameConfig;
    private final GameBasicBase baseGame;
    private final GameBasicFree freeGame;

    BasicProxyGameSession(GameConfig gameConfig, Random random) {
        this.gameConfig = gameConfig;
        this.baseGame = new GameBasicBase(gameConfig, random);
        this.freeGame = new GameBasicFree(gameConfig, random);
    }

    @Override
    public GameRoundResult playRound() {
        SpinResult baseResult = baseGame.spin();
        var freeResults = new ArrayList<SpinResult>();
        if (baseResult.hasFreeSpin()) {
            for (int freeSpin = 0; freeSpin < gameConfig.freeSpinAmount; freeSpin++) {
                freeResults.add(freeGame.spin());
            }
        }
        return new GameRoundResult(baseResult, freeResults);
    }
}
