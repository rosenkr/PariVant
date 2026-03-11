package ar.ss.betting.api.internal.ingest.tipzer;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class TipzerClient {

    private final RestClient restClient;

    public TipzerClient() {
        this.restClient = RestClient.builder()
                .baseUrl("https://tipzer.se")
                .build();
    }

    public String getTeamsRaw() {
        return restClient.get()
                .uri("/lagen.json")
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(String.class);
    }

    public String getSvenskaFolketRaw() {
        return restClient.get()
                .uri("/svf.json")
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(String.class);
    }

    public String getOddsRaw() {
        return restClient.get()
                .uri("/odds.json")
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(String.class);
    }
}