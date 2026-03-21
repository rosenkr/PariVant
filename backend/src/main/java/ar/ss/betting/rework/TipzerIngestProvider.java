package ar.ss.betting.rework;

import ar.ss.betting.domain.RoundType;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
@Order(1)
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
    public boolean supports(RoundType roundType) {
        return roundType == RoundType.STRYKTIPSET
                || roundType == RoundType.EUROPATIPSET
                || roundType == RoundType.TOPPTIPSET;
    }

    @Override
    public IngestOutcome ingestNext(RoundType roundType) {
        TipzerIngestService.IngestResult r = switch (roundType) {
            case STRYKTIPSET -> tipzerIngestService.ingestNextStryktipsetRound();
            case EUROPATIPSET -> tipzerIngestService.ingestNextEuropatipsetRound();
            case TOPPTIPSET -> tipzerIngestService.ingestNextTopptipsetRound();
        };

        if ("CREATED".equals(r.status())) {
            return IngestOutcome.created(r.roundType(), r.roundStartDate(), Objects.requireNonNull(r.roundId()));
        }
        if ("DUPLICATE".equals(r.status())) {
            return IngestOutcome.duplicate(r.roundType(), r.roundStartDate());
        }

        throw new IllegalStateException("Unexpected ingest status from TipzerIngestService: " + r.status());
    }
}