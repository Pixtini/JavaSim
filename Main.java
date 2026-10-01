import config.SimConfig;
import config.GameConfig;

public class Main {

    public static void main(String[] args) {

        SimConfig simConfig = new SimConfig();
        GameConfig gameConfig = new GameConfig();

        standardStats statT = new standardStats();
        standardStats statB = new standardStats();
        standardStats statF = new standardStats();

        for (long i = 0; i < simConfig.rounds; i++) {

            gameBasicBase baseGame = new gameBasicBase();

            spinResult result = baseGame.spin();

            statB.addResult(result);

            double totalWin = result.win;

            if (result.freeSpinFlag){
                
                statF.freegameTriggers++;    

                for (int j = 0; j < gameConfig.freeSpinAmount; j++){
                    
                    gameBasicFree freeGame = new gameBasicFree();
                    spinResult resultFree = freeGame.spin();
                    
                    statF.addResult(result);
                    totalWin += resultFree.win;
                }
            }
            
            statT.winDist(totalWin);
        }
        
        print printer = new print(statB, statF, statT);
        printer.printToConsole();
    }
}
