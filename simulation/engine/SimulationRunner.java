package simulation.engine;

import game.model.GameRoundResult;
import game.model.SpinResult;
import game.Game;
import game.GameSession;
import simulation.config.SimConfig;
import simulation.result.SimulationResult;
import simulation.replay.SavedGameplay;
import simulation.stats.StandardStats;
import toolkit.progress.ProgressBar;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;

public final class SimulationRunner {
    private final SimConfig simConfig;
    private final Game game;

    public SimulationRunner(SimConfig simConfig, Game game) {
        this.simConfig = simConfig;
        this.game = game;
    }

    public SimulationResult run() {
        long startNanos = System.nanoTime();
        validateConfig();
        if (!simConfig.usePreviousSeed) {
            simConfig.seed = ThreadLocalRandom.current().nextLong();
        }

        int[] paytable = game.getPaytable();
        List<String> awardLabels = game.getAwardLabels();
        StandardStats totalGameStats = new StandardStats(simConfig.stake, paytable, awardLabels);
        StandardStats baseGameStats = new StandardStats(simConfig.stake, paytable, awardLabels);
        StandardStats freeGameStats = new StandardStats(simConfig.stake, paytable, awardLabels);
        Map<Integer, StandardStats> baseGameSetStats = createSetStats(
                game.getBaseGameSetCount(), paytable, awardLabels);
        Map<Integer, StandardStats> freeGameSetStats = createSetStats(
                game.getFreeGameSetCount(), paytable, awardLabels);
        List<SavedGameplay> savedGameplays = new ArrayList<>();

        int workerCount = Math.min(simConfig.threads, simConfig.partitions);
        ExecutorService executor = Executors.newFixedThreadPool(workerCount);
        List<Future<PartitionStats>> partitionResults = new ArrayList<>(simConfig.partitions);
        ProgressBar progress = new ProgressBar(
                "Monte Carlo", simConfig.rounds, System.err);
        long baseRoundsPerPartition = simConfig.rounds / simConfig.partitions;
        long extraRounds = simConfig.rounds % simConfig.partitions;

        try {
            for (int partitionId = 0; partitionId < simConfig.partitions; partitionId++) {
                long partitionRounds = baseRoundsPerPartition
                        + (partitionId < extraRounds ? 1 : 0);
                int currentPartitionId = partitionId;
                long firstRoundId = baseRoundsPerPartition * partitionId
                        + Math.min(partitionId, extraRounds) + 1;
                partitionResults.add(executor.submit(
                        () -> runPartition(currentPartitionId, firstRoundId,
                                partitionRounds, progress)));
            }

            // Merge by partition ID, not by completion order, for repeatable floating-point totals.
            for (Future<PartitionStats> partitionResult : partitionResults) {
                PartitionStats partitionStats = partitionResult.get();
                totalGameStats.mergeFrom(partitionStats.totalGameStats);
                baseGameStats.mergeFrom(partitionStats.baseGameStats);
                freeGameStats.mergeFrom(partitionStats.freeGameStats);
                mergeSetStats(baseGameSetStats, partitionStats.baseGameSetStats);
                mergeSetStats(freeGameSetStats, partitionStats.freeGameSetStats);
                savedGameplays.addAll(partitionStats.savedGameplays);
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Simulation was interrupted", exception);
        } catch (ExecutionException exception) {
            throw new IllegalStateException("A simulation partition failed", exception.getCause());
        } finally {
            executor.shutdownNow();
            progress.close();
        }

        return new SimulationResult(
                totalGameStats,
                baseGameStats,
                freeGameStats,
                baseGameSetStats,
                freeGameSetStats,
                savedGameplays,
                System.nanoTime() - startNanos);
    }

    private PartitionStats runPartition(int partitionId, long firstRoundId,
            long rounds, ProgressBar progress) {
        Random random = new Random(seedForPartition(simConfig.seed, partitionId));
        GameSession gameSession = game.createSession(random);

        int[] paytable = game.getPaytable();
        List<String> awardLabels = game.getAwardLabels();
        StandardStats totalGameStats = new StandardStats(simConfig.stake, paytable, awardLabels);
        StandardStats baseGameStats = new StandardStats(simConfig.stake, paytable, awardLabels);
        StandardStats freeGameStats = new StandardStats(simConfig.stake, paytable, awardLabels);
        Map<Integer, StandardStats> baseGameSetStats = createSetStats(
                game.getBaseGameSetCount(), paytable, awardLabels);
        Map<Integer, StandardStats> freeGameSetStats = createSetStats(
                game.getFreeGameSetCount(), paytable, awardLabels);
        freeGameStats.recordStakeBasis(rounds * simConfig.stake);
        freeGameSetStats.values().forEach(stats ->
                stats.recordStakeBasis(rounds * simConfig.stake));
        List<SavedGameplay> savedGameplays = new ArrayList<>();

        long pendingProgress = 0;
        for (long round = 0; round < rounds; round++) {
            double maximumRoundWin = game.getMaxWinMultiplier() * simConfig.stake;
            long gameplayId = firstRoundId + round;
            boolean captureReplay = simConfig.maxSavedGameplays > 0
                    && gameplayId <= simConfig.maxSavedGameplays;
            GameRoundResult roundResult = gameSession.playRound(
                    simConfig.stake, maximumRoundWin, captureReplay);
            if (roundResult.isWinCapReached()) {
                totalGameStats.recordWinCap();
            }
            SpinResult baseResult = roundResult.getBaseGameResult();
            baseGameStats.addResult(baseResult);
            addToSetStats(baseGameSetStats, baseResult);
            baseGameStats.recordWinInDistribution(baseResult.getWin());

            double totalWin = baseResult.getWin();
            if (baseResult.hasFreeSpin()) {
                freeGameStats.recordFreegameTrigger();
                double freeGameWin = 0.0;

                for (SpinResult freeResult : roundResult.getFreeGameResults()) {
                    freeGameStats.addResult(freeResult, 0.0);
                    addToSetStats(freeGameSetStats, freeResult, 0.0);
                    freeGameWin += freeResult.getWin();
                    totalWin += freeResult.getWin();
                }

                freeGameStats.recordWinInDistribution(freeGameWin);
            }
            totalGameStats.recordWinInDistribution(totalWin);

            if (captureReplay && hasCompleteReplayEvents(roundResult)) {
                double featureWin = roundResult.getFreeGameResults().stream()
                        .mapToDouble(SpinResult::getWin).sum();
                savedGameplays.add(new SavedGameplay(gameplayId, simConfig.gameId,
                        simConfig.stake, totalWin, baseResult.getWin(), featureWin,
                        roundResult.getReplayEvents()));
            }

            pendingProgress++;
            if (pendingProgress == 65_536) {
                progress.advance(pendingProgress);
                pendingProgress = 0;
            }
        }

        progress.advance(pendingProgress);

        return new PartitionStats(totalGameStats, baseGameStats, freeGameStats,
                baseGameSetStats, freeGameSetStats, savedGameplays);
    }

    private boolean hasCompleteReplayEvents(GameRoundResult roundResult) {
        return !roundResult.getReplayEvents().isEmpty()
                && roundResult.getReplayEvents().size()
                        == 1 + roundResult.getFreeGameResults().size();
    }

    private Map<Integer, StandardStats> createSetStats(int setCount,
            int[] paytable, List<String> awardLabels) {
        Map<Integer, StandardStats> statsBySet = new LinkedHashMap<>();
        for (int setIndex = 0; setIndex < setCount; setIndex++) {
            statsBySet.put(setIndex, new StandardStats(simConfig.stake, paytable, awardLabels));
        }
        return statsBySet;
    }

    private void addToSetStats(Map<Integer, StandardStats> statsBySet, SpinResult result) {
        addToSetStats(statsBySet, result, simConfig.stake);
    }

    private void addToSetStats(Map<Integer, StandardStats> statsBySet,
            SpinResult result, double stakeContribution) {
        StandardStats stats = statsBySet.get(result.getSetIndex());
        if (stats == null) {
            if (statsBySet.isEmpty() && result.getSetIndex() == -1) {
                return;
            }
            throw new IllegalStateException(
                    "Game result references an unconfigured spin set: " + result.getSetIndex());
        }
        stats.addResult(result, stakeContribution);
    }

    private void mergeSetStats(Map<Integer, StandardStats> destination,
            Map<Integer, StandardStats> source) {
        source.forEach((setIndex, stats) -> {
            StandardStats target = destination.get(setIndex);
            if (target == null) {
                throw new IllegalStateException("Worker returned an unknown spin set: " + setIndex);
            }
            target.mergeFrom(stats);
        });
    }

    private void validateConfig() {
        if (simConfig.rounds <= 0) {
            throw new IllegalArgumentException("Simulation rounds must be greater than zero");
        }
        if (!Double.isFinite(simConfig.stake) || simConfig.stake <= 0.0) {
            throw new IllegalArgumentException("Simulation stake must be finite and greater than zero");
        }
        if (simConfig.threads <= 0) {
            throw new IllegalArgumentException("Simulation thread count must be greater than zero");
        }
        if (simConfig.partitions <= 0) {
            throw new IllegalArgumentException("Simulation partition count must be greater than zero");
        }
        if (simConfig.maxSavedGameplays < 0) {
            throw new IllegalArgumentException("Maximum saved gameplays must not be negative");
        }
    }

    private static long seedForPartition(long seed, int partitionId) {
        long value = seed + 0x9E3779B97F4A7C15L * (partitionId + 1L);
        value = (value ^ (value >>> 30)) * 0xBF58476D1CE4E5B9L;
        value = (value ^ (value >>> 27)) * 0x94D049BB133111EBL;
        return value ^ (value >>> 31);
    }

    private static final class PartitionStats {
        private final StandardStats totalGameStats;
        private final StandardStats baseGameStats;
        private final StandardStats freeGameStats;
        private final Map<Integer, StandardStats> baseGameSetStats;
        private final Map<Integer, StandardStats> freeGameSetStats;
        private final List<SavedGameplay> savedGameplays;

        private PartitionStats(StandardStats totalGameStats,
                StandardStats baseGameStats, StandardStats freeGameStats,
                Map<Integer, StandardStats> baseGameSetStats,
                Map<Integer, StandardStats> freeGameSetStats,
                List<SavedGameplay> savedGameplays) {
            this.totalGameStats = totalGameStats;
            this.baseGameStats = baseGameStats;
            this.freeGameStats = freeGameStats;
            this.baseGameSetStats = baseGameSetStats;
            this.freeGameSetStats = freeGameSetStats;
            this.savedGameplays = savedGameplays;
        }
    }
}
