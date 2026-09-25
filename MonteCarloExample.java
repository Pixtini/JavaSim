import config.SimConfig;

public class MonteCarloExample {

    public static void main(String[] args) {

        SimConfig simConfig = new SimConfig();

        standardStats statB = new standardStats(0, simConfig.stake, 0.0, new int[]{0,0,0,0});
        standardStats statF = new standardStats(0, simConfig.stake, 0.0, new int[]{0,0,0,0});


        for (long i = 0; i < simConfig.spins; i++) {

            gameBasicBase baseGame = new gameBasicBase();

            spinResult result = baseGame.spin();

            statB.addWin(result.win);
            statB.addPaytable(result.winSize);

            if (result.freeSpinFlag){
                gameBasicFree freeGame = new gameBasicFree();

                spinResult resultFree = freeGame.spin();

                statF.addWin(resultFree.win);
                statF.addPaytable(resultFree.winSize);
            }
        }
        
        print printer = new print(statB, statF);
        printer.printToConsole(simConfig.spins, simConfig.stake);


    }
}
