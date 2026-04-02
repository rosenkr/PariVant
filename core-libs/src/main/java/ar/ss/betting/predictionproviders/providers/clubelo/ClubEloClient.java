package ar.ss.betting.predictionproviders.providers.clubelo;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class ClubEloClient {

    private static final String FIXTURES_URL = "http://api.clubelo.com/Fixtures";

    private final HttpClient httpClient = HttpClient.newHttpClient();

    public ClubEloResponse fetchFixtures() {
        Instant fetchedAt = Instant.now();

        String csvBody = downloadCsv();
        List<ClubEloFixtureRow> rows = parseCsv(csvBody);

        return ClubEloResponse.builder()
                .fetchedAt(fetchedAt)
                .fixtures(rows)
                .build();
    }

    private String downloadCsv() {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(FIXTURES_URL))
                .GET()
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new IllegalStateException("ClubElo request failed with status " + response.statusCode());
            }

            return response.body();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to fetch ClubElo fixtures CSV", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while fetching ClubElo fixtures CSV", e);
        }
    }

    private List<ClubEloFixtureRow> parseCsv(String csvBody) {
        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .build();

        try (StringReader reader = new StringReader(csvBody);
             CSVParser parser = new CSVParser(reader, format)) {

            List<ClubEloFixtureRow> rows = new ArrayList<>();

            for (CSVRecord record : parser) {
                rows.add(ClubEloFixtureRow.builder()
                        .home(get(record, "Home"))
                        .away(get(record, "Away"))
                        .date(getOptional(record, "Date"))

                        .gdMinusMoreThan5(getDouble(record, "GD<-5"))
                        .gdMinus5(getDouble(record, "GD=-5"))
                        .gdMinus4(getDouble(record, "GD=-4"))
                        .gdMinus3(getDouble(record, "GD=-3"))
                        .gdMinus2(getDouble(record, "GD=-2"))
                        .gdMinus1(getDouble(record, "GD=-1"))
                        .gd0(getDouble(record, "GD=0"))
                        .gd1(getDouble(record, "GD=1"))
                        .gd2(getDouble(record, "GD=2"))
                        .gd3(getDouble(record, "GD=3"))
                        .gd4(getDouble(record, "GD=4"))
                        .gd5(getDouble(record, "GD=5"))
                        .gdMoreThan5(getDouble(record, "GD>5"))
                        .build());
            }

            log.info("Parsed {} ClubElo fixture rows", rows.size());
            return rows;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to parse ClubElo fixtures CSV", e);
        }
    }

    private String get(CSVRecord record, String column) {
        return record.get(column).trim();
    }

    private String getOptional(CSVRecord record, String column) {
        if (!record.isMapped(column)) {
            return null;
        }
        String value = record.get(column);
        return value == null ? null : value.trim();
    }

    private double getDouble(CSVRecord record, String column) {
        String value = record.get(column).trim();
        return value.isEmpty() ? 0.0 : Double.parseDouble(value);
    }
}