package toolkit.reelset;

import game.ExhaustiveReelGame;
import game.Game;
import game.GameFactory;

/** Command-line entry point for a complete reel-stop combination run. */
public final class FullReelsetMain {
    private static final double DEFAULT_STAKE = 1.0;
    private static final int DEFAULT_WORKERS = Runtime.getRuntime().availableProcessors();

    private FullReelsetMain() {}

    public static void main(String[] args) {
        if (args.length < 1 || args.length > 3) {
            printUsage();
            return;
        }

        try {
            double stake = args.length >= 2 ? Double.parseDouble(args[1]) : DEFAULT_STAKE;
            int workers = args.length >= 3 ? Integer.parseInt(args[2]) : DEFAULT_WORKERS;
            Game selectedGame = GameFactory.create(args[0]);
            if (!(selectedGame instanceof ExhaustiveReelGame reelGame)) {
                throw new IllegalArgumentException(
                        "Game does not support exhaustive reel-stop evaluation: " + args[0]);
            }

            long combinations = FullReelsetSimulator.countCombinations(reelGame);
            long weightedCombinations = FullReelsetSimulator.countWeightedCombinations(reelGame);
            System.out.printf("Game: %s%n", args[0]);
            System.out.printf("Reel sets: %s%n", reelStops(reelGame));
            System.out.printf("Set-stop outcomes evaluated: %d%n", combinations);
            System.out.printf("Selector-weighted outcome count: %d%n", weightedCombinations);
            System.out.printf("Evaluating with %d workers...%n", workers);

            ReelsetSimulationResult result = new FullReelsetSimulator()
                    .simulate(reelGame, stake, workers);
            ReelsetReportPrinter.print(result, System.out);
        } catch (NumberFormatException exception) {
            System.err.println("Stake must be numeric and worker count must be an integer.");
            printUsage();
        } catch (IllegalArgumentException | ArithmeticException exception) {
            System.err.println(exception.getMessage());
            printUsage();
        }
    }

    private static String reelStops(ExhaustiveReelGame game) {
        StringBuilder text = new StringBuilder();
        for (int set = 0; set < game.getReelSetCount(); set++) {
            if (set > 0) {
                text.append("; ");
            }
            text.append("set ").append(set).append(" (weight ")
                    .append(game.getReelSetWeight(set)).append("): ");
            for (int reel = 0; reel < game.getReelCount(); reel++) {
                if (reel > 0) {
                    text.append(" × ");
                }
                text.append(game.getStopCount(set, reel));
            }
        }
        return text.toString();
    }

    private static void printUsage() {
        System.out.println("Usage: java toolkit.reelset.FullReelsetMain <game-id> [stake] [workers]");
        System.out.println("Example: java toolkit.reelset.FullReelsetMain expanding-wild 1.0 8");
    }
}
