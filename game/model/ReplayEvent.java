package game.model;

import java.util.Objects;

/** One replayable game event, with game-owned opaque input needed to recalculate it. */
public record ReplayEvent(String type, double win, String payload) {
    public ReplayEvent {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(payload, "payload");
        if (type.isBlank() || type.contains("\n") || type.contains("\r")
                || payload.contains("\n") || payload.contains("\r") || !Double.isFinite(win)) {
            throw new IllegalArgumentException("Replay event type and win must be valid");
        }
    }

    public ReplayEvent withWin(double updatedWin) {
        return new ReplayEvent(type, updatedWin, payload);
    }
}
