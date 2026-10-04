package GameModuleFramework.probability;

import java.util.List;
import java.util.Objects;
import java.util.Random;

/** Immutable table for drawing values according to positive relative weights. */
public final class WeightedTable<T> {
    private final List<Entry<T>> entries;
    private final long[] cumulativeWeights;
    private final long totalWeight;

    public WeightedTable(List<Entry<T>> entries) {
        Objects.requireNonNull(entries, "entries");
        if (entries.isEmpty()) {
            throw new IllegalArgumentException("A weighted table must contain at least one entry");
        }

        this.entries = List.copyOf(entries);
        this.cumulativeWeights = new long[this.entries.size()];

        long weightTotal = 0;
        for (int index = 0; index < this.entries.size(); index++) {
            Entry<T> entry = Objects.requireNonNull(this.entries.get(index), "weighted table entry");
            if (entry.weight() <= 0) {
                throw new IllegalArgumentException("Weighted table weights must be positive");
            }
            try {
                weightTotal = Math.addExact(weightTotal, entry.weight());
            } catch (ArithmeticException exception) {
                throw new IllegalArgumentException("Total weighted table weight is too large", exception);
            }
            cumulativeWeights[index] = weightTotal;
        }
        this.totalWeight = weightTotal;
    }

    /** Draws one entry using the supplied random stream. */
    public T draw(Random random) {
        Objects.requireNonNull(random, "random");
        long ticket = totalWeight <= Integer.MAX_VALUE
                ? random.nextInt((int) totalWeight)
                : random.nextLong(totalWeight);

        int low = 0;
        int high = cumulativeWeights.length - 1;
        while (low < high) {
            int middle = (low + high) >>> 1;
            if (ticket < cumulativeWeights[middle]) {
                high = middle;
            } else {
                low = middle + 1;
            }
        }
        return entries.get(low).value();
    }

    public long getTotalWeight() {
        return totalWeight;
    }

    public List<Entry<T>> getEntries() {
        return entries;
    }

    /** A configured outcome and its positive relative weight. */
    public record Entry<T>(T value, long weight) {
        public Entry {
            Objects.requireNonNull(value, "value");
        }
    }
}
