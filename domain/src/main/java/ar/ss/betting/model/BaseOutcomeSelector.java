package ar.ss.betting.model;

import ar.ss.betting.domain.Outcome;

import java.util.Objects;

/**
 * Chooses the base outcome for a match.
 *
 * score(outcome) = p_i * log(p_i / q_i)
 * where:
 * - p_i = internal probability for the outcome
 * - q_i = public probability for the outcome
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
                    publicProbabilities.get(outcome)
            );

            if (score > bestScore) {
                bestScore = score;
                bestOutcome = outcome;
            }
        }

        return Objects.requireNonNull(bestOutcome, "bestOutcome cannot be null");
    }


    /**
     * Returns the per-outcome contribution to KL divergence D_KL(internal || public).
     *
     * For one outcome i, the score is:
     *   p_i * log(p_i / q_i)
     * where:
     * - p_i is the internal probability for the outcome
     * - q_i is the public probability for the outcome
     *
     * A larger positive score means the outcome is weighted more heavily by the
     * internal model than by the public distribution.
     */
    static double score(double internalProbability, double publicProbability) {
        if (internalProbability < 0.0 || internalProbability > 1.0) {
            throw new IllegalArgumentException("internalProbability must be between 0 and 1");
        }
        if (publicProbability <= 0.0 || publicProbability > 1.0) {
            throw new IllegalArgumentException("publicProbability must be in (0, 1]");
        }

        if (internalProbability == 0.0) {
            return 0.0;
        }

        return internalProbability * Math.log(internalProbability / publicProbability);
    }
}