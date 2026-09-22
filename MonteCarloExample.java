import java.util.Random;

public class MonteCarloRTP {

    public static void main(String[] args) {

        Random random = new Random();

        long spins = 10_000_000;
        double stake = 1.0;
        double totalWinnings = 0.0;

        long zeroWins = 0;
        long smallWins = 0;
        long bigWins = 0;

        for (long i = 0; i < spins; i++) {

            double roll = random.nextDouble();
            double win;

            if (roll < 0.95) {
                win = 0.0;
                zeroWins++;

            } else if (roll < 0.99) {
                win = 5.0;
                smallWins++;

            } else {
                win = 50.0;
                bigWins++;
            }

            totalWinnings += win;
        }

        double totalStaked = spins * stake;
        double rtp = totalWinnings / totalStaked;

        System.out.println("Spins: " + spins);
        System.out.println("Total staked: £" + totalStaked);
        System.out.println("Total winnings: £" + totalWinnings);

        System.out.println();
        System.out.println("0x wins: " + zeroWins);
        System.out.println("5x wins: " + smallWins);
        System.out.println("50x wins: " + bigWins);

        System.out.println();
        System.out.printf("Simulated RTP: %.4f%%%n", rtp * 100);
    }
}