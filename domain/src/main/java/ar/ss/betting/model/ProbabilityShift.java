package ar.ss.betting.model;

import java.util.Objects;

/**
 * Additive probability delta over the three outcomes.
 *
 * Intended invariant:
 * homeDelta + drawDelta + awayDelta = 0
 *
 * This keeps the total mass stable before the final positivity clamp/normalization step.
 */
public class ProbabilityShift {

    private static final double EPSILON = 1e-9;

    private final double homeDelta;
    private final double drawDelta;
    private final double awayDelta;

    public ProbabilityShift(double homeDelta, double drawDelta, double awayDelta) {
        double sum = homeDelta + drawDelta + awayDelta;
        if (Math.abs(sum) > EPSILON) {
            throw new IllegalArgumentException("ProbabilityShift deltas must sum to 0");
        }

        this.homeDelta = homeDelta;
        this.drawDelta = drawDelta;
        this.awayDelta = awayDelta;
    }

    public double getHomeDelta() {
        return homeDelta;
    }

    public double getDrawDelta() {
        return drawDelta;
    }

    public double getAwayDelta() {
        return awayDelta;
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