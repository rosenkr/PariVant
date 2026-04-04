package ar.ss.betting.rework;

import ar.ss.betting.domain.RoundType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/internal/rounds")
public class RoundController {

    private final RoundApiService roundApiService;

    public RoundController(RoundApiService roundApiService) {
        this.roundApiService = Objects.requireNonNull(roundApiService);
    }

    @PostMapping
    public ResponseEntity<CreateRoundResponse> createRound(@RequestBody CreateRoundRequest request) {
        long id = roundApiService.createRound(
                RoundType.valueOf(request.roundType()),
                LocalDateTime.parse(request.roundStartDate()),
                request.matches()
        );
        return ResponseEntity.ok(new CreateRoundResponse(id));
    }

    @PostMapping("/{roundId}/model-runs")
    public ResponseEntity<CreatePresetModelRunsResponse> createPresetModelRuns(
            @PathVariable long roundId,
            @RequestBody CreatePresetModelRunsRequest request
    ) {
        List<RoundApiService.CreatedModelRun> createdRuns = roundApiService.runPresetModelRuns(
                roundId,
                request.contexts(),
                request.interventions()
        );

        return ResponseEntity.ok(new CreatePresetModelRunsResponse(createdRuns));
    }

    @GetMapping("/{roundId}/model-runs/presets/latest")
    public ResponseEntity<List<RoundApiService.ModelRunView>> latestPresetRuns(@PathVariable long roundId) {
        return ResponseEntity.ok(roundApiService.getLatestPresetModelRuns(roundId));
    }

    public record CreateRoundRequest(
            String roundType,
            String roundStartDate,
            List<ModelSelectionRequestDto.MatchDto> matches
    ) { }

    public record CreateRoundResponse(long roundId) { }

    public record CreatePresetModelRunsRequest(
            Map<Integer, ModelSelectionRequestDto.MatchContextDto> contexts,
            Map<Integer, ModelSelectionRequestDto.MatchInterventionsDto> interventions
    ) { }

    public record CreatePresetModelRunsResponse(
            List<RoundApiService.CreatedModelRun> createdRuns
    ) { }
}