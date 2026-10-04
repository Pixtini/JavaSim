import game.Game;
import game.GameFactory;
import simulation.config.SimConfig;
import simulation.engine.SimulationRunner;
import simulation.reporting.Print;
import simulation.result.SimulationResult;

public class Main {

    public static void main(String[] args) {
        SimConfig simConfig = new SimConfig();
        Game game = GameFactory.create(simConfig.gameId);

        SimulationResult result = new SimulationRunner(
                simConfig, game).run();
        Print printer = new Print(result);
        printer.printToConsole(simConfig);
    }
}
