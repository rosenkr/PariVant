package ar.ss.betting.rework;

import ar.ss.betting.domain.RoundType;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
public class IngestOrchestrator {

    private final List<IngestProvider> providers;

    public IngestOrchestrator(List<IngestProvider> providers) {
        this.providers = List.copyOf(Objects.requireNonNull(providers));
    }

    public OrchestratorResult ingestNext(RoundType roundType, String endpointLabel) {
        Objects.requireNonNull(roundType, "roundType");
        Objects.requireNonNull(endpointLabel, "endpointLabel");

        Exception last = null;

        for (IngestProvider p : providers) {
            if (!p.supports(roundType)) {
                continue;
            }

            try {
                IngestProvider.IngestOutcome outcome = p.ingestNext(roundType);
                return OrchestratorResult.success(p.sourceName(), outcome);
            } catch (Exception ex) {
                last = ex;
            }
        }

        if (last == null) {
            last = new IllegalStateException("No provider available for " + roundType);
        }

        return OrchestratorResult.failed(roundType, last);
    }

    public record OrchestratorResult(
            String status,
            String source,
            RoundType roundType,
            String message,
            IngestProvider.IngestOutcome outcome
    ) {
        public static OrchestratorResult success(String source, IngestProvider.IngestOutcome outcome) {
            return new OrchestratorResult(
                    "SUCCESS",
                    source,
                    outcome.roundType(),
                    outcome.status(),
                    outcome
            );
        }

        public static OrchestratorResult failed(RoundType roundType, Exception ex) {
            String msg = ex.getClass().getSimpleName() + ": " + (ex.getMessage() == null ? "" : ex.getMessage());
            return new OrchestratorResult("FAILED", null, roundType, msg, null);
        }
    }
}