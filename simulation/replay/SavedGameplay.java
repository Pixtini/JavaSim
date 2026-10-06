package simulation.replay;

import game.model.ReplayEvent;
import java.util.List;
import java.util.Objects;

/** Game-neutral saved round summary and the ordered game-owned events needed to replay it. */
public record SavedGameplay(long id, String gameId, double stake, double totalWin,
        double baseGameWin, double featureGameWin, List<ReplayEvent> events) {
    public SavedGameplay {
        if (id <= 0 || !Double.isFinite(stake) || stake <= 0.0
                || !Double.isFinite(totalWin) || !Double.isFinite(baseGameWin)
                || !Double.isFinite(featureGameWin)) {
            throw new IllegalArgumentException("Saved gameplay summary values are invalid");
        }
        Objects.requireNonNull(gameId, "gameId");
        if (gameId.isBlank()) {
            throw new IllegalArgumentException("Saved gameplay game ID must not be blank");
        }
        events = List.copyOf(events);
        if (events.isEmpty()) {
            throw new IllegalArgumentException("Saved gameplay must contain replay events");
        }
    }
}
