package ar.ss.betting.model;

import ar.ss.betting.domain.Outcome;

import java.util.Objects;

/**
 * Chooses the base outcome for a match.
 *
 * score(outcome) = (internal - public) * exp(-k * (1 - internal))
 */
public class BaseOutcomeSelector {

    public Outcome chooseBaseOutcome(ProbabilityTriple internal,
                                     ProbabilityTriple publicProbabilities) {

        Objects.requireNonNull(internal, "internal cannot be null");
        Objects.requireNonNull(publicProbabilities, "publicProbabilities cannot be null");

        Outcome bestOutcome = null;
        double bestScore = Double.NEGATIVE_INFINITY;

        for (Outcome outcome : Outcome.values()) {
            double score = score(
                    internal.get(outcome),
                    publicProbabilities.get(outcome),
                    ModelConstants.BASE_PICK_AGGRESSIVENESS_K
            );

            if (score > bestScore) {
                bestScore = score;
                bestOutcome = outcome;
            }
        }

        return Objects.requireNonNull(bestOutcome, "bestOutcome cannot be null");
    }

    static double score(double internalProbability, double publicProbability, double k) {
        double value = internalProbability - publicProbability;
        return value * Math.exp(-k * (1.0 - internalProbability));
    }
}