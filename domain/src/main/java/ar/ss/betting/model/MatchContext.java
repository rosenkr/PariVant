package ar.ss.betting.model;

import java.util.List;
import java.util.Objects;

/**
 * Base model input for one match.
 *
 * Contains only:
 * - market probabilities
 * - public probabilities
 * - zero or more provider probability triples
 *
 * Recent-form-based inputs have been removed from the model.
 */
public class MatchContext {

    private final ProbabilityTriple marketProbabilities;
    private final ProbabilityTriple publicProbabilities;
    private final List<ProbabilityTriple> providerProbabilities;

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

    public ProbabilityTriple getMarketProbabilities() {
        return marketProbabilities;
    }

    public ProbabilityTriple getPublicProbabilities() {
        return publicProbabilities;
    }

    public List<ProbabilityTriple> getProviderProbabilities() {
        return providerProbabilities;
    }
}