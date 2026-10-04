package simulation.engine;

import game.model.GameRoundResult;
import game.model.SpinResult;
import game.Game;
import game.GameSession;
import simulation.config.SimConfig;
import simulation.result.SimulationResult;
import simulation.stats.StandardStats;
import java.util.ArrayList;
import java.util.List;
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

        int workerCount = Math.min(simConfig.threads, simConfig.partitions);
        ExecutorService executor = Executors.newFixedThreadPool(workerCount);
        List<Future<PartitionStats>> partitionResults = new ArrayList<>(simConfig.partitions);
        long baseRoundsPerPartition = simConfig.rounds / simConfig.partitions;
        long extraRounds = simConfig.rounds % simConfig.partitions;

        try {
            for (int partitionId = 0; partitionId < simConfig.partitions; partitionId++) {
                long partitionRounds = baseRoundsPerPartition
                        + (partitionId < extraRounds ? 1 : 0);
                int currentPartitionId = partitionId;
                partitionResults.add(executor.submit(
                        () -> runPartition(currentPartitionId, partitionRounds)));
            }

            // Merge by partition ID, not by completion order, for repeatable floating-point totals.
            for (Future<PartitionStats> partitionResult : partitionResults) {
                PartitionStats partitionStats = partitionResult.get();
                totalGameStats.mergeFrom(partitionStats.totalGameStats);
                baseGameStats.mergeFrom(partitionStats.baseGameStats);
                freeGameStats.mergeFrom(partitionStats.freeGameStats);
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Simulation was interrupted", exception);
        } catch (ExecutionException exception) {
            throw new IllegalStateException("A simulation partition failed", exception.getCause());
        } finally {
            executor.shutdownNow();
        }

        return new SimulationResult(
                totalGameStats,
                baseGameStats,
                freeGameStats,
                System.nanoTime() - startNanos);
    }

    private PartitionStats runPartition(int partitionId, long rounds) {
        Random random = new Random(seedForPartition(simConfig.seed, partitionId));
        GameSession gameSession = game.createSession(random);

        int[] paytable = game.getPaytable();
        List<String> awardLabels = game.getAwardLabels();
        StandardStats totalGameStats = new StandardStats(simConfig.stake, paytable, awardLabels);
        StandardStats baseGameStats = new StandardStats(simConfig.stake, paytable, awardLabels);
        StandardStats freeGameStats = new StandardStats(simConfig.stake, paytable, awardLabels);
        freeGameStats.recordStakeBasis(rounds * simConfig.stake);

        for (long round = 0; round < rounds; round++) {
            GameRoundResult roundResult = gameSession.playRound(simConfig.stake);
            SpinResult baseResult = roundResult.getBaseGameResult();
            baseGameStats.addResult(baseResult);
            baseGameStats.recordWinInDistribution(baseResult.getWin());

            double totalWin = baseResult.getWin();
            if (baseResult.hasFreeSpin()) {
                freeGameStats.recordFreegameTrigger();
                double freeGameWin = 0.0;

                for (SpinResult freeResult : roundResult.getFreeGameResults()) {
                    freeGameStats.addResult(freeResult, 0.0);
                    freeGameWin += freeResult.getWin();
                    totalWin += freeResult.getWin();
                }

                freeGameStats.recordWinInDistribution(freeGameWin);
            }
            totalGameStats.recordWinInDistribution(totalWin);
        }

        return new PartitionStats(totalGameStats, baseGameStats, freeGameStats);
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

        private PartitionStats(StandardStats totalGameStats,
                StandardStats baseGameStats, StandardStats freeGameStats) {
            this.totalGameStats = totalGameStats;
            this.baseGameStats = baseGameStats;
            this.freeGameStats = freeGameStats;
        }
    }
}
