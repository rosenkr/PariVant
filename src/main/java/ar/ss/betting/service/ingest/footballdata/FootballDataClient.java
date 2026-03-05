package ar.ss.betting.service.ingest.footballdata;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class FootballDataClient {

    private final RestClient client;

    // Cache: competitionCode -> (normalizedTeamName -> teamId)
    private final Map<String, CachedTeams> teamsCache = new ConcurrentHashMap<>();

    public FootballDataClient(RestClient footballDataRestClient) {
        this.client = Objects.requireNonNull(footballDataRestClient);
    }

    /**
     * Returns a "recent form score" in range 0..10 based on last 5 finished matches.
     * Scoring: win=2, draw=1, loss=0 (max=10).
     *
     * Requires teamId (use resolveTeamId(...) first).
     */
    public int getRecentFormScore(int teamId) {
        TeamMatchesResponse resp = client.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/teams/{id}/matches")
                        .queryParam("status", "FINISHED")
                        .queryParam("limit", 5)
                        .build(teamId))
                .retrieve()
                .body(TeamMatchesResponse.class);

        if (resp == null || resp.matches == null || resp.matches.isEmpty()) {
            return 0;
        }

        int points = 0;
        for (Match m : resp.matches) {
            // football-data returns "winner": HOME_TEAM / AWAY_TEAM / DRAW / null
            if (m.score == null || m.score.winner == null) continue;

            boolean isHome = m.homeTeam != null && m.homeTeam.id == teamId;
            boolean isAway = m.awayTeam != null && m.awayTeam.id == teamId;

            if (!isHome && !isAway) continue;

            switch (m.score.winner) {
                case "DRAW" -> points += 1;
                case "HOME_TEAM" -> points += isHome ? 2 : 0;
                case "AWAY_TEAM" -> points += isAway ? 2 : 0;
                default -> { /* ignore */ }
            }
        }

        // Clamp to [0,10]
        if (points < 0) return 0;
        if (points > 10) return 10;
        return points;
    }

    /**
     * Resolve a teamId from a competition code (e.g. PL, SA, PD, FL1, BL1) and teamName.
     * We cache competition teams for 24h to avoid burning API calls.
     */
    public int resolveTeamId(String competitionCode, String teamName) {
        if (competitionCode == null || competitionCode.isBlank()) {
            throw new IllegalArgumentException("competitionCode is required (e.g. PL/SA/PD/FL1/BL1)");
        }
        if (teamName == null || teamName.isBlank()) {
            throw new IllegalArgumentException("teamName is required");
        }

        CachedTeams cached = teamsCache.get(competitionCode);
        if (cached == null || cached.isExpired()) {
            cached = refreshTeamsCache(competitionCode);
            teamsCache.put(competitionCode, cached);
        }

        String key = normalize(teamName);
        Integer id = cached.nameToId.get(key);
        if (id == null) {
            // Try "contains" fallback (useful when providers differ slightly in naming)
            for (Map.Entry<String, Integer> e : cached.nameToId.entrySet()) {
                if (e.getKey().contains(key) || key.contains(e.getKey())) {
                    return e.getValue();
                }
            }
            throw new IllegalArgumentException("Could not resolve team '" + teamName + "' in competition " + competitionCode);
        }
        return id;
    }

    private CachedTeams refreshTeamsCache(String competitionCode) {
        CompetitionTeamsResponse resp = client.get()
                .uri("/competitions/{code}/teams", competitionCode)
                .retrieve()
                .body(CompetitionTeamsResponse.class);

        if (resp == null || resp.teams == null) {
            throw new IllegalStateException("football-data returned no teams for competition " + competitionCode);
        }

        Map<String, Integer> map = new HashMap<>();
        for (TeamRef t : resp.teams) {
            if (t == null || t.id == null || t.name == null) continue;
            map.put(normalize(t.name), t.id);
            // also store shortName if present
            if (t.shortName != null && !t.shortName.isBlank()) {
                map.putIfAbsent(normalize(t.shortName), t.id);
            }
        }

        return new CachedTeams(map, Instant.now().plus(Duration.ofHours(24)));
    }

    private static String normalize(String s) {
        return s.trim().toLowerCase(Locale.ROOT)
                .replace("fc ", "")
                .replace(" fc", "")
                .replaceAll("\\s+", " ");
    }

    private static final class CachedTeams {
        private final Map<String, Integer> nameToId;
        private final Instant expiresAt;

        private CachedTeams(Map<String, Integer> nameToId, Instant expiresAt) {
            this.nameToId = nameToId;
            this.expiresAt = expiresAt;
        }

        private boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }
    }

    // --- Minimal DTOs (match just the fields we use) ---

    public static final class CompetitionTeamsResponse {
        public List<TeamRef> teams;
    }

    public static final class TeamRef {
        public Integer id;
        public String name;
        public String shortName;
    }

    public static final class TeamMatchesResponse {
        public List<Match> matches;
    }

    public static final class Match {
        public TeamRef homeTeam;
        public TeamRef awayTeam;
        public Score score;
    }

    public static final class Score {
        public String winner; // "HOME_TEAM" | "AWAY_TEAM" | "DRAW" | null
    }
}