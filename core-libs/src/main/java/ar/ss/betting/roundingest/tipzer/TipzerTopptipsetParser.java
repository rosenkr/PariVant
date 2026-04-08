package ar.ss.betting.roundingest.tipzer;

import ar.ss.betting.domain.RoundType;
import ar.ss.betting.roundingest.IngestedMatch;
import ar.ss.betting.roundingest.IngestedRound;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class TipzerTopptipsetParser {

    private static final Pattern VAR_TEXT_PATTERN =
            Pattern.compile("var\\s+text\\s*=\\s*(\\{.*?\\})\\s*;", Pattern.DOTALL);

    private final ObjectMapper objectMapper = new ObjectMapper();

    public IngestedRound parseFromPageHtml(String html) {
        Objects.requireNonNull(html, "html");

        String json = extractVarTextJson(html);
        JsonNode root = readJson(json);

        JsonNode draws = root.get("draws");
        if (draws == null || !draws.isArray()) {
            throw new IllegalArgumentException("Topptipset page JSON missing draws[]");
        }

        JsonNode draw = null;
        for (JsonNode d : draws) {
            JsonNode events = d.get("events");
            if (events != null && events.isArray() && events.size() == 8) {
                draw = d;
                break;
            }
        }

        if (draw == null) {
            throw new IllegalArgumentException("Could not find a Topptipset draw with 8 events");
        }

        Instant roundStart = inferRoundStart(draw);
        JsonNode events = draw.get("events");

        List<IngestedMatch> matches = new ArrayList<>(events.size());

        for (int i = 0; i < events.size(); i++) {
            int matchNumber = i + 1;
            JsonNode e = events.get(i);

            Instant kickoff = OffsetDateTime.parse(requireText(e, "sportEventStart")).toInstant();

            String home;
            String away;

            JsonNode participants = e.get("participants");
            if (participants != null && participants.isArray() && participants.size() >= 2) {
                home = requireText(participants.get(0), "name");
                away = requireText(participants.get(1), "name");
            } else {
                String desc = requireText(e, "description");
                String[] parts = desc.split("\\s+-\\s+");
                if (parts.length != 2) {
                    throw new IllegalArgumentException("Could not parse team names for Topptipset match " + matchNumber);
                }
                home = parts[0].trim();
                away = parts[1].trim();
            }

            Triple publicPick = extractTriplePercent(e.get("distribution"), "distribution");

            Triple market;
            if (e.hasNonNull("randomResultProbability")) {
                market = extractTriplePercent(e.get("randomResultProbability"), "randomResultProbability");
            } else if (e.hasNonNull("favouriteOdds")) {
                market = extractTriplePercent(e.get("favouriteOdds"), "favouriteOdds");
            } else if (e.hasNonNull("odds")) {
                market = deriveFromDecimalOdds(e.get("odds"));
            } else {
                throw new IllegalArgumentException("No market probability source found for Topptipset match " + matchNumber);
            }

            matches.add(new IngestedMatch(
                    matchNumber,
                    kickoff,
                    home,
                    away,
                    IngestedMatch.ProbabilityTriple.normalized(market.home(), market.draw(), market.away()),
                    IngestedMatch.ProbabilityTriple.normalized(publicPick.home(), publicPick.draw(), publicPick.away())
            ));
        }

        return new IngestedRound(RoundType.TOPPTIPSET, roundStart, matches);
    }

    private Instant inferRoundStart(JsonNode draw) {
        JsonNode events = draw.get("events");
        if (events == null || !events.isArray() || events.isEmpty()) {
            throw new IllegalArgumentException("Draw has no events");
        }

        Instant minKickoff = null;
        for (JsonNode e : events) {
            Instant kickoff = OffsetDateTime.parse(requireText(e, "sportEventStart")).toInstant();
            if (minKickoff == null || kickoff.isBefore(minKickoff)) {
                minKickoff = kickoff;
            }
        }

        if (minKickoff == null) {
            throw new IllegalArgumentException("Could not infer round start");
        }

        return minKickoff.minusSeconds(60);
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
        double homeOdds = parseDecimalComma(requireText(oddsNode, "home"));
        double drawOdds = parseDecimalComma(requireText(oddsNode, "draw"));
        double awayOdds = parseDecimalComma(requireText(oddsNode, "away"));

        double ih = 1.0 / homeOdds;
        double id = 1.0 / drawOdds;
        double ia = 1.0 / awayOdds;
        double sum = ih + id + ia;

        return new Triple(ih / sum, id / sum, ia / sum);
    }

    private double parseDecimalComma(String raw) {
        return Double.parseDouble(raw.trim().replace(",", "."));
    }

    private double parsePercent(JsonNode node, String label) {
        if (node == null || node.isNull()) {
            throw new IllegalArgumentException("Missing percent for " + label);
        }
        return Double.parseDouble(node.asText().trim().replace("%", ""));
    }

    private String requireText(JsonNode node, String field) {
        if (node == null || node.get(field) == null || node.get(field).isNull()) {
            throw new IllegalArgumentException("Missing field '" + field + "'");
        }
        return node.get(field).asText();
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
            throw new IllegalArgumentException("Failed to parse extracted Topptipset JSON", e);
        }
    }

    private record Triple(double home, double draw, double away) {
        private static Triple fromPercent(double hPct, double dPct, double aPct) {
            double h = hPct / 100.0;
            double d = dPct / 100.0;
            double a = aPct / 100.0;
            double sum = h + d + a;
            if (sum <= 1e-12) {
                throw new IllegalArgumentException("Invalid percent triple sum");
            }
            return new Triple(h / sum, d / sum, a / sum);
        }
    }
}