package ar.ss.betting.services.dto;

import java.util.List;
import java.util.Map;

/**
 * Represents data needed to run the model
 *
 * Aligned with the current ensemble-based domain model:
 * - optional provider probabilities
 * - optional per-match tags / buff overlays
 */
public record ModelSelectionRequest(
        String roundType,
        String roundStartDate,
        List<MatchDto> matches,
        Integer budgetInSek,
        Map<Integer, MatchContextDto> contexts,
        Map<Integer, MatchInterventionsDto> interventions
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
            List<ProbabilityTripleDto> providers
    ) { }

    public record ProbabilityTripleDto(
            Double homeWin,
            Double draw,
            Double awayWin
    ) { }

    public record MatchInterventionsDto(
            List<TagDto> tags,
            BuffDto buff
    ) { }

    public record TagDto(
            String type,
            String affectedSide
    ) { }

    public record BuffDto(
            String targetOutcome,
            Integer points
    ) { }
}