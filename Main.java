import config.SimConfig;
import config.GameConfig;

public class Main {

    public static void main(String[] args) {

        SimConfig simConfig = new SimConfig();
        GameConfig gameConfig = new GameConfig();

        standardStats statT = new standardStats(simConfig.stake, gameConfig.paytable);
        standardStats statB = new standardStats(simConfig.stake, gameConfig.paytable);
        standardStats statF = new standardStats(simConfig.stake, gameConfig.paytable);

        for (long i = 0; i < simConfig.rounds; i++) {

            gameBasicBase baseGame = new gameBasicBase();

            spinResult result = baseGame.spin();

            statB.addResult(result);
            statB.recordWinInDistribution(result.getWin());

            double totalWin = result.getWin();

            if (result.hasFreeSpin()){
                
                statF.recordFreegameTrigger();
                double freeGameWin = 0.0;

                for (int j = 0; j < gameConfig.freeSpinAmount; j++){
                    
                    gameBasicFree freeGame = new gameBasicFree();
                    spinResult resultFree = freeGame.spin();
                    
                    statF.addResult(resultFree);
                    freeGameWin += resultFree.getWin();
                    totalWin += resultFree.getWin();
                }

                statF.recordWinInDistribution(freeGameWin);
            }
            
            statT.recordWinInDistribution(totalWin);
        }
        
        print printer = new print(statB, statF, statT);
        printer.printToConsole(simConfig);
    }
}
