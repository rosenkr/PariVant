package ar.ss.betting.model;

import ar.ss.betting.domain.Outcome;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * Holds probabilities for HOME_WIN/DRAW/AWAY_WIN.
 * Assumes inputs are already normalized (sum ~ 1.0). We validate lightly.
 */
public class ProbabilityTriple {

    private final EnumMap<Outcome, Double> probs;

    public ProbabilityTriple(double homeWin, double draw, double awayWin) {
        this.probs = new EnumMap<>(Outcome.class);
        probs.put(Outcome.HOME_WIN, homeWin);
        probs.put(Outcome.DRAW, draw);
        probs.put(Outcome.AWAY_WIN, awayWin);

        validate();
    }

    private void validate() {
        for (Map.Entry<Outcome, Double> e : probs.entrySet()) {
            Double v = Objects.requireNonNull(e.getValue(), "Probability cannot be null");
            if (v < 0.0 || v > 1.0) {
                throw new IllegalArgumentException("Probability out of range for " + e.getKey() + ": " + v);
            }
        }

        double sum = probs.values().stream().mapToDouble(Double::doubleValue).sum();
        // Allow some tolerance for rounding / imperfect inputs.
        if (sum < 0.98 || sum > 1.02) {
            throw new IllegalArgumentException("Probabilities must sum to ~1.0, got: " + sum);
        }
    }

    public double get(Outcome outcome) {
        return probs.get(outcome);
    }

    public Outcome argMax() {
        Outcome best = null;
        double bestV = Double.NEGATIVE_INFINITY;

        for (Map.Entry<Outcome, Double> e : probs.entrySet()) {
            if (e.getValue() > bestV) {
                bestV = e.getValue();
                best = e.getKey();
            }
        }
        return best;
    }

    public EnumMap<Outcome, Double> asMap() {
        return new EnumMap<>(probs);
    }
}