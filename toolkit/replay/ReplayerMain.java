package toolkit.replay;

import game.Game;
import game.GameFactory;
import game.GameSession;
import game.model.GameRoundResult;
import simulation.replay.SavedGameplay;
import simulation.replay.SavedGameplayCsv;
import toolkit.viewer.WinScreenPrinter;
import java.nio.file.Path;
import java.nio.file.Files;
import java.io.PrintStream;
import java.util.List;
import java.util.Random;

/** Recalculates one saved gameplay and checks it against the CSV's recorded wins. */
public final class ReplayerMain {
    private ReplayerMain() {}

    public static void main(String[] args) {
        if (args.length != 2) {
            printUsage();
            return;
        }
        try {
            Path csvPath = Path.of(args[0]);
            long gameplayId = Long.parseLong(args[1]);
            replay(csvPath, gameplayId, System.out);
        } catch (NumberFormatException exception) {
            System.err.println("Gameplay ID must be an integer.");
            printUsage();
        } catch (Exception exception) {
            System.err.println("Replay failed: " + exception.getMessage());
            System.exit(1);
        }
    }

    /** Replays and validates a gameplay record using the supplied output stream. */
    public static void replay(Path csvPath, long gameplayId, PrintStream output) throws Exception {
        SavedGameplay saved = SavedGameplayCsv.readById(csvPath, gameplayId);
        Game game = createGameForReplay(saved.gameId(), saved.gameMode(), csvPath);
        GameSession session = game.createSession(new Random(0));
        GameRoundResult replayed = session.replayRound(saved.stake(),
                game.getMaxWinMultiplier() * saved.stake(), saved.events());
        boolean matches = simulation.replay.ReplayValidation.matches(saved, replayed);
        WinScreenPrinter.printReplay(saved, replayed, matches, output);
        if (!matches) {
            throw new IllegalStateException("Replayed outcomes differ from saved wins");
        }
    }

    private static Game createGameForReplay(String gameId, String gameMode, Path csvPath) throws Exception {
        Path reportFolder = csvPath.toAbsolutePath().normalize().getParent();
        if (reportFolder == null || !Files.isDirectory(reportFolder)) {
            if (!gameMode.equalsIgnoreCase("Normal")) {
                throw new IllegalArgumentException("Replay requires a PAR workbook for mode '"
                        + gameMode + "'");
            }
            return GameFactory.create(gameId);
        }
        List<Path> workbooks;
        try (var files = Files.list(reportFolder)) {
            workbooks = files.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().toLowerCase().endsWith(".xlsx"))
                    .toList();
        }
        if (workbooks.size() > 1) {
            throw new IllegalArgumentException("Replay report folder contains multiple PAR workbooks; "
                    + "keep the matching workbook beside saved_gameplays.csv");
        }
        if (workbooks.isEmpty()) {
            if (!gameMode.equalsIgnoreCase("Normal")) {
                throw new IllegalArgumentException("Replay report has no PAR workbook for mode '"
                        + gameMode + "'");
            }
            return GameFactory.create(gameId);
        }
        return GameFactory.create(gameId, workbooks.get(0), gameMode);
    }

    private static void printUsage() {
        System.out.println("Usage: java toolkit.replay.ReplayerMain <saved-gameplays.csv> <gameplay-id>");
    }
}
