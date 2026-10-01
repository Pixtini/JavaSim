package game.proxy;

import java.util.Random;
import game.config.GameConfig;
import game.model.SpinResult;

public class GameBasicBase extends GameBasic {

    public GameBasicBase(GameConfig gameConfig, Random random) {
        super(gameConfig, random);
    }

    @Override
    public SpinResult spin() {
        double roll = nextRoll();
        SpinResult result = spinForRoll(roll);

        int firstDigit = (int) (roll * 10);
        boolean triggersFreeSpins = firstDigit % 2 == 1;
        return new SpinResult(result.getWin(), result.getWinSize(), triggersFreeSpins);
    }
}
