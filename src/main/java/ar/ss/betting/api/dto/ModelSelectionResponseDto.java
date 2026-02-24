package ar.ss.betting.api.dto;

import java.util.List;
import java.util.Map;

/**
 * Response DTO for model selection.
 */
public record ModelSelectionResponseDto(
        String modelName,
        String generatedAt,
        Integer totalCostInSek,
        Integer halfGuardsCount,
        Integer fullGuardsCount,
        Map<Integer, List<String>> selections
) { }