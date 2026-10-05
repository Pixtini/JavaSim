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

/** Exhaustively evaluates each unique combination of reel stop positions. */
public final class FullReelsetSimulator {
    public ReelsetSimulationResult simulate(
            ExhaustiveReelGame game, double stake, int workerCount) {
        if (!Double.isFinite(stake) || stake <= 0.0) {
            throw new IllegalArgumentException("Stake must be finite and greater than zero");
        }
        if (workerCount <= 0) {
            throw new IllegalArgumentException("Worker count must be greater than zero");
        }

        int reelCount = game.getReelCount();
        int[] stopCounts = new int[reelCount];
        long combinations = 1;
        for (int reel = 0; reel < reelCount; reel++) {
            stopCounts[reel] = game.getStopCount(reel);
            if (stopCounts[reel] <= 0) {
                throw new IllegalArgumentException("Every reel must have at least one stop");
            }
            combinations = Math.multiplyExact(combinations, stopCounts[reel]);
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
                        game, stopCounts, rangeStart, rangeEnd, stake, progress)));
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
            return new ReelsetSimulationResult(combinations, featureTriggers,
                    combinations * stake, totalWinnings, awardHits);
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

    public static long countCombinations(ExhaustiveReelGame game) {
        long combinations = 1;
        for (int reel = 0; reel < game.getReelCount(); reel++) {
            combinations = Math.multiplyExact(combinations, game.getStopCount(reel));
        }
        return combinations;
    }

    private WorkerResult evaluateRange(ExhaustiveReelGame game, int[] stopCounts,
            long start, long end, double stake, ProgressBar progress) {
        ExhaustiveReelGame.StopEvaluator evaluator = game.createStopEvaluator();
        int[] stops = new int[stopCounts.length];
        Map<String, Long> awardHits = new LinkedHashMap<>();
        long featureTriggers = 0;
        double totalWinnings = 0.0;
        long pendingProgress = 0;

        for (long combination = start; combination < end; combination++) {
            decodeStops(combination, stopCounts, stops);
            SpinResult result = evaluator.evaluate(stops, stake);
            totalWinnings += result.getWin();
            if (result.hasFreeSpin()) {
                featureTriggers++;
            }
            for (String award : result.getAwards()) {
                awardHits.merge(award, 1L, Long::sum);
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

    private record WorkerResult(long combinations, long featureTriggers,
            double totalWinnings, Map<String, Long> awardHits) {}
}
