package toolkit.player;

import java.util.List;
import java.util.Objects;

/** Game-neutral, presentation-only data adapted from one game's calculated round result. */
public final class PlayerDisplayData {
    private PlayerDisplayData() {}

    public record SymbolCell(String label, String style) {
        public SymbolCell {
            Objects.requireNonNull(label, "label");
            Objects.requireNonNull(style, "style");
        }
    }

    /** Zero-based viewport coordinates; reel is the horizontal axis and row is vertical. */
    public record Position(int reel, int row) {
        public Position {
            if (reel < 0 || row < 0) {
                throw new IllegalArgumentException("Win positions cannot be negative");
            }
        }
    }

    /** One visualizable award, with optional connecting line for ordered paylines or ways. */
    public record Win(String description, double amount, List<Position> positions,
            boolean connectPositions) {
        public Win(String description, double amount, List<Position> positions) {
            this(description, amount, positions, true);
        }

        public Win {
            Objects.requireNonNull(description, "description");
            positions = List.copyOf(positions);
            if (!Double.isFinite(amount) || amount < 0.0) {
                throw new IllegalArgumentException("Displayed win must be finite and non-negative");
            }
        }
    }

    /** Reel-major symbol grid and calculated win details for one displayed spin. */
    public record Spin(String mode, List<List<SymbolCell>> reels,
            double totalWin, List<Win> wins, String details) {
        public Spin(String mode, List<List<SymbolCell>> reels, double totalWin, List<Win> wins) {
            this(mode, reels, totalWin, wins, "");
        }

        public Spin {
            Objects.requireNonNull(mode, "mode");
            Objects.requireNonNull(details, "details");
            reels = reels.stream().map(List::copyOf).toList();
            wins = List.copyOf(wins);
            if (reels.isEmpty() || reels.get(0).isEmpty()) {
                throw new IllegalArgumentException("A displayed spin needs a non-empty grid");
            }
            int rows = reels.get(0).size();
            if (reels.stream().anyMatch(reel -> reel.size() != rows)) {
                throw new IllegalArgumentException("Displayed reels must have equal visible heights");
            }
            if (!Double.isFinite(totalWin) || totalWin < 0.0) {
                throw new IllegalArgumentException("Displayed total win must be finite and non-negative");
            }
        }

        public int reelCount() {
            return reels.size();
        }

        public int rowCount() {
            return reels.get(0).size();
        }
    }

    /** One basegame result followed by any feature spins triggered by it. */
    public record Round(Spin baseGame, List<Spin> featureSpins,
            boolean featureTriggered, int awardedFreeGames) {
        public Round {
            Objects.requireNonNull(baseGame, "baseGame");
            featureSpins = List.copyOf(featureSpins);
            if (awardedFreeGames < 0) {
                throw new IllegalArgumentException("Awarded free games cannot be negative");
            }
        }

        public double totalWin() {
            return baseGame.totalWin()
                    + featureSpins.stream().mapToDouble(Spin::totalWin).sum();
        }
    }
}
