import java.util.Random;

public class MonteCarloExample {

    public static void main(String[] args) {

        long spins = 10_000_000;
        double stake = 1.0;
        double totalWinnings = 0.0;

        for (long i = 0; i < spins; i++) {

            spinResult result = gameBasic.spin();

            totalWinnings += result.win;
        }

        double totalStaked = spins * stake;
        double rtp = totalWinnings / totalStaked;

        System.out.println("Spins: " + spins);
        System.out.println("Total staked: £" + totalStaked);
        System.out.println("Total winnings: £" + totalWinnings);

        System.out.println();
        System.out.printf("Simulated RTP: %.4f%%%n", rtp * 100);
    }
}