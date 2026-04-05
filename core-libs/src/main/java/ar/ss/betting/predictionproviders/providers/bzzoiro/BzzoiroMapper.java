package ar.ss.betting.predictionproviders.providers.bzzoiro;

import ar.ss.betting.predictionproviders.domain.MatchPrediction;
import ar.ss.betting.predictionproviders.domain.ProviderProbabilityTriple;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Objects;

@Component
public class BzzoiroMapper {

    public List<MatchPrediction> toMatchPredictions(BzzoiroResponse response) {
        return response.getPredictions().stream()
                .filter(row -> row.getEvent() != null)
                .filter(this::hasCompletePrediction)
                .map(row -> MatchPrediction.builder()
                        .provider("bzzoiro")
                        .homeTeam(row.getEvent().getHomeTeam())
                        .awayTeam(row.getEvent().getAwayTeam())
                        .kickoff(parseKickoff(row.getEvent().getEventDate()))
                        .kickoffRaw(row.getEvent().getEventDate())
                        .probabilities(toProbabilityTriple(row))
                        .fetchedAt(response.getFetchedAt())
                        .build())
                .toList();
    }

    private boolean hasCompletePrediction(BzzoiroResponse.BzzoiroPredictionRow row) {
        return Objects.nonNull(row.getProbHomeWin())
                && Objects.nonNull(row.getProbDraw())
                && Objects.nonNull(row.getProbAwayWin())
                && row.getEvent() != null
                && row.getEvent().getHomeTeam() != null
                && row.getEvent().getAwayTeam() != null;
    }

    private ProviderProbabilityTriple toProbabilityTriple(BzzoiroResponse.BzzoiroPredictionRow row) {
        return ProviderProbabilityTriple.of(
                row.getProbHomeWin() / 100.0,
                row.getProbDraw() / 100.0,
                row.getProbAwayWin() / 100.0
        );
    }

    private OffsetDateTime parseKickoff(String rawDate) {
        if (rawDate == null || rawDate.isBlank()) {
            return null;
        }

        try {
            return OffsetDateTime.parse(rawDate);
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}