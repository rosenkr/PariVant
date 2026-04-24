package ar.ss.betting.controllers;

import ar.ss.betting.persistence.entity.ModelRunEntity;
import ar.ss.betting.persistence.entity.ModelRunProviderPredictionEntity;
import ar.ss.betting.persistence.repo.ModelRunProviderPredictionRepository;
import ar.ss.betting.persistence.repo.ModelRunRepository;
import ar.ss.betting.persistence.repo.RoundRepository;
import ar.ss.betting.services.RoundApiService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/public/rounds")
public class PublicModelRunController {

    private final RoundApiService roundApiService;
    private final RoundRepository roundRepository;
    private final ModelRunRepository modelRunRepository;
    private final ModelRunProviderPredictionRepository modelRunProviderPredictionRepository;

    public PublicModelRunController(RoundApiService roundApiService,
                                    RoundRepository roundRepository,
                                    ModelRunRepository modelRunRepository,
                                    ModelRunProviderPredictionRepository modelRunProviderPredictionRepository) {
        this.roundApiService = Objects.requireNonNull(roundApiService);
        this.roundRepository = Objects.requireNonNull(roundRepository);
        this.modelRunRepository = Objects.requireNonNull(modelRunRepository);
        this.modelRunProviderPredictionRepository = Objects.requireNonNull(modelRunProviderPredictionRepository);
    }

    /**
     * Public read-only endpoint: latest model run per preset budget (32/64/128/256) for a round.
     */
    @GetMapping("/{roundId}/model-runs")
    public ResponseEntity<List<RoundApiService.ModelRunView>> getLatestPresetRuns(@PathVariable long roundId) {
        return ResponseEntity.ok(roundApiService.getLatestPresetModelRuns(roundId));
    }

    /**
     * Public read-only endpoint: provider predictions persisted for the latest model run of the round.
     * Frontend can fetch once per round and switch matches locally.
     */
    @GetMapping("/{roundId}/provider-predictions")
    public ResponseEntity<RoundProviderPredictionsResponse> getLatestProviderPredictions(@PathVariable long roundId) {
        if (!roundRepository.existsById(roundId)) {
            return ResponseEntity.notFound().build();
        }

        List<ModelRunEntity> runs = modelRunRepository.findByRoundIdOrderByGeneratedAtDesc(roundId);
        if (runs.isEmpty()) {
            return ResponseEntity.ok(new RoundProviderPredictionsResponse(
                    roundId,
                    null,
                    null,
                    List.of()
            ));
        }

        ModelRunEntity latestRun = runs.get(0);

        List<ModelRunProviderPredictionEntity> rows =
                modelRunProviderPredictionRepository.findByModelRunIdOrderByMatchNumberAscProviderNameAsc(latestRun.getId());

        Map<Integer, List<ProviderPredictionView>> byMatch = new LinkedHashMap<>();
        for (ModelRunProviderPredictionEntity row : rows) {
            byMatch.computeIfAbsent(row.getMatchNumber(), __ -> new ArrayList<>())
                    .add(new ProviderPredictionView(
                            row.getProviderName(),
                            row.getStatus(),
                            row.getMessage(),
                            row.getProbabilityHome(),
                            row.getProbabilityDraw(),
                            row.getProbabilityAway()
                    ));
        }

        List<MatchProviderPredictionsView> matches = byMatch.entrySet().stream()
                .map(e -> new MatchProviderPredictionsView(e.getKey(), e.getValue()))
                .toList();

        return ResponseEntity.ok(new RoundProviderPredictionsResponse(
                roundId,
                latestRun.getId(),
                latestRun.getGeneratedAt(),
                matches
        ));
    }

    public record ProviderPredictionView(
            String providerName,
            String status,
            String message,
            Double probabilityHome,
            Double probabilityDraw,
            Double probabilityAway
    ) { }

    public record MatchProviderPredictionsView(
            int matchNumber,
            List<ProviderPredictionView> providers
    ) { }

    public record RoundProviderPredictionsResponse(
            long roundId,
            Long modelRunId,
            Instant generatedAt,
            List<MatchProviderPredictionsView> matches
    ) { }
}