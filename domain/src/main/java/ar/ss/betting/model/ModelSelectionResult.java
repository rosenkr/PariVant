package ar.ss.betting.model;

import ar.ss.betting.domain.Outcome;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Output from a GameModel.
 *
 * This is intentionally separate from Coupon so the model remains a pure engine.
 */
public class ModelSelectionResult {

    private final String modelName;
    private final LocalDateTime generatedAt;
    private final Map<Integer, Set<Outcome>> selections;
    private final Map<Integer, ProbabilityTriple> internalProbabilities;
    private final int totalCostInSek;
    private final int halfGuardsCount;
    private final int fullGuardsCount;

    /**
     * Transitional constructor kept so older call sites/tests can still compile during migration.
     * Internal probabilities default to an empty map.
     */
    public ModelSelectionResult(String modelName,
                                LocalDateTime generatedAt,
                                Map<Integer, Set<Outcome>> selections,
                                int totalCostInSek,
                                int halfGuardsCount,
                                int fullGuardsCount) {
        this(
                modelName,
                generatedAt,
                selections,
                Map.of(),
                totalCostInSek,
                halfGuardsCount,
                fullGuardsCount
        );
    }

    public ModelSelectionResult(String modelName,
                                LocalDateTime generatedAt,
                                Map<Integer, Set<Outcome>> selections,
                                Map<Integer, ProbabilityTriple> internalProbabilities,
                                int totalCostInSek,
                                int halfGuardsCount,
                                int fullGuardsCount) {

        this.modelName = Objects.requireNonNull(modelName, "modelName cannot be null");
        this.generatedAt = Objects.requireNonNull(generatedAt, "generatedAt cannot be null");
        this.selections = Map.copyOf(Objects.requireNonNull(selections, "selections cannot be null"));
        this.internalProbabilities = Map.copyOf(
                Objects.requireNonNull(internalProbabilities, "internalProbabilities cannot be null")
        );

        if (totalCostInSek <= 0) {
            throw new IllegalArgumentException("totalCostInSek must be positive");
        }
        if (halfGuardsCount < 0 || fullGuardsCount < 0) {
            throw new IllegalArgumentException("guard counts cannot be negative");
        }

        this.totalCostInSek = totalCostInSek;
        this.halfGuardsCount = halfGuardsCount;
        this.fullGuardsCount = fullGuardsCount;
    }

    public String getModelName() {
        return modelName;
    }

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }

    public Map<Integer, Set<Outcome>> getSelections() {
        return Collections.unmodifiableMap(selections);
    }

    public Map<Integer, ProbabilityTriple> getInternalProbabilities() {
        return Collections.unmodifiableMap(internalProbabilities);
    }

    public int getTotalCostInSek() {
        return totalCostInSek;
    }

    public int getHalfGuardsCount() {
        return halfGuardsCount;
    }

    public int getFullGuardsCount() {
        return fullGuardsCount;
    }
}