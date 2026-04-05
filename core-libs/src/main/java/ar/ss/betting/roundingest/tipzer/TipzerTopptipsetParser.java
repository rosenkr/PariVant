package ar.ss.betting.rework;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class TipzerTopptipsetParser {

    // Extracts: var text = { ... };
    private static final Pattern VAR_TEXT_PATTERN =
            Pattern.compile("var\\s+text\\s*=\\s*(\\{.*?\\})\\s*;", Pattern.DOTALL);

    private final ObjectMapper objectMapper = new ObjectMapper();

    public TopptipsetSnapshot parseFromPageHtml(String html) {
        Objects.requireNonNull(html, "html");

        String json = extractVarTextJson(html);
        JsonNode root = readJson(json);

        JsonNode draws = root.get("draws");
        if (draws == null || !draws.isArray()) {
            throw new IllegalArgumentException("Topptipset page JSON missing draws[]");
        }

        // Pick the draw that looks like Topptipset:
        // - events length == 8 is a strong signal
        JsonNode draw = null;
        for (JsonNode d : draws) {
            JsonNode events = d.get("events");
            if (events != null && events.isArray() && events.size() == 8) {
                draw = d;
                break;
            }
        }
        if (draw == null) {
            throw new IllegalArgumentException("Could not find a draw with 8 events (Topptipset) in var text JSON");
        }

        OffsetDateTime roundStart = extractRoundStartFromEvents(draw);

        JsonNode events = draw.get("events");
        List<TopptipsetMatch> matches = new ArrayList<>(8);
        List<Triple> publicTriples = new ArrayList<>(8);
        List<Triple> marketTriples = new ArrayList<>(8);

        for (int i = 0; i < events.size(); i++) {
            int matchNumber = i + 1;
            JsonNode e = events.get(i);

            OffsetDateTime kickoff = OffsetDateTime.parse(requireText(e, "sportEventStart"));

            // Teams: prefer participants[0..1].name
            String home = null;
            String away = null;
            JsonNode participants = e.get("participants");
            if (participants != null && participants.isArray() && participants.size() >= 2) {
                home = requireText(participants.get(0), "name");
                away = requireText(participants.get(1), "name");
            } else {
                // Fallback: description like "Roma - Juventus"
                String desc = requireText(e, "description");
                String[] parts = desc.split("\\s+-\\s+");
                if (parts.length == 2) {
                    home = parts[0].trim();
                    away = parts[1].trim();
                } else {
                    throw new IllegalArgumentException("Could not parse team names for match " + matchNumber);
                }
            }

            matches.add(new TopptipsetMatch(matchNumber, kickoff, home, away));

            // Svenska folket (distribution) appears as strings "82"/"10"/"8" (percent)
            Triple pub = extractTriplePercent(e.get("distribution"), "distribution");
            publicTriples.add(pub);

            // Market-ish triple: prefer randomResultProbability, else favouriteOdds, else derive from odds
            Triple market;

            if (e.hasNonNull("randomResultProbability")) {
                market = extractTriplePercent(e.get("randomResultProbability"), "randomResultProbability");
            } else if (e.hasNonNull("favouriteOdds")) {
                market = extractTriplePercent(e.get("favouriteOdds"), "favouriteOdds");
            } else if (e.hasNonNull("odds")) {
                market = deriveFromDecimalOdds(e.get("odds"));
            } else {
                throw new IllegalArgumentException("No market probability source found for match " + matchNumber);
            }

            marketTriples.add(market);
        }

        return new TopptipsetSnapshot(roundStart, matches, publicTriples, marketTriples);
    }

    /**
     * Tipzer's "lastDateWithoutTimeOfDay" tends to be a date boundary (midnight) and is not what we
     * want for a coupon's usable start time. Instead, we define roundStart as:
     *   roundStart = min(kickoffTimes) - 1 minute
     *
     * This matches how Tipzer defines roundStart for lagen.json (often one minute before first kickoff).
     */
    private OffsetDateTime extractRoundStartFromEvents(JsonNode draw) {
        JsonNode events = draw.get("events");
        if (events == null || !events.isArray() || events.isEmpty()) {
            throw new IllegalArgumentException("Draw has no events; cannot infer roundStart");
        }

        OffsetDateTime minKickoff = null;
        for (JsonNode e : events) {
            OffsetDateTime k = OffsetDateTime.parse(requireText(e, "sportEventStart"));
            if (minKickoff == null || k.isBefore(minKickoff)) {
                minKickoff = k;
            }
        }
        if (minKickoff == null) {
            throw new IllegalArgumentException("Cannot infer roundStart from events");
        }

        return minKickoff.minusMinutes(1);
    }

    private Triple extractTriplePercent(JsonNode node, String label) {
        if (node == null || !node.isObject()) {
            throw new IllegalArgumentException("Missing " + label + " object");
        }
        double h = parsePercent(node.get("home"), label + ".home");
        double d = parsePercent(node.get("draw"), label + ".draw");
        double a = parsePercent(node.get("away"), label + ".away");
        return Triple.fromPercent(h, d, a);
    }

    private Triple deriveFromDecimalOdds(JsonNode oddsNode) {
        // odds appear as strings like "1,23" (decimal comma)
        double homeOdds = parseDecimalComma(requireText(oddsNode, "home"));
        double drawOdds = parseDecimalComma(requireText(oddsNode, "draw"));
        double awayOdds = parseDecimalComma(requireText(oddsNode, "away"));

        // implied probs then normalize
        double ih = 1.0 / homeOdds;
        double id = 1.0 / drawOdds;
        double ia = 1.0 / awayOdds;
        double sum = ih + id + ia;
        return new Triple(ih / sum, id / sum, ia / sum);
    }

    private double parseDecimalComma(String s) {
        String norm = s.trim().replace(",", ".");
        return Double.parseDouble(norm);
    }

    private double parsePercent(JsonNode node, String label) {
        if (node == null || node.isNull()) {
            throw new IllegalArgumentException("Missing percent for " + label);
        }
        String raw = node.asText().trim();
        raw = raw.replace("%", "");
        return Double.parseDouble(raw);
    }

    private String requireText(JsonNode obj, String field) {
        if (obj == null || obj.get(field) == null || obj.get(field).isNull()) {
            throw new IllegalArgumentException("Missing field '" + field + "'");
        }
        return obj.get(field).asText();
    }

    private String extractVarTextJson(String html) {
        Matcher m = VAR_TEXT_PATTERN.matcher(html);
        if (!m.find()) {
            throw new IllegalArgumentException("Could not find 'var text = {...};' in topptipset.php");
        }
        return m.group(1);
    }

    private JsonNode readJson(String json) {
        try {
            return objectMapper.readTree(json);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse extracted var text JSON", e);
        }
    }

    public record TopptipsetSnapshot(
            OffsetDateTime roundStart,
            List<TopptipsetMatch> matches,
            List<Triple> publicPick,   // probabilities 0..1
            List<Triple> marketPick    // probabilities 0..1
    ) {}

    public record TopptipsetMatch(
            int matchNumber,
            OffsetDateTime kickoff,
            String home,
            String away
    ) {}

    public record Triple(double home, double draw, double away) {
        public static Triple fromPercent(double hPct, double dPct, double aPct) {
            double h = hPct / 100.0;
            double d = dPct / 100.0;
            double a = aPct / 100.0;
            double sum = h + d + a;
            if (sum <= 1e-12) throw new IllegalArgumentException("Invalid percent triple sum");
            return new Triple(h / sum, d / sum, a / sum);
        }
    }
}