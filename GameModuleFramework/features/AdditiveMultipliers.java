package GameModuleFramework.features;

import java.util.Collection;

/** Combines active multiplier values additively; an empty set means the base 1x. */
public final class AdditiveMultipliers {
    private AdditiveMultipliers() {
    }

    public static double combine(Collection<? extends Number> multipliers) {
        if (multipliers.isEmpty()) {
            return 1.0;
        }
        return multipliers.stream().mapToDouble(Number::doubleValue).sum();
    }
}
