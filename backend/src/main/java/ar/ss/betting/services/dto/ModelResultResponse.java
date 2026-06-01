package ar.ss.betting.services.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record ModelResultResponse(
        String modelName,
        Instant generatedAt,
        Integer totalCostInSek,
        Integer halfGuardsCount,
        Integer fullGuardsCount,
        Map<Integer, List<String>> selections,
        Map<Integer, String> basePicks,
        Map<Integer, ProbabilityTripleResponse> internalProbabilities
) {
    public record ProbabilityTripleResponse(
            Double homeWin,
            Double draw,
            Double awayWin
    ) { }
}
