package ar.ss.betting.roundingest;

import ar.ss.betting.domain.RoundType;
import ar.ss.betting.roundingest.IngestedRound;
import ar.ss.betting.roundingest.RoundIngestSource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
public class IngestOrchestrator {

    private final List<RoundIngestSource> sources;
    private final RoundIngestService roundIngestService;

    public IngestOrchestrator(List<RoundIngestSource> sources,
                              RoundIngestService roundIngestService) {
        this.sources = List.copyOf(Objects.requireNonNull(sources));
        this.roundIngestService = Objects.requireNonNull(roundIngestService);
    }

    public OrchestratorResult ingestNext(RoundType roundType) {
        Objects.requireNonNull(roundType, "roundType");

        Exception last = null;

        for (RoundIngestSource source : sources) {
            if (!source.supports(roundType)) {
                continue;
            }

            try {
                IngestedRound ingestedRound = source.fetchRound(roundType);
                RoundIngestService.PersistResult persistResult =
                        roundIngestService.persistIfNew(ingestedRound);

                return OrchestratorResult.success(source.sourceName(), persistResult);
            } catch (Exception ex) {
                last = ex;
            }
        }

        if (last == null) {
            last = new IllegalStateException("No source available for " + roundType);
        }

        return OrchestratorResult.failed(roundType, last);
    }

    public record OrchestratorResult(
            String status,
            String source,
            RoundType roundType,
            String message,
            RoundIngestService.PersistResult outcome
    ) {
        public static OrchestratorResult success(String source, RoundIngestService.PersistResult outcome) {
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