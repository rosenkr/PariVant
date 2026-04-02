package ar.ss.betting.predictionproviders.providers._11elo;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
public class _11eloClient {

    private static final String UPCOMING_MATCHES_URL = "https://api.11elo.com/api/matches/upcoming";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;

    public _11eloClient(
            ObjectMapper objectMapper,
            @Value("${ELEVEN_ELO_KEY:}") String apiKey
    ) {
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
    }

    public _11eloResponse fetchUpcomingMatches() {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("Missing ELEVEN_ELO_KEY environment variable");
        }

        Instant fetchedAt = Instant.now();
        String body = downloadJson();
        List<_11eloMatchRow> matches = parseJson(body);

        return _11eloResponse.builder()
                .fetchedAt(fetchedAt)
                .matches(matches)
                .build();
    }

    private String downloadJson() {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(UPCOMING_MATCHES_URL))
                .header("X-API-Key", apiKey)
                .GET()
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new IllegalStateException("11elo request failed with status " + response.statusCode());
            }

            return response.body();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to fetch 11elo upcoming matches", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while fetching 11elo upcoming matches", e);
        }
    }

    private List<_11eloMatchRow> parseJson(String body) {
        try {
            _11eloMatchRow[] rows = objectMapper.readValue(body, _11eloMatchRow[].class);
            log.info("Parsed {} rows from 11elo", rows.length);
            return Arrays.asList(rows);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to parse 11elo upcoming matches JSON", e);
        }
    }
}