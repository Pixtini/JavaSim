package GameModuleFramework.paylines;

/** A selected payline award after stake and any line multiplier have been applied. */
public record EvaluatedLineWin(
        int paylineNumber,
        LineWin candidate,
        double baseWin,
        double multiplier,
        double totalWin) {
}
