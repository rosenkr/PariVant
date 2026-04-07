package ar.ss.betting;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class TeamNameExportTool {

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static void main(String[] args) {
        String truthFile = args.length >= 1 ? args[0] : "truth.txt";

        exportSafely("11elo_output.txt", TeamNameExportTool::export11Elo);
        exportSafely("bzzoiro_output.txt", TeamNameExportTool::exportBzzoiro);
        exportSafely("clubelo_output.txt", TeamNameExportTool::exportClubElo);
        exportSafely("api_football_output.txt", TeamNameExportTool::exportApiFootball);
        exportSafely("suggested_aliases.txt", () -> generateSuggestions(truthFile));
    }

    private static void exportSafely(String outputFileName, ThrowingRunnable runnable) {
        try {
            runnable.run();
            System.out.println("Wrote " + outputFileName);
        } catch (Exception e) {
            System.err.println("Failed writing " + outputFileName + ": " + e.getMessage());
            writeErrorFile(outputFileName, e);
        }
    }

    private static void export11Elo() throws Exception {
        String apiKey = requireConfig("ELEVEN_ELO_KEY");

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.11elo.com/api/matches/upcoming"))
                .header("X-API-Key", apiKey)
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        ensureSuccess(response, "11elo");

        JsonNode root = OBJECT_MAPPER.readTree(response.body());
        if (!root.isArray()) {
            throw new IllegalStateException("11elo response was not a JSON array");
        }

        Set<String> lines = new LinkedHashSet<>();
        for (JsonNode item : root) {
            String home = firstNonBlankText(item,
                    "homeTeamName", "home_team_name", "homeTeam", "home_team");
            String away = firstNonBlankText(item,
                    "awayTeamName", "away_team_name", "awayTeam", "away_team");

            if (!isBlank(home) && !isBlank(away)) {
                lines.add(home + "\t" + away);
            }
        }

        writeLines("11elo_output.txt", lines);
    }

    private static void exportBzzoiro() throws Exception {
        String apiKey = requireConfig("BZZOIRO_KEY");

        String nextUrl = "https://sports.bzzoiro.com/api/predictions/?upcoming=true&tz=UTC";
        Set<String> lines = new LinkedHashSet<>();

        while (!isBlank(nextUrl)) {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(nextUrl))
                    .header("Authorization", "Token " + apiKey)
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            ensureSuccess(response, "bzzoiro");

            JsonNode root = OBJECT_MAPPER.readTree(response.body());
            JsonNode results = root.path("results");

            if (results.isArray()) {
                for (JsonNode result : results) {
                    JsonNode event = result.path("event");

                    String home = firstNonBlankText(event,
                            "home_team", "homeTeam", "home_team_name", "homeTeamName");
                    String away = firstNonBlankText(event,
                            "away_team", "awayTeam", "away_team_name", "awayTeamName");

                    if (!isBlank(home) && !isBlank(away)) {
                        lines.add(home + "\t" + away);
                    }
                }
            }

            nextUrl = textOrNull(root.path("next"));
        }

        writeLines("bzzoiro_output.txt", lines);
    }

    private static void exportClubElo() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://api.clubelo.com/Fixtures"))
                .header("Accept", "text/csv")
                .GET()
                .build();

        HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        ensureSuccess(response, "clubelo");

        String body = response.body();
        String[] rows = body.split("\\R");

        if (rows.length == 0) {
            throw new IllegalStateException("ClubElo returned no rows");
        }

        Set<String> lines = new LinkedHashSet<>();

        List<String> header = null;
        int startRow = 0;

        if (looksLikeHeader(rows[0])) {
            header = parseCsvLine(rows[0]);
            startRow = 1;
        }

        for (int i = startRow; i < rows.length; i++) {
            String row = rows[i].trim();
            if (row.isEmpty()) {
                continue;
            }

            List<String> columns = parseCsvLine(row);
            String home = null;
            String away = null;

            if (header != null) {
                int homeIdx = findColumnIndex(header, "home");
                int awayIdx = findColumnIndex(header, "away");

                if (homeIdx >= 0 && awayIdx >= 0
                        && homeIdx < columns.size()
                        && awayIdx < columns.size()) {
                    home = columns.get(homeIdx).trim();
                    away = columns.get(awayIdx).trim();
                }
            }

            // Fallback for the observed ClubElo fixtures format:
            // date, competition, home, away, ...
            if (isBlank(home) || isBlank(away)) {
                if (columns.size() >= 4) {
                    home = columns.get(2).trim();
                    away = columns.get(3).trim();
                }
            }

            if (!isBlank(home) && !isBlank(away)) {
                lines.add(home + "\t" + away);
            }
        }

        writeLines("clubelo_output.txt", lines);
    }

    private static void exportApiFootball() throws Exception {
        String apiKey = requireConfig("API_FOOTBALL_KEY");

        Set<String> lines = new LinkedHashSet<>();
        LocalDate today = LocalDate.now(ZoneOffset.UTC);

        for (int i = 0; i < 7; i++) {
            LocalDate date = today.plusDays(i);

            String url = "https://v3.football.api-sports.io/fixtures"
                    + "?date=" + urlEncode(date.toString());

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("x-rapidapi-key", apiKey)
                    .header("x-rapidapi-host", "v3.football.api-sports.io")
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            ensureSuccess(response, "api-football");

            JsonNode root = OBJECT_MAPPER.readTree(response.body());
            JsonNode responseItems = root.path("response");

            if (!responseItems.isArray()) {
                continue;
            }

            for (JsonNode item : responseItems) {
                JsonNode fixture = item.path("fixture");
                JsonNode status = fixture.path("status");
                String shortStatus = textOrNull(status.path("short"));

                // keep only not-started / upcoming fixtures
                if (!"NS".equals(shortStatus) && !"TBD".equals(shortStatus)) {
                    continue;
                }

                JsonNode teams = item.path("teams");
                String home = firstNonBlankText(teams.path("home"), "name");
                String away = firstNonBlankText(teams.path("away"), "name");

                if (isBlank(home) || isBlank(away)) {
                    continue;
                }

                lines.add(home + "\t" + away);
            }
        }

        writeLines("api_football_output.txt", lines);
    }

    private static void generateSuggestions(String truthFile) throws Exception {
        List<TruthMatch> truthMatches = readTruthMatches(truthFile);

        List<String> output = new ArrayList<>();
        output.add("# Suggested alias candidates");
        output.add("# Review manually before adding");
        output.add("");

        Set<String> emitted = new LinkedHashSet<>();

        collectSuggestionsForProvider("11elo", "11elo_output.txt", truthMatches, emitted, output);
        collectSuggestionsForProvider("bzzoiro", "bzzoiro_output.txt", truthMatches, emitted, output);
        collectSuggestionsForProvider("clubelo", "clubelo_output.txt", truthMatches, emitted, output);
        collectSuggestionsForProvider("api-football", "api_football_output.txt", truthMatches, emitted, output);

        Files.write(Path.of("suggested_aliases.txt"), output, StandardCharsets.UTF_8);
    }

    private static void collectSuggestionsForProvider(
            String providerName,
            String providerFile,
            List<TruthMatch> truthMatches,
            Set<String> emitted,
            List<String> output
    ) throws IOException {
        Path path = Path.of(providerFile);
        if (!Files.exists(path)) {
            output.add("# Missing provider file: " + providerFile);
            output.add("");
            return;
        }

        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        for (String line : lines) {
            if (line == null || line.isBlank() || line.startsWith("#")) {
                continue;
            }

            ProviderMatch providerMatch = parseProviderMatch(line);
            if (providerMatch == null) {
                continue;
            }

            BestTruthMatch best = findBestTruthMatch(providerMatch, truthMatches);
            if (best == null) {
                continue;
            }

            emitAliasSuggestion(
                    providerName,
                    providerMatch.home,
                    best.matchedTruthHome,
                    providerMatch,
                    best.truth,
                    emitted,
                    output
            );

            emitAliasSuggestion(
                    providerName,
                    providerMatch.away,
                    best.matchedTruthAway,
                    providerMatch,
                    best.truth,
                    emitted,
                    output
            );
        }
    }

    private static void emitAliasSuggestion(
            String providerName,
            String rawName,
            String truthName,
            ProviderMatch providerMatch,
            TruthMatch truthMatch,
            Set<String> emitted,
            List<String> output
    ) {
        if (isBlank(rawName) || isBlank(truthName)) {
            return;
        }

        String rawNorm = normalize(rawName);
        String truthNorm = normalize(truthName);

        if (rawName.equalsIgnoreCase(truthName)) {
            return;
        }

        String dedupeKey = rawNorm + "->" + truthNorm;
        if (!emitted.add(dedupeKey)) {
            return;
        }

        output.add(
                "aliases.put(\"" + escapeJava(rawNorm) + "\", \"" + escapeJava(truthNorm) + "\"); "
                        + "inferred from " + providerName
                        + " \"" + providerMatch.home + " vs " + providerMatch.away + "\""
                        + " matched to truth \"" + truthMatch.home + "|" + truthMatch.away + "\""
        );
    }

    private static double subsetTokenContainmentScore(Set<String> a, Set<String> b) {
        if (a.isEmpty() || b.isEmpty()) {
            return 0.0;
        }

        Set<String> smaller = a.size() <= b.size() ? a : b;
        Set<String> larger = a.size() <= b.size() ? b : a;

        int matches = 0;
        for (String token : smaller) {
            if (larger.contains(token)) {
                matches++;
            }
        }

        if (matches == 0) {
            return 0.0;
        }

        // If every token in the shorter name exists in the longer name,
        // treat that as a very strong match.
        if (matches == smaller.size()) {
            return 0.92;
        }

        return (double) matches / smaller.size();
    }

    private static boolean isFullTokenSubset(Set<String> a, Set<String> b) {
        if (a.isEmpty() || b.isEmpty()) {
            return false;
        }

        Set<String> smaller = a.size() <= b.size() ? a : b;
        Set<String> larger = a.size() <= b.size() ? b : a;

        return larger.containsAll(smaller);
    }

    private static BestTruthMatch findBestTruthMatch(ProviderMatch providerMatch, List<TruthMatch> truthMatches) {
        BestTruthMatch best = null;

        for (TruthMatch truth : truthMatches) {
            double orderedHomeScore = nameSimilarity(providerMatch.home, truth.home);
            double orderedAwayScore = nameSimilarity(providerMatch.away, truth.away);
            double orderedPairScore = pairScore(orderedHomeScore, orderedAwayScore);

            if (orderedPairScore >= 1.40 && orderedHomeScore >= 0.55 && orderedAwayScore >= 0.55) {
                BestTruthMatch candidate = new BestTruthMatch(
                        truth,
                        truth.home,
                        truth.away,
                        orderedPairScore
                );
                if (best == null || candidate.score > best.score) {
                    best = candidate;
                }
            }

            double swappedHomeScore = nameSimilarity(providerMatch.home, truth.away);
            double swappedAwayScore = nameSimilarity(providerMatch.away, truth.home);
            double swappedPairScore = pairScore(swappedHomeScore, swappedAwayScore);

            if (swappedPairScore >= 1.40 && swappedHomeScore >= 0.55 && swappedAwayScore >= 0.55) {
                BestTruthMatch candidate = new BestTruthMatch(
                        truth,
                        truth.away,
                        truth.home,
                        swappedPairScore
                );
                if (best == null || candidate.score > best.score) {
                    best = candidate;
                }
            }
        }

        return best;
    }

    private static double pairScore(double first, double second) {
        double total = first + second;

        if (first >= 0.75 && second >= 0.75) {
            total += 0.20;
        }
        if (first >= 0.85 || second >= 0.85) {
            total += 0.05;
        }

        return total;
    }

    private static double nameSimilarity(String a, String b) {
        String na = normalize(a);
        String nb = normalize(b);

        if (na.equals(nb)) {
            return 1.0;
        }

        Set<String> ta = tokenSet(na);
        Set<String> tb = tokenSet(nb);

        if (isMeaningfulSubsetMatch(ta, tb)) {
            return 0.92;
        }

        double tokenOverlap = tokenOverlapScore(ta, tb);
        double containment = containmentScore(na, nb);
        double edit = normalizedLevenshteinSimilarity(na, nb);

        double best = Math.max(edit, Math.max(tokenOverlap, containment));

        if (looksLikeAbbreviationMatch(ta, tb, na, nb)) {
            best = Math.max(best, 0.78);
        }

        return best;
    }
    private static boolean isMeaningfulSubsetMatch(Set<String> a, Set<String> b) {
        if (!isFullTokenSubset(a, b)) {
            return false;
        }

        Set<String> smaller = a.size() <= b.size() ? a : b;
        if (smaller.size() >= 2) {
            return true;
        }

        String only = smaller.iterator().next();
        return only.length() >= 5
                && !only.equals("city")
                && !only.equals("united")
                && !only.equals("real")
                && !only.equals("sporting");
    }
    private static boolean looksLikeAbbreviationMatch(Set<String> ta, Set<String> tb, String na, String nb) {
        if (ta.isEmpty() || tb.isEmpty()) {
            return false;
        }

        for (String a : ta) {
            for (String b : tb) {
                if (a.length() <= 4 && b.contains(a) && b.length() > a.length()) {
                    return true;
                }
                if (b.length() <= 4 && a.contains(b) && a.length() > b.length()) {
                    return true;
                }
            }
        }

        return (na.length() <= 5 && nb.contains(na)) || (nb.length() <= 5 && na.contains(nb));
    }

    private static double tokenOverlapScore(Set<String> a, Set<String> b) {
        if (a.isEmpty() || b.isEmpty()) {
            return 0.0;
        }

        int intersection = 0;
        for (String token : a) {
            if (b.contains(token)) {
                intersection++;
            }
        }

        return (2.0 * intersection) / (a.size() + b.size());
    }

    private static double containmentScore(String a, String b) {
        if (a.contains(b) || b.contains(a)) {
            int min = Math.min(a.length(), b.length());
            int max = Math.max(a.length(), b.length());
            return max == 0 ? 0.0 : ((double) min / max);
        }
        return 0.0;
    }

    private static double normalizedLevenshteinSimilarity(String a, String b) {
        if (a.isEmpty() && b.isEmpty()) {
            return 1.0;
        }
        int distance = levenshteinDistance(a, b);
        int maxLen = Math.max(a.length(), b.length());
        return maxLen == 0 ? 1.0 : 1.0 - ((double) distance / maxLen);
    }

    private static int levenshteinDistance(String a, String b) {
        int[] prev = new int[b.length() + 1];
        int[] curr = new int[b.length() + 1];

        for (int j = 0; j <= b.length(); j++) {
            prev[j] = j;
        }

        for (int i = 1; i <= a.length(); i++) {
            curr[0] = i;
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                curr[j] = Math.min(
                        Math.min(curr[j - 1] + 1, prev[j] + 1),
                        prev[j - 1] + cost
                );
            }
            int[] tmp = prev;
            prev = curr;
            curr = tmp;
        }

        return prev[b.length()];
    }

    private static Set<String> tokenSet(String normalized) {
        Set<String> tokens = new HashSet<>();
        for (String token : normalized.split(" ")) {
            if (!token.isBlank()) {
                tokens.add(token);
            }
        }
        return tokens;
    }

    private static List<TruthMatch> readTruthMatches(String truthFile) throws IOException {
        Path path = Path.of(truthFile);
        if (!Files.exists(path)) {
            throw new IllegalStateException("Truth file not found: " + truthFile);
        }

        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        List<TruthMatch> result = new ArrayList<>();

        for (String line : lines) {
            if (line == null || line.isBlank() || line.startsWith("#")) {
                continue;
            }

            String[] parts = line.split("\\|", -1);
            if (parts.length < 2) {
                continue;
            }

            String home = parts[0].trim();
            String away = parts[1].trim();

            if (!isBlank(home) && !isBlank(away)) {
                result.add(new TruthMatch(home, away));
            }
        }

        return result;
    }

    private static ProviderMatch parseProviderMatch(String line) {
        String[] tabParts = line.split("\\t", -1);
        if (tabParts.length >= 2) {
            String home = tabParts[0].trim();
            String away = tabParts[1].trim();
            if (!isBlank(home) && !isBlank(away)) {
                return new ProviderMatch(home, away);
            }
        }

        String[] pipeParts = line.split("\\|", -1);
        if (pipeParts.length >= 2) {
            String home = pipeParts[0].trim();
            String away = pipeParts[1].trim();
            if (!isBlank(home) && !isBlank(away)) {
                return new ProviderMatch(home, away);
            }
        }

        return null;
    }

    private static boolean looksLikeHeader(String row) {
        String lower = row.toLowerCase(Locale.ROOT);
        return lower.contains("home") && lower.contains("away");
    }

    private static int findColumnIndex(List<String> header, String needle) {
        for (int i = 0; i < header.size(); i++) {
            String h = header.get(i).trim().toLowerCase(Locale.ROOT);
            if (h.equals(needle) || h.contains(needle)) {
                return i;
            }
        }
        return -1;
    }

    private static String normalize(String value) {
        String s = value.toLowerCase(Locale.ROOT);
        s = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        s = s.replace("&", " and ");
        s = s.replaceAll("[^a-z0-9 ]", " ");
        s = s.replaceAll("\\bfc\\b", " ");
        s = s.replaceAll("\\bcf\\b", " ");
        s = s.replaceAll("\\bafc\\b", " ");
        s = s.replaceAll("\\bclub\\b", " ");
        s = s.replaceAll("\\bfootball\\b", " ");
        s = s.replaceAll("\\bteam\\b", " ");
        s = s.replaceAll("\\bdeportivo\\b", "deportivo");
        s = s.replaceAll("\\butd\\b", "united");
        s = s.replaceAll("\\bst\\b", "saint");
        s = s.replaceAll("\\bsv\\b", " ");
        s = s.replaceAll("\\bsc\\b", " ");
        s = s.replaceAll("\\bac\\b", " ");
        s = s.replaceAll("\\bcd\\b", " ");
        s = s.replaceAll("\\bca\\b", " ");
        s = s.replaceAll("\\bif\\b", " ");
        s = s.replaceAll("\\bik\\b", " ");
        s = s.replaceAll("\\bfk\\b", " ");
        s = s.replaceAll("\\b1\\b", " ");
        s = s.replaceAll("\\b05\\b", " ");
        s = s.replaceAll("\\s+", " ").trim();
        return s;
    }

    private static String escapeJava(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static void ensureSuccess(HttpResponse<String> response, String providerName) {
        int status = response.statusCode();
        if (status < 200 || status >= 300) {
            throw new IllegalStateException(
                    providerName + " returned HTTP " + status + " with body: " + response.body()
            );
        }
    }

    private static String requireConfig(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            value = System.getProperty(name);
        }
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing environment variable or system property: " + name);
        }
        return value;
    }

    private static void writeLines(String fileName, Set<String> lines) throws IOException {
        Files.write(Path.of(fileName), new ArrayList<>(lines), StandardCharsets.UTF_8);
    }

    private static void writeErrorFile(String fileName, Exception e) {
        List<String> lines = List.of(
                "# ERROR: " + e.getClass().getSimpleName(),
                "# MESSAGE: " + safeMessage(e)
        );
        try {
            Files.write(Path.of(fileName), lines, StandardCharsets.UTF_8);
        } catch (IOException ioException) {
            System.err.println("Also failed to write error file " + fileName + ": " + ioException.getMessage());
        }
    }

    private static String safeMessage(Exception e) {
        return e.getMessage() == null ? "(no message)" : e.getMessage();
    }

    private static String firstNonBlankText(JsonNode node, String... fieldNames) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }

        for (String fieldName : fieldNames) {
            String value = textOrNull(node.path(fieldName));
            if (!isBlank(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private static String textOrNull(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        String text = node.asText();
        return text == null || text.isBlank() ? null : text;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static List<String> parseCsvLine(String line) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                result.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }

        result.add(current.toString());
        return result;
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }

    private static final class TruthMatch {
        private final String home;
        private final String away;

        private TruthMatch(String home, String away) {
            this.home = home;
            this.away = away;
        }
    }

    private static final class ProviderMatch {
        private final String home;
        private final String away;

        private ProviderMatch(String home, String away) {
            this.home = home;
            this.away = away;
        }
    }

    private static final class BestTruthMatch {
        private final TruthMatch truth;
        private final String matchedTruthHome;
        private final String matchedTruthAway;
        private final double score;

        private BestTruthMatch(TruthMatch truth, String matchedTruthHome, String matchedTruthAway, double score) {
            this.truth = truth;
            this.matchedTruthHome = matchedTruthHome;
            this.matchedTruthAway = matchedTruthAway;
            this.score = score;
        }
    }
}