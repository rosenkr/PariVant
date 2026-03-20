package ar.ss.betting.service.ingest.providers;

import ar.ss.betting.api.internal.ingest.tipzer.TipzerIngestService;
import ar.ss.betting.domain.GameType;
import ar.ss.betting.service.ingest.IngestProvider;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
@Order(1) // Tipzer is primary provider for now
public class TipzerIngestProvider implements IngestProvider {

    private final TipzerIngestService tipzerIngestService;

    public TipzerIngestProvider(TipzerIngestService tipzerIngestService) {
        this.tipzerIngestService = Objects.requireNonNull(tipzerIngestService);
    }

    @Override
    public String sourceName() {
        return "TIPZER";
    }

    @Override
    public boolean supports(GameType gameType) {
        // Tipzer supports all 3 in your current codebase.
        return gameType == GameType.STRYKTIPSET
                || gameType == GameType.EUROPATIPSET
                || gameType == GameType.TOPPTIPSET;
    }

    @Override
    public IngestOutcome ingestNext(GameType gameType) {
        TipzerIngestService.IngestResult r = switch (gameType) {
            case STRYKTIPSET -> tipzerIngestService.ingestNextStryktipsetRound();
            case EUROPATIPSET -> tipzerIngestService.ingestNextEuropatipsetRound();
            case TOPPTIPSET -> tipzerIngestService.ingestNextTopptipsetRound();
        };

        if ("CREATED".equals(r.status())) {
            return IngestOutcome.created(r.gameType(), r.roundStartDate(), Objects.requireNonNull(r.roundId()));
        }
        if ("DUPLICATE".equals(r.status())) {
            return IngestOutcome.duplicate(r.gameType(), r.roundStartDate());
        }

        // Defensive: if TipzerIngestService ever changes statuses.
        throw new IllegalStateException("Unexpected ingest status from TipzerIngestService: " + r.status());
    }
}