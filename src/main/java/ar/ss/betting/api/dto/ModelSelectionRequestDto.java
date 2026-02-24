package ar.ss.betting.api.dto;

import java.util.List;
import java.util.Map;

/**
 * Request DTO for running the model.
 *
 * Designed as a Java record for immutability and conciseness.
 */
public record ModelSelectionRequestDto(
        String gameType,
        String roundStartDate,
        List<MatchDto> matches,
        Integer budgetInSek,
        Map<Integer, MatchContextDto> contexts,
        WeightsDto weights,
        DecisionParametersDto decisionParameters
) {

    public record MatchDto(
            Integer matchNumber,
            String startDate,
            String homeTeamName,
            String awayTeamName
    ) { }

    public record MatchContextDto(
            ProbabilityTripleDto market,
            ProbabilityTripleDto publicPick,
            Integer homeRecentFormScore,
            Integer awayRecentFormScore
    ) { }

    public record ProbabilityTripleDto(
            Double homeWin,
            Double draw,
            Double awayWin
    ) { }

    public record WeightsDto(
            Double recentFormWeight
    ) { }

    public record DecisionParametersDto(
            Double probabilityFloorTopptipset,
            Double probabilityFloorStryktipset,
            Double valueThresholdTopptipset,
            Double valueThresholdStryktipset,
            Integer maxFullGuardsTopptipset,
            Integer maxFullGuardsStryktipset
    ) { }
}