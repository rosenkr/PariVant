package ar.ss.betting.predictionproviders.providers._11elo;

import ar.ss.betting.predictionproviders.domain.MatchPrediction;
import ar.ss.betting.predictionproviders.domain.ProbabilityTriple;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Objects;

@Component
public class _11eloMapper {

    public List<MatchPrediction> toMatchPredictions(_11eloResponse response) {
        return response.getMatches().stream()
                .filter(row -> row.getPrediction() != null)
                .filter(this::hasCompletePrediction)
                .map(row -> MatchPrediction.builder()
                        .provider("11elo")
                        .homeTeam(row.getHomeTeam())
                        .awayTeam(row.getAwayTeam())
                        .kickoff(parseKickoff(row.getDate()))
                        .kickoffRaw(row.getDate())
                        .probabilities(toProbabilityTriple(row.getPrediction()))
                        .fetchedAt(response.getFetchedAt())
                        .build())
                .toList();
    }

    private boolean hasCompletePrediction(_11eloMatchRow row) {
        _11eloPrediction prediction = row.getPrediction();
        return Objects.nonNull(prediction.getHomeWin())
                && Objects.nonNull(prediction.getDraw())
                && Objects.nonNull(prediction.getAwayWin());
    }

    private ProbabilityTriple toProbabilityTriple(_11eloPrediction prediction) {
        return ProbabilityTriple.of(
                prediction.getHomeWin() / 100.0,
                prediction.getDraw() / 100.0,
                prediction.getAwayWin() / 100.0
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