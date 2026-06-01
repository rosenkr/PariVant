package ar.ss.betting.model;

import java.util.Map;
import java.util.Objects;

public record ValueModelInput(
        Map<Integer, ProbabilityTriple> internalProbabilities,
        Map<Integer, ProbabilityTriple> publicProbabilities
) {
    public ValueModelInput {
        Objects.requireNonNull(internalProbabilities, "internalProbabilities cannot be null");
        Objects.requireNonNull(publicProbabilities, "publicProbabilities cannot be null");

        internalProbabilities = Map.copyOf(internalProbabilities);
        publicProbabilities = Map.copyOf(publicProbabilities);
    }

    public ProbabilityTriple getInternalProbabilities(int matchNumber) {
        ProbabilityTriple probabilities = internalProbabilities.get(matchNumber);
        if (probabilities == null) {
            throw new IllegalArgumentException("Missing internal probabilities for match number " + matchNumber);
        }
        return probabilities;
    }

    public ProbabilityTriple getPublicProbabilities(int matchNumber) {
        ProbabilityTriple probabilities = publicProbabilities.get(matchNumber);
        if (probabilities == null) {
            throw new IllegalArgumentException("Missing public probabilities for match number " + matchNumber);
        }
        return probabilities;
    }
}
