import game.expandingwild.ExpandingWildGameSession;
import game.expandingwild.config.ExpandingWildConfig;
import GameModuleFramework.features.SymbolInsertion;
import GameModuleFramework.symbols.Symbol;
import GameModuleFramework.paylines.Payline;
import GameModuleFramework.paylines.Paytable;
import game.model.GameRoundResult;
import simulation.replay.SavedGameplay;
import simulation.replay.SavedGameplayCsv;
import simulation.config.SimConfig;
import simulation.engine.SimulationRunner;
import game.GameFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Random;

/** Focused tests for replay payloads, capped events, and CSV round trips. */
public final class ReplayTests {
    private static int assertions;

    private ReplayTests() {}

    public static void main(String[] args) throws Exception {
        testExpandingWildReplayAndCsvRoundTrip();
        testRecordedMultiplierChoicesIgnoreCurrentWeights();
        testRecordedInsertionLocationsIgnoreCurrentHeatMap();
        testConfiguredCaptureLimit();
        System.out.println("Passed " + assertions + " replay assertions.");
    }

    private static void testConfiguredCaptureLimit() {
        SimConfig config = new SimConfig();
        config.rounds = 8;
        config.threads = 2;
        config.partitions = 4;
        config.seed = 9;
        config.usePreviousSeed = true;
        config.exportReport = false;
        config.maxSavedGameplays = 3;
        var result = new SimulationRunner(config, GameFactory.create(config.gameId)).run();
        check(result.getSavedGameplays().size() == 3,
                "simulation captures no more than the configured number of replayable rounds");
        check(result.getSavedGameplays().stream().map(SavedGameplay::id).toList()
                        .equals(List.of(1L, 2L, 3L)),
                "parallel partitions retain stable gameplay IDs in round order");

        config.maxSavedGameplays = 0;
        var disabled = new SimulationRunner(config, GameFactory.create(config.gameId)).run();
        check(disabled.getSavedGameplays().isEmpty(), "zero disables replay capture");
    }

    private static void testRecordedMultiplierChoicesIgnoreCurrentWeights() {
        ExpandingWildConfig originalConfig = deterministicConfig();
        originalConfig.freeGamesAwarded = 1;
        originalConfig.freeGame.setSelectionWeights = List.of(
                new GameModuleFramework.probability.WeightedTable.Entry<>(1, 1));
        originalConfig.freeGame.sets.get(1).reelStrips = List.of(
                List.of(0), List.of(0), List.of(0), List.of(0), List.of(0));
        originalConfig.freeGame.sets.get(1).symbolInsertions = List.of(
                fixedInsertion(ExpandingWildConfig.BANNER, 1, List.of(0), 0));
        originalConfig.freeGame.sets.get(1).bannerMultiplierWeights = List.of(
                new GameModuleFramework.probability.WeightedTable.Entry<>(2, 1));
        GameRoundResult original = new ExpandingWildGameSession(
                originalConfig, new Random(5)).playRound(1.0, 100.0, true);
        String recordedPayload = original.getReplayEvents().get(1).payload();
        check(recordedPayload.contains("\"bannerMultipliers\"")
                        && recordedPayload.contains("\"0\":2"),
                "replay payload stores each resolved banner multiplier");

        ExpandingWildConfig changedWeights = deterministicConfig();
        changedWeights.freeGamesAwarded = 1;
        changedWeights.freeGame.sets.get(1).reelStrips = List.of(
                List.of(0), List.of(0), List.of(0), List.of(0), List.of(0));
        changedWeights.freeGame.sets.get(1).symbolInsertions = List.of(
                fixedInsertion(ExpandingWildConfig.BANNER, 1, List.of(0), 0));
        changedWeights.freeGame.sets.get(1).bannerMultiplierWeights = List.of(
                new GameModuleFramework.probability.WeightedTable.Entry<>(10, 1));
        GameRoundResult replayed = new ExpandingWildGameSession(
                changedWeights, new Random(6)).replayRound(
                        1.0, 100.0, original.getReplayEvents());
        checkClose(replayed.getFreeGameResults().get(0).getWin(),
                original.getFreeGameResults().get(0).getWin(),
                "replay injects recorded multipliers instead of redrawing current weights");
    }

    private static void testRecordedInsertionLocationsIgnoreCurrentHeatMap() {
        ExpandingWildConfig originalConfig = deterministicConfig();
        GameRoundResult original = new ExpandingWildGameSession(
                originalConfig, new Random(91)).playRound(1.0, 1_000.0, true);
        ExpandingWildConfig changedHeatMap = deterministicConfig();
        changedHeatMap.baseGame.sets.get(0).symbolInsertions = List.of(
                fixedInsertion(ExpandingWildConfig.SCATTER, 3, List.of(0, 2, 4), 4));

        GameRoundResult replayed = new ExpandingWildGameSession(
                changedHeatMap, new Random(92)).replayRound(
                        1.0, 1_000.0, original.getReplayEvents());

        var originalInsertions = game.expandingwild.ExpandingWildReplayPayload
                .fromJson(original.getReplayEvents().get(0).payload()).insertions();
        var replayedInsertions = game.expandingwild.ExpandingWildReplayPayload
                .fromJson(replayed.getReplayEvents().get(0).payload()).insertions();
        check(replayedInsertions.equals(originalInsertions)
                        && originalInsertions.stream().allMatch(placement -> placement.row() == 0),
                "replay restores recorded insertion positions after a heat-map change");
        checkClose(replayed.getBaseGameResult().getWin(),
                original.getBaseGameResult().getWin(),
                "replayed insertion produces the original recalculated basegame win");
    }

