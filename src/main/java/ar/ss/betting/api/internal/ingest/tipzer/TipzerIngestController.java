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

    /**
     * Ingest the next Europatipset round from Tipzer JSON endpoints (elagen/esvf/eodds).
     * Persists the same data as Stryktipset:
     * - game_round + matches
     * - match_context rows
     *
     * Model runs are NOT created here — scheduler handles OPENED/T_MINUS_15 runs.
     */
    @PostMapping("/europatipset/next")
    public ResponseEntity<TipzerIngestService.IngestResult> ingestNextEuropatipset() {
        return ResponseEntity.ok(tipzerIngestService.ingestNextEuropatipsetRound());
    }

    @PostMapping("/topptipset/next")
    public ResponseEntity<TipzerIngestService.IngestResult> ingestNextTopptipset() {
        return ResponseEntity.ok(tipzerIngestService.ingestNextTopptipsetRound());
    }
}