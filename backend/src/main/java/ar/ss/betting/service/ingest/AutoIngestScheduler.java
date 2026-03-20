package ar.ss.betting.service.ingest;

import ar.ss.betting.domain.GameType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class AutoIngestScheduler {

    private static final Logger log = LoggerFactory.getLogger(AutoIngestScheduler.class);

    private final IngestOrchestrator ingestOrchestrator;

    public AutoIngestScheduler(IngestOrchestrator ingestOrchestrator) {
        this.ingestOrchestrator = Objects.requireNonNull(ingestOrchestrator);
    }

    @Scheduled(fixedDelay = 2 * 60 * 60 * 1000L) // 2 hours
    public void tick() {
        ingest(GameType.STRYKTIPSET, "/scheduler/ingest/stryktipset/next");
        ingest(GameType.EUROPATIPSET, "/scheduler/ingest/europatipset/next");
        ingest(GameType.TOPPTIPSET, "/scheduler/ingest/topptipset/next");
    }

    private void ingest(GameType type, String endpointLabel) {
        var r = ingestOrchestrator.ingestNext(type, endpointLabel);

        if ("SUCCESS".equals(r.status())) {
            log.info("[auto-ingest] {} OK via {} => {} (roundId={})",
                    type, r.source(), r.message(), r.outcome() == null ? null : r.outcome().roundId());
        } else {
            log.warn("[auto-ingest] {} FAILED => {}", type, r.message());
        }
    }
}