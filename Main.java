import config.GameConfig;
import config.SimConfig;

public class Main {

    public static void main(String[] args) {
        SimConfig simConfig = new SimConfig();
        GameConfig gameConfig = new GameConfig();

        SimulationResult result = new SimulationRunner(simConfig, gameConfig).run();
        print printer = new print(
                result.getBaseGameStats(),
                result.getFreeGameStats(),
                result.getTotalGameStats());
        printer.printToConsole(simConfig);
    }
}
