package ar.ss.betting.predictionproviders.providers.bzzoiro;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Slf4j
@Component
public class BzzoiroClient {

    private static final String UPCOMING_PREDICTIONS_URL =
            "https://sports.bzzoiro.com/api/predictions/?upcoming=true&tz=UTC";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final Clock clock;
    public BzzoiroClient(
            ObjectMapper objectMapper,
            @Value("${BZZOIRO_KEY:}") String apiKey,
            Clock clock
    ) {
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.clock = Objects.requireNonNull(clock);
    }

    public BzzoiroResponse fetchUpcomingPredictions() {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("Missing BZZOIRO_KEY environment variable");
        }

        Instant fetchedAt = Instant.now(clock);
        List<BzzoiroResponse.BzzoiroPredictionRow> allRows = new ArrayList<>();

        String nextUrl = UPCOMING_PREDICTIONS_URL;
        while (nextUrl != null && !nextUrl.isBlank()) {
            String body = downloadJson(nextUrl);
            BzzoiroResponse.BzzoiroPage page = parseJson(body);

            if (page.getResults() != null) {
                allRows.addAll(page.getResults());
            }

            nextUrl = page.getNext();
        }

        log.info("Parsed {} rows from Bzzoiro", allRows.size());

        return BzzoiroResponse.builder()
                .fetchedAt(fetchedAt)
                .predictions(allRows)
                .build();
    }

    private String downloadJson(String url) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Token " + apiKey)
                .GET()
                .build();

        try {
            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new IllegalStateException(
                        "Bzzoiro request failed with status " + response.statusCode()
                );
            }

            return response.body();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to fetch Bzzoiro predictions", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while fetching Bzzoiro predictions", e);
        }
    }

    private BzzoiroResponse.BzzoiroPage parseJson(String body) {
        try {
            return objectMapper.readValue(body, BzzoiroResponse.BzzoiroPage.class);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to parse Bzzoiro predictions JSON", e);
        }
    }
}