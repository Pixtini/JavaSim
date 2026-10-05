package toolkit.viewer;

import game.Game;
import game.GameFactory;
import java.util.Random;

/** Command-line entry point for finding and displaying a winning game screen. */
public final class ViewerMain {
    private static final long DEFAULT_SPIN_LIMIT = 100;
    private static final double DEFAULT_STAKE = 1.0;

    private ViewerMain() {}

    public static void main(String[] args) {
        if (args.length < 1 || args.length > 3) {
            printUsage();
            return;
        }

        try {
            long spinLimit = args.length >= 2
                    ? Long.parseLong(args[1]) : DEFAULT_SPIN_LIMIT;
            double stake = args.length >= 3
                    ? Double.parseDouble(args[2]) : DEFAULT_STAKE;
            Game game = GameFactory.create(args[0]);
            WinFinder.Result result = new WinFinder(game, new Random(), stake, spinLimit).find();
            if (result.foundWin()) {
                WinScreenPrinter.print(result, stake, System.out);
            } else {
                System.out.printf("Win could not be found within %d spins.%n", spinLimit);
            }
        } catch (NumberFormatException exception) {
            System.err.println("Spin limit must be an integer and stake must be numeric.");
            printUsage();
        } catch (IllegalArgumentException exception) {
            System.err.println(exception.getMessage());
            printUsage();
        }
    }

    private static void printUsage() {
        System.out.println("Usage: java toolkit.viewer.ViewerMain <game-id> [max-spins] [stake]");
        System.out.println("Example: java toolkit.viewer.ViewerMain expanding-wild 100 1.0");
    }
}
