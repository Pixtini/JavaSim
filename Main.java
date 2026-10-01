import game.config.GameConfig;
import game.proxy.BasicProxyGame;
import simulation.config.SimConfig;
import simulation.engine.SimulationRunner;
import simulation.reporting.Print;
import simulation.result.SimulationResult;

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
