package game;

import game.model.SpinResult;
import java.util.List;

/** Game capability for evaluating every exact combination of reel stops. */
public interface ExhaustiveReelGame extends Game {
    int getReelCount();

    int getStopCount(int reelIndex);

    /** Creates an evaluator that may be owned by one worker thread. */
    StopEvaluator createStopEvaluator();

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
