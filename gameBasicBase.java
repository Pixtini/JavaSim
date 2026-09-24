import java.util.Random;
import config.GameConfig;

public class gameBasicBase extends gameBasic {

    @Override
    public spinResult spin() {
        spinResult result = super.spin();

        int firstDigit = (int) (roll * 10);
        boolean isOdd = firstDigit % 2 == 1;

        if (isOdd){
            result.freeSpinFlag = true;
        }
        
        // Use isOdd here
        return result;
    }

}

