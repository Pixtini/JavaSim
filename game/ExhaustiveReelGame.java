package game;

import game.model.SpinResult;
import java.util.List;

/** Game capability for evaluating every exact combination of reel stops. */
public interface ExhaustiveReelGame extends Game {
    int getReelCount();

    int getStopCount(int reelIndex);

    /** Number of independently configured reel sets; simple games have one. */
    default int getReelSetCount() {
        return 1;
    }

    /** Stop count for a reel in a selected set. */
    default int getStopCount(int setIndex, int reelIndex) {
        if (setIndex != 0) {
            throw new IndexOutOfBoundsException("Unknown reel set: " + setIndex);
        }
        return getStopCount(reelIndex);
    }

    /** Relative selector weight for a set, defaulting to one for single-set games. */
    default long getReelSetWeight(int setIndex) {
        if (setIndex != 0) {
            throw new IndexOutOfBoundsException("Unknown reel set: " + setIndex);
        }
        return 1L;
    }

    /** Creates an evaluator that may be owned by one worker thread. */
    StopEvaluator createStopEvaluator();

    /** Creates an evaluator for a particular reel set. */
    default StopEvaluator createStopEvaluator(int setIndex) {
        if (setIndex != 0) {
            throw new IndexOutOfBoundsException("Unknown reel set: " + setIndex);
        }
        return createStopEvaluator();
    }

    @FunctionalInterface
    interface StopEvaluator {
        /** Evaluates one zero-based stop index per reel without advancing randomness. */
        SpinResult evaluate(int[] stops, double totalRoundStake);
    }

    @Override
    default List<String> getAwardLabels() {
        return List.of();
    }
}
