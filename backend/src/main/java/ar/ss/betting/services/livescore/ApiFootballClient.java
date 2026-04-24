package ar.ss.betting.services.livescore;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Minimal API-Football v3 client.
 *
 * We only call: GET /fixtures?live=all
 * Host: v3.football.api-sports.io
 * Header: x-apisports-key: <YOUR_KEY>
 *
 * Key is loaded from env var API_FOOTBALL_KEY.
 */
@Component
public class ApiFootballClient {

    private static final String BASE_URL = "https://v3.football.api-sports.io";
    private static final String ENV_KEY = "API_FOOTBALL_KEY";

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public ApiFootballClient(ObjectMapper objectMapper) {
        this.objectMapper = Objects.requireNonNull(objectMapper);

        String key = System.getenv(ENV_KEY);
        if (key == null || key.isBlank()) {
            throw new IllegalStateException("Missing API-Football key. Set env var " + ENV_KEY);
        }

        this.restClient = RestClient.builder()
                .baseUrl(BASE_URL)
                .defaultHeader("x-apisports-key", key)
                .build();
    }

    /**
     * Fetches all currently live fixtures.
     */
    public List<LiveFixture> getLiveFixtures() {
        String raw = restClient.get()
                .uri("/fixtures?live=all")
                .retrieve()
                .body(String.class);

        if (raw == null || raw.isBlank()) {
            return List.of();
        }

        try {
            JsonNode root = objectMapper.readTree(raw);

            JsonNode response = root.get("response");
            if (response == null || !response.isArray()) {
                return List.of();
            }

            List<LiveFixture> out = new ArrayList<>();
            for (JsonNode item : response) {
                JsonNode fixture = item.get("fixture");
                JsonNode teams = item.get("teams");
                JsonNode goals = item.get("goals");

                if (fixture == null || teams == null) continue;

                Long fixtureId = asLong(fixture.get("id"));
                String statusShort = asText(fixture.path("status").path("short"));
                Integer elapsed = asInt(fixture.path("status").path("elapsed"));

                String homeName = asText(teams.path("home").path("name"));
                String awayName = asText(teams.path("away").path("name"));

                Integer homeGoals = goals != null ? asInt(goals.get("home")) : null;
                Integer awayGoals = goals != null ? asInt(goals.get("away")) : null;

                if (fixtureId == null || homeName == null || awayName == null) continue;

                out.add(new LiveFixture(
                        fixtureId,
                        homeName,
                        awayName,
                        statusShort,
                        elapsed,
                        homeGoals,
                        awayGoals
                ));
            }

            return out;
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed parsing API-Football live fixtures JSON: " + e.getMessage(), e);
        }
    }

    private static String asText(JsonNode n) {
        if (n == null || n.isNull()) return null;
        String s = n.asText();
        return (s == null || s.isBlank()) ? null : s;
    }

    private static Integer asInt(JsonNode n) {
        if (n == null || n.isNull()) return null;
        if (!n.isNumber()) return null;
        return n.asInt();
    }

    private static Long asLong(JsonNode n) {
        if (n == null || n.isNull()) return null;
        if (!n.isNumber()) return null;
        return n.asLong();
    }

    /**
     * Minimal live fixture view extracted from API-Football.
     */
    public record LiveFixture(
            long fixtureId,
            String homeTeamName,
            String awayTeamName,
            String statusShort,
            Integer elapsedMinutes,
            Integer homeGoals,
            Integer awayGoals
    ) { }
}