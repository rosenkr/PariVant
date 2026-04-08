package ar.ss.betting.predictionproviders.providers.clubelo;

import ar.ss.betting.predictionproviders.domain.MatchPrediction;
import ar.ss.betting.predictionproviders.domain.ProviderProbabilityTriple;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.List;

@Component
public class ClubEloMapper {

    public List<MatchPrediction> toMatchPredictions(ClubEloResponse response) {
        return response.getFixtures().stream()
                .map(row -> MatchPrediction.builder()
                        .provider("clubelo")
                        .homeTeam(row.getHome())
                        .awayTeam(row.getAway())
                        .kickoff(parseKickoff(row.getDate()))
                        .kickoffRaw(row.getDate())
                        .probabilities(toProbabilityTriple(row))
                        .fetchedAt(response.getFetchedAt())
                        .build())
                .toList();
    }

    private ProviderProbabilityTriple toProbabilityTriple(ClubEloFixtureRow row) {
        double homeWin =
                row.getGd1() +
                        row.getGd2() +
                        row.getGd3() +
                        row.getGd4() +
                        row.getGd5() +
                        row.getGdMoreThan5();

        double draw = row.getGd0();

        double awayWin =
                row.getGdMinus1() +
                        row.getGdMinus2() +
                        row.getGdMinus3() +
                        row.getGdMinus4() +
                        row.getGdMinus5() +
                        row.getGdMinusMoreThan5();

        return ProviderProbabilityTriple.of(homeWin, draw, awayWin);
    }

    private Instant parseKickoff(String rawDate) {
        if (rawDate == null || rawDate.isBlank()) {
            return null;
        }

        try {
            return LocalDate.parse(rawDate)
                    .atTime(12, 0)
                    .atOffset(ZoneOffset.UTC)
                    .toInstant();
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}