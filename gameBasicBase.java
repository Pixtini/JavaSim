import java.util.Random;
import config.GameConfig;

public class gameBasicBase extends gameBasic {

    @Override
    public spinResult spin() {
        spinResult result = super.spin();

        int firstDigit = (int) (roll * 10);
        boolean isOdd = firstDigit % 2 == 1;

        return new spinResult(result.getWin(), result.getWinSize(), isOdd);
    }

}
