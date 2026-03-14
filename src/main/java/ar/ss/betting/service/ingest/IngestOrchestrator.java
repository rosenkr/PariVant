package ar.ss.betting.service.ingest;

import ar.ss.betting.domain.GameType;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
public class IngestOrchestrator {

    private final List<IngestProvider> providers;
    private final IngestAttemptService ingestAttemptService;

    public IngestOrchestrator(List<IngestProvider> providers,
                              IngestAttemptService ingestAttemptService) {
        this.providers = List.copyOf(Objects.requireNonNull(providers));
        this.ingestAttemptService = Objects.requireNonNull(ingestAttemptService);
    }

    /**
     * Attempt ingestion for a game type.
     * Tries providers in injected order, records attempt rows for each.
     *
     * endpointLabel is stored in ingest_attempt.endpoint so you can distinguish
     * manual calls vs scheduler calls.
     */
    public OrchestratorResult ingestNext(GameType gameType, String endpointLabel) {
        Objects.requireNonNull(gameType, "gameType");
        Objects.requireNonNull(endpointLabel, "endpointLabel");

        Exception last = null;

        for (IngestProvider p : providers) {
            if (!p.supports(gameType)) continue;

            try {
                IngestProvider.IngestOutcome outcome = p.ingestNext(gameType);
                long attemptId = ingestAttemptService.recordSuccess(
                        p.sourceName(),
                        gameType.name(),
                        endpointLabel,
                        outcome.status()
                );
                return OrchestratorResult.success(attemptId, p.sourceName(), outcome);
            } catch (Exception ex) {
                last = ex;
                long attemptId = ingestAttemptService.recordFailure(
                        p.sourceName(),
                        gameType.name(),
                        endpointLabel,
                        ex
                );
                // In the future: if we have another provider, continue and try it.
                // For now, we continue the loop anyway, but only Tipzer exists.
            }
        }

        // No provider succeeded.
        if (last == null) last = new IllegalStateException("No provider available for " + gameType);
        return OrchestratorResult.failed(gameType, last);
    }

    public record OrchestratorResult(
            String status,     // SUCCESS | FAILED
            Long attemptId,    // attempt id for success (or null if none succeeded)
            String source,     // winning source name if success
            GameType gameType,
            String message,
            IngestProvider.IngestOutcome outcome
    ) {
        public static OrchestratorResult success(long attemptId, String source, IngestProvider.IngestOutcome outcome) {
            return new OrchestratorResult(
                    "SUCCESS",
                    attemptId,
                    source,
                    outcome.gameType(),
                    outcome.status(),
                    outcome
            );
        }

        public static OrchestratorResult failed(GameType gameType, Exception ex) {
            String msg = ex.getClass().getSimpleName() + ": " + (ex.getMessage() == null ? "" : ex.getMessage());
            return new OrchestratorResult("FAILED", null, null, gameType, msg, null);
        }
    }
}