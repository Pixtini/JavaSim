package toolkit.reelset;

import game.ExhaustiveReelGame;
import game.model.SpinResult;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import toolkit.progress.ProgressBar;

/** Exhaustively evaluates each unique stop combination in every configured reel set. */
public final class FullReelsetSimulator {
    public ReelsetSimulationResult simulate(
            ExhaustiveReelGame game, double stake, int workerCount) {
        if (!Double.isFinite(stake) || stake <= 0.0) {
            throw new IllegalArgumentException("Stake must be finite and greater than zero");
        }
        if (workerCount <= 0) {
            throw new IllegalArgumentException("Worker count must be greater than zero");
        }

        List<SetScenario> scenarios = buildScenarios(game);
        long combinations = scenarios.stream().mapToLong(SetScenario::combinations).sum();
        long weightedCombinations = 0;
        for (SetScenario scenario : scenarios) {
            weightedCombinations = Math.addExact(weightedCombinations,
                    Math.multiplyExact(scenario.combinations(), scenario.weight()));
        }

        int tasks = (int) Math.min(combinations, workerCount);
        ExecutorService executor = Executors.newFixedThreadPool(tasks);
        ProgressBar progress = new ProgressBar("Full reelset", combinations, System.err);
        try {
            List<Future<WorkerResult>> futures = new ArrayList<>(tasks);
            long baseRangeSize = combinations / tasks;
            long extraStops = combinations % tasks;
            long start = 0;
            for (int task = 0; task < tasks; task++) {
                long rangeSize = baseRangeSize + (task < extraStops ? 1 : 0);
                long rangeStart = start;
                long rangeEnd = rangeStart + rangeSize;
                start = rangeEnd;
                futures.add(executor.submit(() -> evaluateRange(
                        game, scenarios, rangeStart, rangeEnd, stake, progress)));
            }

            Map<String, Long> awardHits = new LinkedHashMap<>();
            game.getAwardLabels().forEach(label -> awardHits.put(label, 0L));
            long featureTriggers = 0;
            double totalWinnings = 0.0;
            for (Future<WorkerResult> future : futures) {
                WorkerResult partial = future.get();
                featureTriggers = Math.addExact(featureTriggers, partial.featureTriggers());
                totalWinnings += partial.totalWinnings();
                partial.awardHits().forEach((label, count) ->
                        awardHits.merge(label, count, Math::addExact));
            }
            return new ReelsetSimulationResult(combinations, weightedCombinations,
                    featureTriggers, weightedCombinations * stake, totalWinnings, awardHits);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Full reelset simulation was interrupted", exception);
        } catch (ExecutionException exception) {
            throw new IllegalStateException("A full reelset worker failed", exception.getCause());
        } finally {
            executor.shutdownNow();
            progress.close();
        }
    }

    /** Number of unique set-and-stop outcomes evaluated, before applying selector weights. */
    public static long countCombinations(ExhaustiveReelGame game) {
        return buildScenarios(game).stream().mapToLong(SetScenario::combinations).sum();
    }

    /** Number of selector-weighted outcomes represented by the complete evaluation. */
    public static long countWeightedCombinations(ExhaustiveReelGame game) {
        long weighted = 0;
        for (SetScenario scenario : buildScenarios(game)) {
            weighted = Math.addExact(weighted,
                    Math.multiplyExact(scenario.combinations(), scenario.weight()));
        }
        return weighted;
    }

    private static List<SetScenario> buildScenarios(ExhaustiveReelGame game) {
        List<SetScenario> scenarios = new ArrayList<>();
        long start = 0;
        for (int setIndex = 0; setIndex < game.getReelSetCount(); setIndex++) {
            long weight = game.getReelSetWeight(setIndex);
            if (weight <= 0) {
                continue;
            }
            int[] stopCounts = new int[game.getReelCount()];
            long combinations = 1;
            for (int reel = 0; reel < stopCounts.length; reel++) {
                stopCounts[reel] = game.getStopCount(setIndex, reel);
                if (stopCounts[reel] <= 0) {
                    throw new IllegalArgumentException("Every reel must have at least one stop");
                }
                combinations = Math.multiplyExact(combinations, stopCounts[reel]);
            }
            long end = Math.addExact(start, combinations);
            scenarios.add(new SetScenario(setIndex, stopCounts, combinations, weight, start, end));
            start = end;
        }
        if (scenarios.isEmpty()) {
            throw new IllegalArgumentException("At least one reel set must have positive selector weight");
        }
        long combinationsPerSet = scenarios.get(0).combinations();
        if (scenarios.stream().anyMatch(
                scenario -> scenario.combinations() != combinationsPerSet)) {
            throw new IllegalArgumentException(
                    "Weighted full-reelset evaluation requires selectable sets to have the same "
                            + "number of stop combinations");
        }
        return List.copyOf(scenarios);
    }

    private WorkerResult evaluateRange(ExhaustiveReelGame game, List<SetScenario> scenarios,
            long start, long end, double stake, ProgressBar progress) {
        int[] stops = new int[game.getReelCount()];
        Map<String, Long> awardHits = new LinkedHashMap<>();
        long featureTriggers = 0;
        double totalWinnings = 0.0;
        long pendingProgress = 0;
        int scenarioIndex = 0;
        ExhaustiveReelGame.StopEvaluator evaluator = null;

        for (long outcome = start; outcome < end; outcome++) {
            while (outcome >= scenarios.get(scenarioIndex).end()) {
                scenarioIndex++;
                evaluator = null;
            }
            SetScenario scenario = scenarios.get(scenarioIndex);
            if (evaluator == null) {
                evaluator = game.createStopEvaluator(scenario.setIndex());
            }
            decodeStops(outcome - scenario.start(), scenario.stopCounts(), stops);
            SpinResult result = evaluator.evaluate(stops, stake);
            totalWinnings += result.getWin() * scenario.weight();
            if (result.hasFreeSpin()) {
                featureTriggers = Math.addExact(featureTriggers, scenario.weight());
            }
            for (String award : result.getAwards()) {
                awardHits.merge(award, scenario.weight(), Math::addExact);
            }
            pendingProgress++;
            if (pendingProgress == 65_536) {
                progress.advance(pendingProgress);
                pendingProgress = 0;
            }
        }
        progress.advance(pendingProgress);
        return new WorkerResult(end - start, featureTriggers, totalWinnings, awardHits);
    }

    private static void decodeStops(long combination, int[] stopCounts, int[] stops) {
        long remaining = combination;
        for (int reel = stopCounts.length - 1; reel >= 0; reel--) {
            stops[reel] = (int) (remaining % stopCounts[reel]);
            remaining /= stopCounts[reel];
        }
    }

    private record SetScenario(int setIndex, int[] stopCounts, long combinations,
            long weight, long start, long end) {}

    private record WorkerResult(long combinations, long featureTriggers,
            double totalWinnings, Map<String, Long> awardHits) {}
}
