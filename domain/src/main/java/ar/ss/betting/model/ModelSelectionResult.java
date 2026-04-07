package ar.ss.betting.model;

import ar.ss.betting.domain.Outcome;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public record ModelSelectionResult(String modelName, LocalDateTime generatedAt, Map<Integer, Outcome> basePicks,
                                   Map<Integer, Set<Outcome>> selections,
                                   Map<Integer, ProbabilityTriple> internalProbabilities, int totalCostInSek,
                                   int halfGuardsCount, int fullGuardsCount) {

    public ModelSelectionResult(String modelName,
                                LocalDateTime generatedAt,
                                Map<Integer, Outcome> basePicks,
                                Map<Integer, Set<Outcome>> selections,
                                Map<Integer, ProbabilityTriple> internalProbabilities,
                                int totalCostInSek,
                                int halfGuardsCount,
                                int fullGuardsCount) {
        this.modelName = Objects.requireNonNull(modelName, "modelName cannot be null");
        this.generatedAt = Objects.requireNonNull(generatedAt, "generatedAt cannot be null");
        this.basePicks = Map.copyOf(Objects.requireNonNull(basePicks, "basePicks cannot be null"));
        this.selections = Map.copyOf(Objects.requireNonNull(selections, "selections cannot be null"));
        this.internalProbabilities = Map.copyOf(
                Objects.requireNonNull(internalProbabilities, "internalProbabilities cannot be null")
        );

        if (totalCostInSek <= 0) {
            throw new IllegalArgumentException("totalCostInSek must be positive");
        }
        if (halfGuardsCount < 0) {
            throw new IllegalArgumentException("halfGuardsCount cannot be negative");
        }
        if (fullGuardsCount < 0) {
            throw new IllegalArgumentException("fullGuardsCount cannot be negative");
        }

        this.totalCostInSek = totalCostInSek;
        this.halfGuardsCount = halfGuardsCount;
        this.fullGuardsCount = fullGuardsCount;
    }

    @Override
    public Map<Integer, Outcome> basePicks() {
        return Collections.unmodifiableMap(basePicks);
    }

    @Override
    public Map<Integer, Set<Outcome>> selections() {
        return Collections.unmodifiableMap(selections);
    }

    @Override
    public Map<Integer, ProbabilityTriple> internalProbabilities() {
        return Collections.unmodifiableMap(internalProbabilities);
    }
}