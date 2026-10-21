package simulation.books;

import GameModuleFramework.probability.WeightedTable;
import game.Game;
import game.GameSession;
import game.model.*;
import simulation.config.SimConfig;
import simulation.replay.*;
import toolkit.par.*;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.*;
import java.util.*;

/** Prevalidates a finite weighted book and caches typed rounds at the requested base stake. */
public final class GameplayBook {
    public record Entry(long gameplayId, long weight, GameRoundResult round) {}
    private final WeightedTable<Entry> selector;
    private final BookRunMetadata metadata;

    private GameplayBook(List<WeightedTable.Entry<Entry>> entries, BookRunMetadata metadata) {
        this.selector = new WeightedTable<>(entries); this.metadata = metadata;
    }
    public Entry draw(Random random) { return selector.draw(random); }
    public BookRunMetadata metadata() { return metadata; }
    public List<WeightedTable.Entry<Entry>> entries() { return selector.getEntries(); }

    public static GameplayBook load(SimConfig config, Game game) throws IOException {
        if (config.parWorkbookPath == null)
            throw new IllegalArgumentException("Book simulation requires a PAR workbook");
        Path path = config.parWorkbookPath.toAbsolutePath().normalize();
        String workbookHash = BookRunMetadata.sha256(path);
        var workbook = ExcelWorkbookReader.read(path);
        String mode = ParWorkbookModes.select(workbook, config.gameMode).mode();
        String name = findSheet(workbook, mode);
        var sheet = workbook.requireSheet(name);
        if (!sheet.cell(1, 0).trim().equalsIgnoreCase("Gameplay ID")
                || !sheet.cell(1, 1).trim().equalsIgnoreCase("Weight"))
            throw new IllegalArgumentException(name + " needs Gameplay ID and Weight headers in A1:B1");
        Map<Long, Long> weights = new LinkedHashMap<>();
        int tableRows = 0; boolean ended = false;
        for (int row = 1; row <= sheet.lastRow(); row++) {
            var cells = sheet.rows().getOrDefault(row, Map.of());
            if (cells.keySet().stream().anyMatch(col -> col > 1))
                throw new IllegalArgumentException(name + " has unexpected columns at row " + row);
            if (row == 1) continue;
            String idText = sheet.cell(row, 0).trim(), weightText = sheet.cell(row, 1).trim();
            if (idText.isEmpty() && weightText.isEmpty()) { ended = true; continue; }
            if (ended || idText.isEmpty() || weightText.isEmpty())
                throw new IllegalArgumentException(name + " has a gap/incomplete row at " + row);
            long id = integer(idText, name, row), weight = integer(weightText, name, row);
            if (id <= 0 || weight < 0)
                throw new IllegalArgumentException(name + " needs positive IDs and nonnegative weights at row " + row);
            try { weights.merge(id, weight, Math::addExact); }
            catch (ArithmeticException e) { throw new IllegalArgumentException("Book weight overflow for ID " + id, e); }
            tableRows++;
        }
        weights.values().removeIf(weight -> weight == 0);
        if (weights.isEmpty()) throw new IllegalArgumentException(name + " has no positive gameplay weights");
        long totalWeight = 0;
        try { for (long weight : weights.values()) totalWeight = Math.addExact(totalWeight, weight); }
        catch (ArithmeticException e) { throw new IllegalArgumentException("Total book weight overflow", e); }
        Path csv = findCsv(path, mode);
        String csvHash = BookRunMetadata.sha256(csv);
        var saved = SavedGameplayCsv.readByIds(csv, weights.keySet());
        List<WeightedTable.Entry<Entry>> entries = new ArrayList<>();
        double expectedWin = 0, triggerProbability = 0;
        for (var weighted : weights.entrySet()) {
            SavedGameplay original = saved.get(weighted.getKey());
            if (!original.gameId().equals(config.gameId) || !original.gameMode().equalsIgnoreCase(mode))
                throw new IllegalArgumentException("Book gameplay " + original.id() + " belongs to another game or mode");
            GameSession replaySession = game.createSession(new ReplayOnlyRandom());
            GameRoundResult recorded;
            try {
                recorded = replaySession.replayRound(original.stake(),
                        original.stake() * game.getMaxWinMultiplier(), original.events());
            } catch (RuntimeException e) {
                throw new IllegalArgumentException("Cannot reconstruct book gameplay " + original.id() + ": " + e.getMessage(), e);
            }
            if (!ReplayValidation.matches(original, recorded))
                throw new IllegalArgumentException("Book gameplay " + original.id()
                        + " does not match this PAR; use matching reels/rules or harvest again");
            GameRoundResult round = config.stake == original.stake() ? recorded
                    : replaySession.replayRound(config.stake, config.stake * game.getMaxWinMultiplier(), original.events());
            validateRound(round, game, config.stake, original.id());
            double probability = weighted.getValue() / (double) totalWeight;
            expectedWin += probability * total(round);
            if (round.getBaseGameResult().hasFreeSpin()) triggerProbability += probability;
            entries.add(new WeightedTable.Entry<>(new Entry(original.id(), weighted.getValue(), round), weighted.getValue()));
        }
        var metadata = new BookRunMetadata(path, mode, name, csv, tableRows, weights.size(), totalWeight,
                expectedWin / (config.stake * game.getCostMultiplier()), triggerProbability, workbookHash, csvHash);
        metadata.verifyInputsUnchanged();
        return new GameplayBook(entries, metadata);
    }
    private static void validateRound(GameRoundResult round, Game game, double stake, long id) {
        List<SpinResult> spins = new ArrayList<>(); spins.add(round.getBaseGameResult()); spins.addAll(round.getFreeGameResults());
        if (round.getReplayEvents().size() != spins.size())
            throw new IllegalArgumentException("Book gameplay " + id + " lacks complete replay events");
        for (int i = 0; i < spins.size(); i++) {
            var spin = spins.get(i);
            int sets = i == 0 ? game.getBaseGameSetCount() : game.getFreeGameSetCount();
            if (!Double.isFinite(spin.getWin()) || spin.getWin() < 0
                    || (sets == 0 ? spin.getSetIndex() != -1 : spin.getSetIndex() < 0 || spin.getSetIndex() >= sets))
                throw new IllegalArgumentException("Book gameplay " + id + " has invalid win or set metadata");
        }
        if (!round.getBaseGameResult().hasFreeSpin() && !round.getFreeGameResults().isEmpty())
            throw new IllegalArgumentException("Book gameplay " + id + " has feature spins without a trigger");
        double win = total(round), cap = stake * game.getMaxWinMultiplier();
        if (!Double.isFinite(win) || win > cap + 1e-8 * Math.max(1, cap))
            throw new IllegalArgumentException("Book gameplay " + id + " exceeds the round cap");
    }
    private static double total(GameRoundResult round) {
        return round.getBaseGameResult().getWin() + round.getFreeGameResults().stream().mapToDouble(SpinResult::getWin).sum();
    }
    private static String findSheet(ExcelWorkbookReader.Workbook workbook, String mode) {
        var matches = workbook.sheets().keySet().stream().filter(s -> s.trim().equalsIgnoreCase(mode + "-Books")).toList();
        if (matches.size() != 1) throw new IllegalArgumentException("Book simulation requires exactly one " + mode + "-Books sheet");
        return matches.get(0);
    }
    private static Path findCsv(Path workbook, String mode) throws IOException {
        var dirs = matchingChildren(workbook.getParent(), "books");
        if (dirs.size() != 1 || !Files.isDirectory(dirs.get(0)))
            throw new IOException("Book simulation requires a books/ directory beside the PAR workbook");
        String filename = "saved_gameplays_" + mode.toLowerCase(Locale.ROOT) + ".csv";
        var files = matchingChildren(dirs.get(0), filename);
        if (files.size() != 1 || !Files.isRegularFile(files.get(0)))
            throw new IOException("Book simulation requires " + dirs.get(0).resolve(filename));
        return files.get(0);
    }
    private static List<Path> matchingChildren(Path parent, String name) throws IOException {
        try (var paths = Files.list(parent)) {
            return paths.filter(p -> p.getFileName().toString().equalsIgnoreCase(name)).toList();
        }
    }
    private static long integer(String text, String sheet, int row) {
        try { return new BigDecimal(text).longValueExact(); }
        catch (NumberFormatException | ArithmeticException e) {
            throw new IllegalArgumentException(sheet + " needs integer IDs/weights at row " + row, e);
        }
    }
    private static final class ReplayOnlyRandom extends Random {
        @Override protected int next(int bits) {
            throw new IllegalStateException("Book replay must reconstruct events without drawing fresh random outcomes");
        }
    }
}
