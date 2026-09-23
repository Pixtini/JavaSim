import java.util.Random;

public class MonteCarloExample {

    public static void main(String[] args) {

        long spinCount = 10_000_000;

        standardStats stat = new standardStats(spinCount, 1.0, 0.0);

        for (long i = 0; i < spinCount; i++) {

            spinResult result = gameBasic.spin();

            stat.addWin(result.win);
        }


        stat.calculateStats();
        stat.printStats();


        ///
        ///double totalStaked = spins * stake;
        ///double rtp = totalWinnings / totalStaked;

        ///System.out.println("Spins: " + spins);
        ///System.out.println("Total staked: £" + totalStaked);
        ///System.out.println("Total winnings: £" + totalWinnings);

        ///System.out.println();
        ///System.out.printf("Simulated RTP: %.4f%%%n", rtp * 100);
        
    }
}