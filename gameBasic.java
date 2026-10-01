import java.util.Random;
import config.GameConfig;

public class gameBasic {

    protected final GameConfig gameConfig;
    private final Random random;

    public gameBasic(GameConfig gameConfig, Random random) {
        this.gameConfig = gameConfig;
        this.random = random;
    }

    public spinResult spin() {
        return spinForRoll(nextRoll());
    }

    protected final double nextRoll() {
        return random.nextDouble();
    }

    protected final spinResult spinForRoll(double roll) {
        int winSize;
        double win;

        if (roll < gameConfig.winChanceThresholds[0]) {
            win = 0.0;
            winSize = 0;
        } else if (roll < gameConfig.winChanceThresholds[1]) {
            win = gameConfig.winSize[0];
            winSize = 1;
        } else if (roll < gameConfig.winChanceThresholds[2]) {
            win = gameConfig.winSize[1];
            winSize = 2;
        } else {
            win = gameConfig.winSize[2];
            winSize = 3;
        }

        return new spinResult(win, winSize, false);
    }
}
