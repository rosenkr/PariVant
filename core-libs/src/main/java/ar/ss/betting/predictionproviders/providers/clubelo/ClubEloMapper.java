package ar.ss.betting.predictionproviders.providers.clubelo;

import ar.ss.betting.predictionproviders.domain.MatchPrediction;
import ar.ss.betting.predictionproviders.domain.ProviderProbabilityTriple;
import org.springframework.stereotype.Component;


import java.time.*;
import java.time.format.DateTimeFormatter;
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

    private OffsetDateTime parseKickoff(String rawDate) {
        if (rawDate == null || rawDate.isBlank()) {
            return null;
        }

        List<DateTimeFormatter> formatters = List.of(
                DateTimeFormatter.ISO_OFFSET_DATE_TIME,
                DateTimeFormatter.ISO_LOCAL_DATE_TIME,
                DateTimeFormatter.ISO_LOCAL_DATE
        );

        for (DateTimeFormatter formatter : formatters) {
            try {
                if (formatter == DateTimeFormatter.ISO_OFFSET_DATE_TIME) {
                    return OffsetDateTime.parse(rawDate, formatter);
                }

                if (formatter == DateTimeFormatter.ISO_LOCAL_DATE_TIME) {
                    return LocalDateTime.parse(rawDate, formatter).atOffset(ZoneOffset.UTC);
                }

                if (formatter == DateTimeFormatter.ISO_LOCAL_DATE) {
                    return LocalDate.parse(rawDate, formatter)
                            .atStartOfDay()
                            .atOffset(ZoneOffset.UTC);
                }
            } catch (DateTimeParseException ignored) {
            }
        }

        return null;
    }
}