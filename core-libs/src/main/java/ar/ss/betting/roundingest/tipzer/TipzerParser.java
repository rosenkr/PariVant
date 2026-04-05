package ar.ss.betting.rework;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Component
public class TipzerParser {

    private final ObjectMapper objectMapper;

    public TipzerParser(ObjectMapper objectMapper) {
        this.objectMapper = Objects.requireNonNull(objectMapper);
    }

    public TipzerSnapshot parse(String teamsRaw, String svfRaw, String oddsRaw) {
        try {
            JsonNode teamsRoot = objectMapper.readTree(teamsRaw);
            JsonNode svfRoot = objectMapper.readTree(svfRaw);
            JsonNode oddsRoot = objectMapper.readTree(oddsRaw);

            if (!teamsRoot.isArray()) throw new IllegalArgumentException("lagen.json must be an array");
            if (!svfRoot.isArray()) throw new IllegalArgumentException("svf.json must be an array");
            if (!oddsRoot.isArray()) throw new IllegalArgumentException("odds.json must be an array");

            // teamsRoot: 13 match arrays + final string roundStart
            if (teamsRoot.size() < 14) {
                throw new IllegalArgumentException("lagen.json expected 14 items (13 matches + roundStart string)");
            }

            String roundStartStr = teamsRoot.get(teamsRoot.size() - 1).asText();
            OffsetDateTime roundStart = OffsetDateTime.parse(roundStartStr);

            List<TipzerMatch> matches = new ArrayList<>(13);
            for (int i = 0; i < 13; i++) {
                JsonNode row = teamsRoot.get(i);
                if (!row.isArray() || row.size() < 3) {
                    throw new IllegalArgumentException("lagen.json match row " + (i + 1) + " must be [home, away, date]");
                }
                String home = row.get(0).asText();
                String away = row.get(1).asText();
                OffsetDateTime kickoff = OffsetDateTime.parse(row.get(2).asText());
                matches.add(new TipzerMatch(i + 1, home, away, kickoff));
            }

            List<TipzerTriple> svf = parseTriples(svfRoot, "svf.json");
            List<TipzerTriple> odds = parseTriples(oddsRoot, "odds.json");

            if (svf.size() != 13 || odds.size() != 13) {
                throw new IllegalArgumentException("svf/odds must have 13 rows each");
            }

            return new TipzerSnapshot(roundStart, matches, svf, odds);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed parsing Tipzer JSON: " + e.getMessage(), e);
        }
    }

    private List<TipzerTriple> parseTriples(JsonNode root, String label) {
        List<TipzerTriple> out = new ArrayList<>(13);

        for (int i = 0; i < root.size(); i++) {
            JsonNode row = root.get(i);
            if (!row.isArray() || row.size() < 3) {
                throw new IllegalArgumentException(label + " row " + (i + 1) + " must be array with first 3 entries as % strings");
            }
            double h = parsePercent(row.get(0).asText());
            double d = parsePercent(row.get(1).asText());
            double a = parsePercent(row.get(2).asText());
            out.add(new TipzerTriple(h, d, a));
        }

        return out;
    }

    private double parsePercent(String s) {
        // Tipzer gives "50" meaning 50%. Store as probability 0.50.
        double v = Double.parseDouble(s.trim());
        return v / 100.0;
    }

    public record TipzerSnapshot(
            OffsetDateTime roundStart,
            List<TipzerMatch> matches,
            List<TipzerTriple> svf,
            List<TipzerTriple> odds
    ) { }

    public record TipzerMatch(int matchNumber, String home, String away, OffsetDateTime kickoff) { }

    public record TipzerTriple(double home, double draw, double away) { }
}