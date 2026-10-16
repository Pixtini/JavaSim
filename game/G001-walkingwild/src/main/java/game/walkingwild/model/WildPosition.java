package game.walkingwild.model;

/** Zero-based reel and visible-row coordinate for a Freegame wild. */
public record WildPosition(int reel, int row) {
    public WildPosition {
        if (reel < 0 || row < 0) throw new IllegalArgumentException("Wild position cannot be negative");
    }
}
