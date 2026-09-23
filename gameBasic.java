import java.util.Random;
import config.GameConfig;

public class gameBasic {

    public static spinResult spin() {

        Random random = new Random();

        GameConfig gameConfig = new GameConfig();

        int smallWins = 0;
        int mediumWins = 0;
        int bigWins = 0;

        double roll = random.nextDouble();
        double win;

        if (roll < gameConfig.smallWinChance) {
            win = 0.0;

        } else if (roll < gameConfig.mediumWinChance) {
            win = gameConfig.smallWin;
            smallWins++;

        } else if (roll < gameConfig.bigWinChance) {
            win = gameConfig.mediumWin;
            mediumWins++;
 
        } else {
            win = gameConfig.bigWin;
            bigWins++;
        }
        
        return new spinResult(win, smallWins, mediumWins, bigWins);

        }

    }