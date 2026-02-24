package ar.ss.betting.model;

import java.util.Objects;

/**
 * Holds user-configurable weights for truth-estimating signals.
 *
 * All weights are expected in range [0.0, 1.0].
 *
 * 0.0  -> signal ignored completely
 * 1.0  -> full influence of the signal
 *
 * More signals will be added over time (injuries, fatigue, psychology, etc.).
 */
public class AdjustmentWeights {

    private final double recentFormWeight;

    public AdjustmentWeights(double recentFormWeight) {
        validateRange(recentFormWeight, "recentFormWeight");
        this.recentFormWeight = recentFormWeight;
    }

    private void validateRange(double value, String name) {
        if (value < 0.0 || value > 1.0) {
            throw new IllegalArgumentException(name + " must be between 0.0 and 1.0");
        }
    }

    public double getRecentFormWeight() {
        return recentFormWeight;
    }

    /**
     * Convenience factory for "no adjustments".
     */
    public static AdjustmentWeights none() {
        return new AdjustmentWeights(0.0);
    }
}