import java.util.Random;
import config.GameConfig;

public class gameBasic {

    public static spinResult spin() {

        Random random = new Random();

        GameConfig gameConfig = new GameConfig();

        int winSize = 0;

        double roll = random.nextDouble();
        double win;

        if (roll < gameConfig.winChanceThresholds[0]) {
            win = 0.0;

        } else if (roll < gameConfig.winChanceThresholds[1]) {
            win = gameConfig.winSize[0];
            winSize  = winSize + 1;
 
        } else if (roll < gameConfig.winChanceThresholds[2]) {
            win = gameConfig.winSize[1];
            winSize  = winSize + 2;
 
        } else {
            win = gameConfig.winSize[2];
            winSize  = winSize + 3;
        }
        
        return new spinResult(win, winSize);

        }

    }
