import java.util.Random;

public class gameBasic {

    public static double spin() {

        Random random = new Random();

        double roll = random.nextDouble();
        double win;

        if (roll < 0.40) {
            win = 0.0;

        } else if (roll < 0.99) {
            win = 5.0;
 
        } else {
            win = 50.0;
        }
        
        return win;

        }

    }