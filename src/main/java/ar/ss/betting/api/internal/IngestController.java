package ar.ss.betting.api.internal;

import ar.ss.betting.api.internal.dto.IngestReportDto;
import ar.ss.betting.service.ingest.RoundIngestService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/ingest")
public class IngestController {

    private final RoundIngestService roundIngestService;

    public IngestController(RoundIngestService roundIngestService) {
        this.roundIngestService = roundIngestService;
    }

    @PostMapping("/rounds")
    public ResponseEntity<IngestReportDto> ingestRounds() {
        return ResponseEntity.ok(roundIngestService.ingestInbox());
    }
}