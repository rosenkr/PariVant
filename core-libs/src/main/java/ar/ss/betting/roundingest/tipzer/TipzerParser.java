package ar.ss.betting.roundingest.tipzer;

import ar.ss.betting.domain.RoundType;
import ar.ss.betting.roundingest.IngestedMatch;
import ar.ss.betting.roundingest.IngestedRound;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Component
public class TipzerParser {

    private static final int MATCH_COUNT = 13;
    private static final String MARKET_FALLBACK_REASON = "TIPZER_MARKET_ODDS_ALL_ZERO_USED_SVF";

    private final ObjectMapper objectMapper = new ObjectMapper();

    public IngestedRound parse(RoundType roundType, String teamsRaw, String svfRaw, String oddsRaw) {
        Objects.requireNonNull(roundType, "roundType");
        Objects.requireNonNull(teamsRaw, "teamsRaw");
        Objects.requireNonNull(svfRaw, "svfRaw");
        Objects.requireNonNull(oddsRaw, "oddsRaw");

        try {
            JsonNode teamsRoot = objectMapper.readTree(teamsRaw);
            JsonNode svfRoot = objectMapper.readTree(svfRaw);
            JsonNode oddsRoot = objectMapper.readTree(oddsRaw);

            if (!teamsRoot.isArray()) {
                throw new IllegalArgumentException("lagen.json must be an array");
            }
            if (!svfRoot.isArray()) {
                throw new IllegalArgumentException("svf.json must be an array");
            }
            if (!oddsRoot.isArray()) {
                throw new IllegalArgumentException("odds.json must be an array");
            }

            if (teamsRoot.size() != MATCH_COUNT + 1) {
                throw new IllegalArgumentException("lagen.json expected 14 items (13 matches + roundStart string)");
            }

            OffsetDateTime roundStart = OffsetDateTime.parse(teamsRoot.get(MATCH_COUNT).asText());

            List<TipzerMatchRow> matchesFromTeams = new ArrayList<>(MATCH_COUNT);
            for (int i = 0; i < MATCH_COUNT; i++) {
                JsonNode row = teamsRoot.get(i);

                if (!row.isArray() || row.size() < 3) {
                    throw new IllegalArgumentException(
                            "lagen.json match row " + (i + 1) + " must be [home, away, date]"
                    );
                }

                String home = row.get(0).asText();
                String away = row.get(1).asText();
                OffsetDateTime kickoff = OffsetDateTime.parse(row.get(2).asText());

                matchesFromTeams.add(new TipzerMatchRow(home, away, kickoff));
            }

            List<TipzerTriple> svf = parseTriples(svfRoot, "svf.json");
            List<TipzerTriple> odds = parseTriples(oddsRoot, "odds.json");

            if (svf.size() != MATCH_COUNT) {
                throw new IllegalArgumentException("svf.json expected 13 rows");
            }
            if (odds.size() != MATCH_COUNT) {
                throw new IllegalArgumentException("odds.json expected 13 rows");
            }

            List<IngestedMatch> matches = new ArrayList<>(MATCH_COUNT);

            for (int i = 0; i < MATCH_COUNT; i++) {
                TipzerMatchRow teams = matchesFromTeams.get(i);
                TipzerTriple publicPickRaw = svf.get(i);
                TipzerTriple marketRaw = odds.get(i);

                IngestedMatch.ProbabilityTriple publicPick =
                        IngestedMatch.ProbabilityTriple.normalized(
                                publicPickRaw.home(),
                                publicPickRaw.draw(),
                                publicPickRaw.away()
                        );

                boolean marketFallbackUsed = false;
                String marketFallbackReason = null;

                IngestedMatch.ProbabilityTriple market;
                if (marketRaw.isAllZero()) {
                    market = publicPick;
                    marketFallbackUsed = true;
                    marketFallbackReason = MARKET_FALLBACK_REASON;
                } else {
                    market = IngestedMatch.ProbabilityTriple.normalized(
                            marketRaw.home(),
                            marketRaw.draw(),
                            marketRaw.away()
                    );
                }

                matches.add(new IngestedMatch(
                        i + 1,
                        teams.kickoff(),
                        teams.home(),
                        teams.away(),
                        market,
                        publicPick,
                        marketFallbackUsed,
                        marketFallbackReason
                ));
            }

            return new IngestedRound(roundType, roundStart, matches);

        } catch (Exception e) {
            throw new IllegalArgumentException("Failed parsing Tipzer JSON: " + e.getMessage(), e);
        }
    }

    private List<TipzerTriple> parseTriples(JsonNode root, String label) {
        List<TipzerTriple> out = new ArrayList<>();

        for (int i = 0; i < root.size(); i++) {
            JsonNode row = root.get(i);

            if (!row.isArray() || row.size() < 3) {
                throw new IllegalArgumentException(label + " row " + (i + 1) + " must contain 3 entries");
            }

            double h = parsePercent(row.get(0).asText());
            double d = parsePercent(row.get(1).asText());
            double a = parsePercent(row.get(2).asText());

            out.add(new TipzerTriple(h, d, a));
        }

        return out;
    }

    private double parsePercent(String raw) {
        if (raw == null) {
            return 0.0;
        }

        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            return 0.0;
        }

        return Double.parseDouble(trimmed) / 100.0;
    }

    private record TipzerMatchRow(
            String home,
            String away,
            OffsetDateTime kickoff
    ) { }

    private record TipzerTriple(
            double home,
            double draw,
            double away
    ) {
        private boolean isAllZero() {
            return home == 0.0 && draw == 0.0 && away == 0.0;
        }
    }
}