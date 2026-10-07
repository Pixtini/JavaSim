package GameModuleFramework.features;

import GameModuleFramework.probability.WeightedTable;
import GameModuleFramework.reels.ReelGrid;
import GameModuleFramework.symbols.Symbol;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.Set;

/** Applies weighted symbol insertions to visible reel-grid positions. */
public final class SymbolInsertion {
    private SymbolInsertion() {}

    /** One insertion rule: count distribution, reel/row heat map and placement restrictions. */
    public record Rule(Symbol symbol, List<WeightedTable.Entry<Integer>> countWeights,
            List<List<Long>> positionWeights, int maxPerReel,
            Set<Symbol> invalidReplacementSymbols) {
        public Rule {
            Objects.requireNonNull(symbol, "symbol");
            countWeights = List.copyOf(countWeights);
            positionWeights = positionWeights.stream().map(List::copyOf).toList();
            invalidReplacementSymbols = Set.copyOf(invalidReplacementSymbols);
            new WeightedTable<>(countWeights);
            if (maxPerReel <= 0) {
                throw new IllegalArgumentException("Maximum insertions per reel must be positive");
            }
            for (var entry : countWeights) {
                if (entry.value() < 0) {
                    throw new IllegalArgumentException("Insertion counts cannot be negative");
                }
            }
            if (positionWeights.isEmpty()
                    || positionWeights.stream().anyMatch(reel -> reel.isEmpty())
                    || positionWeights.stream().flatMap(List::stream).anyMatch(weight -> weight < 0)) {
                throw new IllegalArgumentException("Insertion heat map weights must be non-negative");
            }
        }
    }

    /** A symbol and zero-based visible position selected by an insertion rule. */
    public record Placement(Symbol symbol, int reel, int row) {
        public Placement {
            Objects.requireNonNull(symbol, "symbol");
            if (reel < 0 || row < 0) {
                throw new IllegalArgumentException("Insertion position cannot be negative");
            }
        }
    }

    /** The transformed grid and the placements used to produce it. */
    public record Result(ReelGrid grid, List<Placement> placements) {
        public Result {
            Objects.requireNonNull(grid, "grid");
            placements = List.copyOf(placements);
        }
    }

    /** Draws each rule's insertion count and places symbols without replacement. */
    public static Result apply(ReelGrid source, List<Rule> rules, Random random) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(rules, "rules");
        Objects.requireNonNull(random, "random");
        List<List<Symbol>> reels = mutableGrid(source);
        List<Placement> placements = new ArrayList<>();
        for (Rule rule : rules) {
            validateHeatMap(rule, source);
            int requestedCount = new WeightedTable<>(rule.countWeights()).draw(random);
            int[] countByReel = new int[source.getReelCount()];
            Set<Long> usedPositions = new HashSet<>();
            for (int inserted = 0; inserted < requestedCount; inserted++) {
                List<Candidate> candidates = eligiblePositions(
                        reels, rule, countByReel, usedPositions);
                if (candidates.isEmpty()) {
                    break;
                }
                Candidate selected = draw(candidates, random);
                reels.get(selected.reel()).set(selected.row(), rule.symbol());
                countByReel[selected.reel()]++;
                usedPositions.add(positionKey(selected.reel(), selected.row()));
                placements.add(new Placement(rule.symbol(), selected.reel(), selected.row()));
            }
        }
        return new Result(new ReelGrid(reels), placements);
    }

    /** Applies recorded placements without drawing randomness, for deterministic replay. */
    public static Result applyRecorded(ReelGrid source, List<Rule> rules,
            List<Placement> recordedPlacements) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(rules, "rules");
        Objects.requireNonNull(recordedPlacements, "recordedPlacements");
        List<List<Symbol>> reels = mutableGrid(source);
        List<Placement> applied = new ArrayList<>(recordedPlacements.size());
        int ruleIndex = 0;
        int[] countByReel = new int[source.getReelCount()];
        Set<Long> usedPositions = new HashSet<>();
        for (Placement placement : recordedPlacements) {
            while (ruleIndex < rules.size() && !rules.get(ruleIndex).symbol().equals(placement.symbol())) {
                ruleIndex++;
                countByReel = new int[source.getReelCount()];
                usedPositions.clear();
            }
            if (ruleIndex >= rules.size()) {
                throw new IllegalArgumentException("Replay contains an unconfigured inserted symbol");
            }
            Rule rule = rules.get(ruleIndex);
            validateHeatMap(rule, source);
            if (placement.reel() >= source.getReelCount()
                    || placement.row() >= source.getHeight()
                    || rule.invalidReplacementSymbols().contains(
                            reels.get(placement.reel()).get(placement.row()))
                    || countByReel[placement.reel()] >= rule.maxPerReel()
                    || !usedPositions.add(positionKey(placement.reel(), placement.row()))) {
                throw new IllegalArgumentException("Replay contains an invalid symbol insertion");
            }
            reels.get(placement.reel()).set(placement.row(), placement.symbol());
            countByReel[placement.reel()]++;
            applied.add(placement);
        }
        return new Result(new ReelGrid(reels), applied);
    }

    private static List<Candidate> eligiblePositions(List<List<Symbol>> reels, Rule rule,
            int[] countByReel, Set<Long> usedPositions) {
        List<Candidate> candidates = new ArrayList<>();
        for (int reel = 0; reel < reels.size(); reel++) {
            if (countByReel[reel] >= rule.maxPerReel()) {
                continue;
            }
            for (int row = 0; row < reels.get(reel).size(); row++) {
                long weight = rule.positionWeights().get(reel).get(row);
                if (weight > 0 && !rule.invalidReplacementSymbols().contains(reels.get(reel).get(row))
                        && !usedPositions.contains(positionKey(reel, row))) {
                    candidates.add(new Candidate(reel, row, weight));
                }
            }
        }
        return candidates;
    }

    private static Candidate draw(List<Candidate> candidates, Random random) {
        long totalWeight = 0;
        for (Candidate candidate : candidates) {
            totalWeight = Math.addExact(totalWeight, candidate.weight());
        }
        long ticket = totalWeight <= Integer.MAX_VALUE
                ? random.nextInt((int) totalWeight) : random.nextLong(totalWeight);
        for (Candidate candidate : candidates) {
            if (ticket < candidate.weight()) {
                return candidate;
            }
            ticket -= candidate.weight();
        }
        throw new IllegalStateException("Insertion heat map draw exceeded its weight range");
    }

    private static void validateHeatMap(Rule rule, ReelGrid source) {
        if (rule.positionWeights().size() != source.getReelCount()
                || rule.positionWeights().stream().anyMatch(
                        reel -> reel.size() != source.getHeight())) {
            throw new IllegalArgumentException(
                    "Insertion heat map must match the current reel grid dimensions");
        }
    }

    private static List<List<Symbol>> mutableGrid(ReelGrid source) {
        List<List<Symbol>> reels = new ArrayList<>(source.getReelCount());
        for (int reel = 0; reel < source.getReelCount(); reel++) {
            reels.add(new ArrayList<>(source.getReel(reel)));
        }
        return reels;
    }

    private static long positionKey(int reel, int row) {
        return ((long) reel << 32) | (row & 0xffffffffL);
    }

    private record Candidate(int reel, int row, long weight) {}
}
