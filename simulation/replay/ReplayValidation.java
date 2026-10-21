package simulation.replay;

import game.model.GameRoundResult;
import game.model.ReplayEvent;
import java.util.List;

/** Shared monetary/event validation for report replays and weighted gameplay books. */
public final class ReplayValidation {
    private ReplayValidation() {}
    public static boolean matches(SavedGameplay saved, GameRoundResult replayed) {
        double baseWin = replayed.getBaseGameResult().getWin();
        double featureWin = replayed.getFreeGameResults().stream()
                .mapToDouble(result -> result.getWin()).sum();
        double totalWin = baseWin + featureWin;
        if (!sameWin(totalWin, saved.totalWin())
                || !sameWin(baseWin, saved.baseGameWin())
                || !sameWin(featureWin, saved.featureGameWin())) {
            return false;
        }
        List<ReplayEvent> actualEvents = replayed.getReplayEvents();
        if (actualEvents.size() != saved.events().size()) {
            return false;
        }
        for (int index = 0; index < actualEvents.size(); index++) {
            if (!actualEvents.get(index).type().equals(saved.events().get(index).type())
                    || !sameWin(actualEvents.get(index).win(), saved.events().get(index).win())) {
                return false;
            }
        }
        return true;
    }

    private static boolean sameWin(double left, double right) {
        double tolerance = Math.max(1e-9, Math.max(Math.abs(left), Math.abs(right)) * 1e-10);
        return Math.abs(left - right) <= tolerance;
    }

}
