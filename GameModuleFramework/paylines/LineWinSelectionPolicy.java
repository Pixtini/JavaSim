package GameModuleFramework.paylines;

import java.util.List;
import java.util.Optional;

/** Selects which evaluated candidate, if any, pays on one payline. */
@FunctionalInterface
public interface LineWinSelectionPolicy {
    Optional<EvaluatedLineWin> select(List<EvaluatedLineWin> candidates);

    /** Selects the candidate with the greatest final payout, retaining the first on ties. */
    static LineWinSelectionPolicy highestPayout() {
        return candidates -> {
            EvaluatedLineWin best = null;
            for (EvaluatedLineWin candidate : candidates) {
                if (best == null || candidate.totalWin() > best.totalWin()) {
                    best = candidate;
                }
            }
            return Optional.ofNullable(best);
        };
    }
}