    private static void testExpandingWildReplayAndCsvRoundTrip() throws Exception {
        ExpandingWildConfig config = deterministicConfig();
        ExpandingWildGameSession originalSession = new ExpandingWildGameSession(
                config, new Random(1));
        GameRoundResult original = originalSession.playRound(
                1.0, config.maxWinMultiplier, true);
        check(original.getReplayEvents().size() == 4,
                "capture stores the basegame and all configured freegame events");
        check(original.getReplayEvents().get(0).type().equals("basegame"),
                "basegame replay event is first");
        check(original.getReplayEvents().get(1).type().equals("freegame"),
                "freegame replay event is identified independently");
        check(original.getReplayEvents().get(0).payload().contains("\"insertions\""),
                "replay event records inserted symbol locations");

        double featureWin = original.getFreeGameResults().stream()
                .mapToDouble(result -> result.getWin()).sum();
        SavedGameplay saved = new SavedGameplay(42, "expanding-wild", 1.0,
                original.getBaseGameResult().getWin() + featureWin,
                original.getBaseGameResult().getWin(), featureWin, original.getReplayEvents());
        Path csv = Files.createTempFile("saved-gameplays", ".csv");
        try {
            SavedGameplayCsv.write(csv, List.of(saved));
            SavedGameplay loaded = SavedGameplayCsv.readById(csv, 42);
            check(loaded.events().equals(saved.events()), "CSV preserves replay event payloads");

            GameRoundResult replayed = new ExpandingWildGameSession(config, new Random(999))
                    .replayRound(loaded.stake(), config.maxWinMultiplier, loaded.events());
            checkClose(replayed.getBaseGameResult().getWin(), saved.baseGameWin(),
                    "replayed basegame win matches saved win");
            checkClose(replayed.getFreeGameResults().stream()
                            .mapToDouble(result -> result.getWin()).sum(),
                    saved.featureGameWin(), "replayed feature wins match saved wins");
            checkClose(replayed.getBaseGameResult().getWin()
                            + replayed.getFreeGameResults().stream()
                                    .mapToDouble(result -> result.getWin()).sum(),
                    saved.totalWin(), "replayed total win matches saved total");
            check(replayed.getReplayEvents().get(0).payload()
                            .equals(loaded.events().get(0).payload()),
                    "replay retains its recorded reel stops");
        } finally {
            Files.deleteIfExists(csv);
        }
    }

    private static ExpandingWildConfig deterministicConfig() {
        ExpandingWildConfig config = new ExpandingWildConfig();
        config.maxWinMultiplier = 100.0;
        config.freeGamesAwarded = 3;
        config.baseGame.setSelectionWeights = List.of(
                new GameModuleFramework.probability.WeightedTable.Entry<>(0, 1));
        config.freeGame.setSelectionWeights = List.of(
                new GameModuleFramework.probability.WeightedTable.Entry<>(0, 1));
        config.baseGame.sets.get(0).reelStrips = List.of(
                List.of(0), List.of(0), List.of(0), List.of(0), List.of(0));
        config.baseGame.sets.get(0).symbolInsertions = List.of(
                fixedInsertion(ExpandingWildConfig.SCATTER, 3, List.of(0, 2, 4), 0));
        config.freeGame.sets.get(0).reelStrips = List.of(
                List.of(0), List.of(0), List.of(0), List.of(0), List.of(0));
        config.paylines = List.of(new Payline(1, 1, 1, 1, 1));
        config.paytable = new Paytable(Map.of(
                ExpandingWildConfig.T1, Map.of(3, 10.0, 4, 20.0, 5, 30.0)));
        return config;
    }

    private static SymbolInsertion.Rule fixedInsertion(Symbol symbol, int count,
            List<Integer> eligibleReels, int row) {
        List<List<Long>> heatMap = new java.util.ArrayList<>();
        for (int reel = 0; reel < 5; reel++) {
            List<Long> weights = new java.util.ArrayList<>();
            for (int visibleRow = 0; visibleRow < 5; visibleRow++) {
                weights.add(eligibleReels.contains(reel) && visibleRow == row ? 1L : 0L);
            }
            heatMap.add(List.copyOf(weights));
        }
        return new SymbolInsertion.Rule(symbol,
                List.of(new GameModuleFramework.probability.WeightedTable.Entry<>(count, 1)),
                heatMap, 1, java.util.Set.of(ExpandingWildConfig.SCATTER,
                        ExpandingWildConfig.BANNER, ExpandingWildConfig.WILD));
    }

    private static void check(boolean condition, String message) {
        assertions++;
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void checkClose(double actual, double expected, String message) {
        check(Math.abs(actual - expected) <= 1e-9, message);
    }
}
