package ar.ss.betting.api.internal;

import ar.ss.betting.persistence.entity.IngestAttemptEntity;
import ar.ss.betting.service.ingest.IngestAttemptService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/internal/ingest")
public class IngestAttemptController {

    private final IngestAttemptService ingestAttemptService;

    public IngestAttemptController(IngestAttemptService ingestAttemptService) {
        this.ingestAttemptService = Objects.requireNonNull(ingestAttemptService);
    }

    /**
     * List latest ingestion attempts.
     * Examples:
     *  - /internal/ingest/attempts?limit=50
     *  - /internal/ingest/attempts?status=FAILED&limit=50
     */
    @GetMapping("/attempts")
    public ResponseEntity<List<AttemptView>> list(
            @RequestParam(name="status", required = false) String status,
            @RequestParam(name="limit", required = false, defaultValue = "50") int limit
    ) {
        List<IngestAttemptEntity> rows = (status != null && status.equalsIgnoreCase("FAILED"))
                ? ingestAttemptService.latestFailed(limit)
                : ingestAttemptService.latest(limit);

        List<AttemptView> out = rows.stream().map(AttemptView::from).toList();
        return ResponseEntity.ok(out);
    }

    public record AttemptView(
            long id,
            String source,
            String gameType,
            String endpoint,
            String status,
            String reason,
            String createdAt
    ) {
        static AttemptView from(IngestAttemptEntity e) {
            return new AttemptView(
                    e.getId(),
                    e.getSource(),
                    e.getGameType(),
                    e.getEndpoint(),
                    e.getStatus(),
                    e.getReason(),
                    e.getCreatedAt().toString()
            );
        }
    }
}