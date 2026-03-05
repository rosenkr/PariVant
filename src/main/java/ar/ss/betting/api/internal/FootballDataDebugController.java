package ar.ss.betting.api.internal;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;

/**
 * Debug-only endpoints for verifying football-data.org connectivity + auth token wiring.
 *
 * IMPORTANT: keep this under /internal and remove/lock down for production later.
 */
@RestController
@RequestMapping("/internal/football-data")
public class FootballDataDebugController {

    private final RestClient footballDataRestClient;

    public FootballDataDebugController(
            @Qualifier("footballDataRestClient") RestClient footballDataRestClient
    ) {
        this.footballDataRestClient = footballDataRestClient;
    }

    /**
     * Simple "is the token + baseUrl working?" check.
     * football-data.org returns JSON; we return raw text to avoid DTO work.
     */
    @GetMapping("/competitions/raw")
    public ResponseEntity<String> competitionsRaw() {
        String body = footballDataRestClient
                .get()
                .uri("/competitions")
                .retrieve()
                .body(String.class);

        return ResponseEntity.ok(body);
    }

    /**
     * Another small endpoint that tends to be stable.
     * If this works, your RestClient + header injection is correct.
     */
    @GetMapping("/areas/raw")
    public ResponseEntity<String> areasRaw() {
        String body = footballDataRestClient
                .get()
                .uri("/areas")
                .retrieve()
                .body(String.class);

        return ResponseEntity.ok(body);
    }
}
