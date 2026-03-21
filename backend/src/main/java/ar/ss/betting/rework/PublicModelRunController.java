package ar.ss.betting.rework;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/public/rounds")
public class PublicModelRunController {

    private final RoundApiService roundApiService;

    public PublicModelRunController(RoundApiService roundApiService) {
        this.roundApiService = Objects.requireNonNull(roundApiService);
    }

    /**
     * Public read-only endpoint: latest model run per preset budget (32/64/128/256) for a round.
     */
    @GetMapping("/{roundId}/model-runs")
    public ResponseEntity<List<RoundApiService.ModelRunView>> getLatestPresetRuns(@PathVariable long roundId) {
        return ResponseEntity.ok(roundApiService.getLatestPresetModelRuns(roundId));
    }
}