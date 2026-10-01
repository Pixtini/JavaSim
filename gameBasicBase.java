import java.util.Random;
import config.GameConfig;

public class gameBasicBase extends gameBasic {

    public gameBasicBase(GameConfig gameConfig, Random random) {
        super(gameConfig, random);
    }

    @Override
    public spinResult spin() {
        double roll = nextRoll();
        spinResult result = spinForRoll(roll);

        int firstDigit = (int) (roll * 10);
        boolean triggersFreeSpins = firstDigit % 2 == 1;
        return new spinResult(result.getWin(), result.getWinSize(), triggersFreeSpins);
    }
}
