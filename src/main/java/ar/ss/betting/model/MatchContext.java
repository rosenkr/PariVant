package ar.ss.betting.model;

import java.util.Objects;

/**
 * Model-side data for one match (keyed by matchNumber).
 * Contains the two probability sources we start with:
 * - Market implied probabilities
 * - Public pick distribution
 */
public class MatchContext {

    private final ProbabilityTriple marketProbabilities;
    private final ProbabilityTriple publicProbabilities;

    public MatchContext(ProbabilityTriple marketProbabilities,
                        ProbabilityTriple publicProbabilities) {

        this.marketProbabilities = Objects.requireNonNull(marketProbabilities, "marketProbabilities cannot be null");
        this.publicProbabilities = Objects.requireNonNull(publicProbabilities, "publicProbabilities cannot be null");
    }

    public ProbabilityTriple getMarketProbabilities() {
        return marketProbabilities;
    }

    public ProbabilityTriple getPublicProbabilities() {
        return publicProbabilities;
    }
}