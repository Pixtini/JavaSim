import config.GameConfig;
import config.SimConfig;
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
    private final GameConfig gameConfig;

    public SimulationRunner(SimConfig simConfig, GameConfig gameConfig) {
        this.simConfig = simConfig;
        this.gameConfig = gameConfig;
    }

    public SimulationResult run() {
        validateConfig();
        if (!simConfig.usePreviousSeed) {
            simConfig.seed = ThreadLocalRandom.current().nextLong();
        }

        standardStats totalGameStats = new standardStats(simConfig.stake, gameConfig.paytable);
        standardStats baseGameStats = new standardStats(simConfig.stake, gameConfig.paytable);
        standardStats freeGameStats = new standardStats(simConfig.stake, gameConfig.paytable);

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

        return new SimulationResult(totalGameStats, baseGameStats, freeGameStats);
    }

    private PartitionStats runPartition(int partitionId, long rounds) {
        Random random = new Random(seedForPartition(simConfig.seed, partitionId));
        gameBasicBase baseGame = new gameBasicBase(gameConfig, random);
        gameBasicFree freeGame = new gameBasicFree(gameConfig, random);

        standardStats totalGameStats = new standardStats(simConfig.stake, gameConfig.paytable);
        standardStats baseGameStats = new standardStats(simConfig.stake, gameConfig.paytable);
        standardStats freeGameStats = new standardStats(simConfig.stake, gameConfig.paytable);

        for (long round = 0; round < rounds; round++) {
            spinResult baseResult = baseGame.spin();
            baseGameStats.addResult(baseResult);
            baseGameStats.recordWinInDistribution(baseResult.getWin());

            double totalWin = baseResult.getWin();
            if (baseResult.hasFreeSpin()) {
                freeGameStats.recordFreegameTrigger();
                double freeGameWin = 0.0;

                for (int freeSpin = 0; freeSpin < gameConfig.freeSpinAmount; freeSpin++) {
                    spinResult freeResult = freeGame.spin();
                    freeGameStats.addResult(freeResult);
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
        private final standardStats totalGameStats;
        private final standardStats baseGameStats;
        private final standardStats freeGameStats;

        private PartitionStats(standardStats totalGameStats,
                standardStats baseGameStats, standardStats freeGameStats) {
            this.totalGameStats = totalGameStats;
            this.baseGameStats = baseGameStats;
            this.freeGameStats = freeGameStats;
        }
    }
}
