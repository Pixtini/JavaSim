import game.Game;
import game.GameFactory;
import simulation.config.SimConfig;
import simulation.engine.SimulationRunner;
import simulation.reporting.Print;
import simulation.result.SimulationResult;
import java.nio.file.Path;

public class Main {

    public static void main(String[] args) throws Exception {
        SimConfig simConfig = new SimConfig();
        simConfig.parWorkbookPath = args.length == 0
                ? Path.of("game", "expandingwild", "expandingWildPAR.xlsx")
                : Path.of(args[0]).toAbsolutePath().normalize();
        Game game = GameFactory.create(simConfig.gameId, simConfig.parWorkbookPath);

        SimulationResult result = new SimulationRunner(
                simConfig, game).run();
        Print printer = new Print(result);
        printer.printToConsole(simConfig);
    }
}
