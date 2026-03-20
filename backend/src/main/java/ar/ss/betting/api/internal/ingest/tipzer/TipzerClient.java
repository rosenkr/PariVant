package ar.ss.betting.api.internal.ingest.tipzer;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Objects;

@Component
public class TipzerClient {

    private static final String BASE_URL = "https://tipzer.se";

    // Stryktipset
    private static final String STRYK_TEAMS_PATH = "/lagen.json";
    private static final String STRYK_SVF_PATH = "/svf.json";
    private static final String STRYK_ODDS_PATH = "/odds.json";

    // Europatipset
    private static final String EURO_TEAMS_PATH = "/elagen.json";
    private static final String EURO_SVF_PATH = "/esvf.json";
    private static final String EURO_ODDS_PATH = "/eodds.json";

    // Topptipset (special: embedded JS in HTML)
    private static final String TOPP_PAGE_PATH = "/topptipset.php";

    private final RestClient restClient;

    public TipzerClient() {
        this.restClient = RestClient.builder()
                .baseUrl(BASE_URL)
                .build();
    }

    public String getStryktipsetTeamsRaw() {
        return getRaw(STRYK_TEAMS_PATH);
    }

    public String getStryktipsetSvenskaFolketRaw() {
        return getRaw(STRYK_SVF_PATH);
    }

    public String getStryktipsetOddsRaw() {
        return getRaw(STRYK_ODDS_PATH);
    }

    public String getEuropatipsetTeamsRaw() {
        return getRaw(EURO_TEAMS_PATH);
    }

    public String getEuropatipsetSvenskaFolketRaw() {
        return getRaw(EURO_SVF_PATH);
    }

    public String getEuropatipsetOddsRaw() {
        return getRaw(EURO_ODDS_PATH);
    }

    public String getTopptipsetPageRaw() {
        return getRaw(TOPP_PAGE_PATH);
    }

    private String getRaw(String path) {
        Objects.requireNonNull(path, "path");

        return restClient.get()
                .uri(path)
                .retrieve()
                .body(String.class);
    }
}