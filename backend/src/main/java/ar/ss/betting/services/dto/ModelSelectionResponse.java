package ar.ss.betting.services.dto;

import java.util.List;
import java.util.Map;

/**
 * Represents the result of running the model.
 *
 * Aligned with the current domain model:
 * - includes selections
 * - includes internal probabilities per match
 */
public record ModelSelectionResponse(
        String modelName,
        String generatedAt,
        Integer totalCostInSek,
        Integer halfGuardsCount,
        Map<Integer, List<String>> selections,
        Map<Integer, ProbabilityTripleDto> internalProbabilities
) {
    public record ProbabilityTripleDto(
            Double homeWin,
            Double draw,
            Double awayWin
    ) { }
}