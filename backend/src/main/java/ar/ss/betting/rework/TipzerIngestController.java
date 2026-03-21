package ar.ss.betting.rework;

import ar.ss.betting.domain.RoundType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;

@RestController
@RequestMapping("/internal/ingest/tipzer")
public class TipzerIngestController {

    private final IngestOrchestrator ingestOrchestrator;

    public TipzerIngestController(IngestOrchestrator ingestOrchestrator) {
        this.ingestOrchestrator = Objects.requireNonNull(ingestOrchestrator);
    }

    @PostMapping("/stryktipset/next")
    public ResponseEntity<?> ingestNextStryktipset() {
        var r = ingestOrchestrator.ingestNext(RoundType.STRYKTIPSET, "/internal/ingest/tipzer/stryktipset/next");
        return ResponseEntity.ok(r);
    }

    @PostMapping("/europatipset/next")
    public ResponseEntity<?> ingestNextEuropatipset() {
        var r = ingestOrchestrator.ingestNext(RoundType.EUROPATIPSET, "/internal/ingest/tipzer/europatipset/next");
        return ResponseEntity.ok(r);
    }

    @PostMapping("/topptipset/next")
    public ResponseEntity<?> ingestNextTopptipset() {
        var r = ingestOrchestrator.ingestNext(RoundType.TOPPTIPSET, "/internal/ingest/tipzer/topptipset/next");
        return ResponseEntity.ok(r);
    }
}