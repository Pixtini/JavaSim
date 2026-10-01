import config.GameConfig;
import config.SimConfig;

public class Main {

    public static void main(String[] args) {
        SimConfig simConfig = new SimConfig();
        GameConfig gameConfig = new GameConfig();

        SimulationResult result = new SimulationRunner(simConfig, gameConfig).run();
        Print printer = new Print(result);
        printer.printToConsole(simConfig);
    }
}
