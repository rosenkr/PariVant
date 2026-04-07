package ar.ss.betting.model;

import java.util.Objects;

/**
 * Additive probability delta over the three outcomes.
 * <p>
 * Intended invariant:
 * homeDelta + drawDelta + awayDelta = 0
 * <p>
 * This keeps the total mass stable before the final positivity clamp/normalization step.
 */
public record ProbabilityShift(double homeDelta, double drawDelta, double awayDelta) {

    private static final double EPSILON = 1e-9;

    public ProbabilityShift {
        double sum = homeDelta + drawDelta + awayDelta;
        if (Math.abs(sum) > EPSILON) {
            throw new IllegalArgumentException("ProbabilityShift deltas must sum to 0");
        }

    }

    public ProbabilityShift plus(ProbabilityShift other) {
        Objects.requireNonNull(other, "other cannot be null");
        return new ProbabilityShift(
                this.homeDelta + other.homeDelta,
                this.drawDelta + other.drawDelta,
                this.awayDelta + other.awayDelta
        );
    }

    public static ProbabilityShift none() {
        return new ProbabilityShift(0.0, 0.0, 0.0);
    }
}