package ar.ss.betting.api;

import ar.ss.betting.api.dto.ModelSelectionRequestDto;
import ar.ss.betting.domain.GameType;
import ar.ss.betting.service.RoundApiService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/rounds")
public class RoundController {

    private final RoundApiService roundApiService;

    public RoundController(RoundApiService roundApiService) {
        this.roundApiService = Objects.requireNonNull(roundApiService);
    }

    // --- C4 write endpoints ---

    @PostMapping
    public ResponseEntity<CreateRoundResponse> createRound(@RequestBody CreateRoundRequest request) {
        long id = roundApiService.createRound(
                GameType.valueOf(request.gameType()),
                LocalDateTime.parse(request.roundStartDate()),
                request.matches()
        );
        return ResponseEntity.ok(new CreateRoundResponse(id));
    }

    @PostMapping("/{roundId}/model-runs")
    public ResponseEntity<CreateModelRunResponse> createModelRun(@PathVariable long roundId,
                                                                 @RequestBody CreateModelRunRequest request) {

        long modelRunId = roundApiService.runModelAndPersist(
                roundId,
                request.budgetInSek(),
                request.contexts(),
                request.weights(),
                request.decisionParameters()
        );

        return ResponseEntity.ok(new CreateModelRunResponse(modelRunId));
    }

    // --- C5 read endpoints ---

    @GetMapping("/{roundId}")
    public ResponseEntity<RoundApiService.RoundView> getRound(@PathVariable long roundId) {
        return ResponseEntity.ok(roundApiService.getRound(roundId));
    }

    @GetMapping("/{roundId}/model-runs")
    public ResponseEntity<List<RoundApiService.ModelRunView>> getModelRuns(@PathVariable long roundId) {
        return ResponseEntity.ok(roundApiService.getModelRuns(roundId));
    }

    // --- Request/Response records (keeps file count low) ---

    public record CreateRoundRequest(
            String gameType,
            String roundStartDate,
            List<ModelSelectionRequestDto.MatchDto> matches
    ) { }

    public record CreateRoundResponse(long roundId) { }

    public record CreateModelRunRequest(
            int budgetInSek,
            Map<Integer, ModelSelectionRequestDto.MatchContextDto> contexts,
            ModelSelectionRequestDto.WeightsDto weights,
            ModelSelectionRequestDto.DecisionParametersDto decisionParameters
    ) { }

    public record CreateModelRunResponse(long modelRunId) { }
}