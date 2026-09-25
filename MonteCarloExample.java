import config.SimConfig;
import config.GameConfig;

public class MonteCarloExample {

    public static void main(String[] args) {

        SimConfig simConfig = new SimConfig();
        GameConfig gameConfig = new GameConfig();

        standardStats statT = new standardStats(0, simConfig.stake, 0.0, new int[]{0,0,0,0});
        standardStats statB = new standardStats(0, simConfig.stake, 0.0, new int[]{0,0,0,0});
        standardStats statF = new standardStats(0, simConfig.stake, 0.0, new int[]{0,0,0,0});


        for (long i = 0; i < simConfig.spins; i++) {

            gameBasicBase baseGame = new gameBasicBase();

            spinResult totalResult = new spinResult(0,0,false);

            spinResult result = baseGame.spin();

            statB.addWin(result.win);
            statB.addPaytable(result.winSize);
            totalResult.win = result.win;

            if (result.freeSpinFlag){
                statF.freespins = statF.freespins + gameConfig.freeSpinAmount;
                for (int j = 0; j < gameConfig.freeSpinAmount; j++){
                    gameBasicFree freeGame = new gameBasicFree();
                    spinResult resultFree = freeGame.spin();
                    statF.addWin(resultFree.win);
                    statF.addPaytable(resultFree.winSize);
                    totalResult.win = totalResult.win + resultFree.win;
                }
            }
            statT.winDist(totalResult.win);
        }
        
        print printer = new print(statB, statF, statT);
        printer.printToConsole(simConfig.spins, simConfig.stake);

    }
}
