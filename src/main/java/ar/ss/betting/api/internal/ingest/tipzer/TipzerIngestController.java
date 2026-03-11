package ar.ss.betting.api.internal.ingest.tipzer;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;

@RestController
@RequestMapping("/internal/ingest/tipzer")
public class TipzerIngestController {

    private final TipzerIngestService tipzerIngestService;

    public TipzerIngestController(TipzerIngestService tipzerIngestService) {
        this.tipzerIngestService = Objects.requireNonNull(tipzerIngestService);
    }

    /**
     * Ingest the next Stryktipset round from Tipzer JSON endpoints.
     * This persists:
     * - game_round + matches
     * - match_context rows (market probs + svenska folket probs + default form)
     *
     * Model runs are NOT created here — the scheduler should create OPENED/T_MINUS_15 runs.
     */
    @PostMapping("/stryktipset/next")
    public ResponseEntity<TipzerIngestService.IngestResult> ingestNextStryktipset() {
        return ResponseEntity.ok(tipzerIngestService.ingestNextStryktipsetRound());
    }
}