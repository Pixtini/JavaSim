import java.util.Random;

public class gameBasic {

    public static spinResult spin() {

        Random random = new Random();

        int smallWins = 0;
        int mediumWins = 0;
        int bigWins = 0;

        double roll = random.nextDouble();
        double win;

        if (roll < 0.40) {
            win = 0.0;

        } else if (roll < 0.60) {
            win = 5.0;
            smallWins++;

        } else if (roll < 0.99) {
            win = 10.0;
            mediumWins++;
 
        } else {
            win = 50.0;
            bigWins++;
        }
        
        return new spinResult(win, smallWins, mediumWins, bigWins);

        }

    }