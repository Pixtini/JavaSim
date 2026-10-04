package GameModuleFramework.paylines;

import GameModuleFramework.reels.ReelGrid;
import GameModuleFramework.symbols.Symbol;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.ToDoubleFunction;

/** Evaluates and resolves paytable candidates across configured paylines. */
public final class PaylineWinCalculator {
    private final Paytable paytable;
    private final List<Payline> paylines;
    private final Symbol wildSymbol;
    private final LineWinSelectionPolicy selectionPolicy;

    public PaylineWinCalculator(Paytable paytable, List<Payline> paylines,
            Symbol wildSymbol, LineWinSelectionPolicy selectionPolicy) {
        this.paytable = Objects.requireNonNull(paytable, "paytable");
        this.paylines = List.copyOf(paylines);
        this.wildSymbol = Objects.requireNonNull(wildSymbol, "wildSymbol");
        this.selectionPolicy = Objects.requireNonNull(selectionPolicy, "selectionPolicy");
    }

    /** Calculates each candidate with the round stake and candidate-specific multiplier. */
    public List<EvaluatedLineWin> calculate(ReelGrid grid, double totalRoundStake,
            ToDoubleFunction<LineWin> multiplierForCandidate) {
        Objects.requireNonNull(grid, "grid");
        Objects.requireNonNull(multiplierForCandidate, "multiplierForCandidate");
        if (!Double.isFinite(totalRoundStake) || totalRoundStake <= 0.0) {
            throw new IllegalArgumentException("Total round stake must be finite and greater than zero");
        }

        List<EvaluatedLineWin> selectedWins = new ArrayList<>();
        for (int paylineIndex = 0; paylineIndex < paylines.size(); paylineIndex++) {
            List<EvaluatedLineWin> evaluatedCandidates = new ArrayList<>();
            for (LineWin candidate : PaylineEvaluator.candidates(
                    grid, paylines.get(paylineIndex), paytable, wildSymbol)) {
                double multiplier = multiplierForCandidate.applyAsDouble(candidate);
                if (!Double.isFinite(multiplier) || multiplier < 0.0) {
                    throw new IllegalArgumentException(
                            "Candidate multiplier must be finite and non-negative");
                }
                double baseWin = candidate.baseWin() * totalRoundStake;
                double totalWin = baseWin * multiplier;
                if (totalWin > 0.0) {
                    evaluatedCandidates.add(new EvaluatedLineWin(
                            paylineIndex + 1, candidate, baseWin, multiplier, totalWin));
                }
            }
            selectionPolicy.select(List.copyOf(evaluatedCandidates))
                    .ifPresent(selectedWins::add);
        }
        return List.copyOf(selectedWins);
    }
}
