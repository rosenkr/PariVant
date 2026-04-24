package ar.ss.betting.services;

import ar.ss.betting.predictionproviders.service.PredictionQueryService;
import ar.ss.betting.predictionproviders.service.model.ProviderRawPredictionSnapshot;
import lombok.Getter;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

@Service
public class PredictionSnapshotCacheService {

    private final PredictionQueryService predictionQueryService;
    private final Clock clock;

    @Getter
    private volatile CachedPredictionSnapshot latestSnapshot =
            new CachedPredictionSnapshot(Instant.EPOCH, List.of());

    public PredictionSnapshotCacheService(PredictionQueryService predictionQueryService, Clock clock) {
        this.predictionQueryService = Objects.requireNonNull(predictionQueryService);
        this.clock = Objects.requireNonNull(clock);
    }

    @Scheduled(fixedDelay = 600_000)
    public void refresh() {
        List<ProviderRawPredictionSnapshot> snapshots = predictionQueryService.fetchProviderSnapshots();
        latestSnapshot = new CachedPredictionSnapshot(Instant.now(clock), List.copyOf(snapshots));
    }

    public record CachedPredictionSnapshot(
            Instant refreshedAt,
            List<ProviderRawPredictionSnapshot> snapshots
    ) { }
}