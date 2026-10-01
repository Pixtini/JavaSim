import engine.SimulationRunner;
import config.GameConfig;
import config.SimConfig;
import game.proxy.BasicProxyGame;
import reporting.Print;
import result.SimulationResult;

public class Main {

    public static void main(String[] args) {
        SimConfig simConfig = new SimConfig();
        GameConfig gameConfig = new GameConfig();

        SimulationResult result = new SimulationRunner(
                simConfig, new BasicProxyGame(gameConfig)).run();
        Print printer = new Print(result);
        printer.printToConsole(simConfig);
    }
}
