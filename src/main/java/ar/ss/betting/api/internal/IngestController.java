package ar.ss.betting.api.internal;

import ar.ss.betting.service.ingest.RoundIngestService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;

@RestController
@RequestMapping("/internal/ingest")
public class IngestController {

    private final RoundIngestService roundIngestService;

    public IngestController(RoundIngestService roundIngestService) {
        this.roundIngestService = Objects.requireNonNull(roundIngestService);
    }

    @PostMapping("/rounds")
    public ResponseEntity<RoundIngestService.IngestReport> ingestRounds() {
        return ResponseEntity.ok(roundIngestService.ingestAll());
    }
}