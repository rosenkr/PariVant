package ar.ss.betting.matchresolver;

import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Component
public class TeamNameNormalizer {

    // api -> truth, supports multiple api's with different naming, just add the proper alias
    private final Map<String, String> aliases = new HashMap<>();

    public TeamNameNormalizer() {
        aliases.put("man utd", "manchester united");
        aliases.put("man united", "manchester united");
        aliases.put("spurs", "tottenham hotspur");
        aliases.put("internazionale", "inter");
        aliases.put("wolves", "wolverhampton wanderers");
        aliases.put("wolverhampton", "wolverhampton wanderers");
        aliases.put("derby", "derby county");
        aliases.put("m gladbach", "borussia monchengladbach");
        aliases.put("borussia m gladbach", "borussia monchengladbach");
        aliases.put("ein frankfurt", "eintracht frankfurt");
        aliases.put("fc koln", "koln");
        aliases.put("dusseldorf", "fortuna dusseldorf");
        aliases.put("afc wimbledon", "wimbledon");
        aliases.put("oxford united", "oxford");
        aliases.put("sheffield utd", "sheffield u");
        aliases.put("qpr", "queens park rangers");
    }

    public String normalize(String teamName) {
        if (teamName == null) {
            return null;
        }

        String normalized = Normalizer.normalize(teamName, Normalizer.Form.NFKD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9 ]", " ")
                .replaceAll("\\s+", " ")
                .trim();

        if (normalized.endsWith(" ii")) {
            normalized = normalized.substring(0, normalized.length() - 3) + " b";
        }

        return aliases.getOrDefault(normalized, normalized);
    }
}