package ar.ss.betting.api.internal.ingest.tipzer;

import ar.ss.betting.domain.GameType;
import ar.ss.betting.service.ingest.IngestOrchestrator;
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
    public ResponseEntity<IngestOrchestrator.OrchestratorResult> ingestNextStryktipset() {
        var r = ingestOrchestrator.ingestNext(GameType.STRYKTIPSET, "/internal/ingest/tipzer/stryktipset/next");
        return toHttp(r);
    }

    @PostMapping("/europatipset/next")
    public ResponseEntity<IngestOrchestrator.OrchestratorResult> ingestNextEuropatipset() {
        var r = ingestOrchestrator.ingestNext(GameType.EUROPATIPSET, "/internal/ingest/tipzer/europatipset/next");
        return toHttp(r);
    }

    @PostMapping("/topptipset/next")
    public ResponseEntity<IngestOrchestrator.OrchestratorResult> ingestNextTopptipset() {
        var r = ingestOrchestrator.ingestNext(GameType.TOPPTIPSET, "/internal/ingest/tipzer/topptipset/next");
        return toHttp(r);
    }

    private static ResponseEntity<IngestOrchestrator.OrchestratorResult> toHttp(IngestOrchestrator.OrchestratorResult r) {
        if ("FAILED".equals(r.status())) {
            return ResponseEntity.status(502).body(r);
        }
        return ResponseEntity.ok(r);
    }
}