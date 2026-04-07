package ar.ss.betting.model;

import java.util.List;
import java.util.Objects;

/**
 * Base model input for one match.
 * <p>
 * Contains only:
 * - market probabilities
 * - public probabilities
 * - zero or more provider probability triples
 * <p>
 * Recent-form-based inputs have been removed from the model.
 */
public record MatchContext(ProbabilityTriple marketProbabilities, ProbabilityTriple publicProbabilities,
                           List<ProbabilityTriple> providerProbabilities) {

    public MatchContext(ProbabilityTriple marketProbabilities,
                        ProbabilityTriple publicProbabilities,
                        List<ProbabilityTriple> providerProbabilities) {

        this.marketProbabilities = Objects.requireNonNull(marketProbabilities, "marketProbabilities cannot be null");
        this.publicProbabilities = Objects.requireNonNull(publicProbabilities, "publicProbabilities cannot be null");
        this.providerProbabilities = List.copyOf(
                Objects.requireNonNull(providerProbabilities, "providerProbabilities cannot be null")
        );

        for (ProbabilityTriple providerProbability : this.providerProbabilities) {
            Objects.requireNonNull(providerProbability, "providerProbabilities cannot contain null entries");
        }
    }
}