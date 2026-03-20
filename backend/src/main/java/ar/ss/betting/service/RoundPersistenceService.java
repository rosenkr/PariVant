package ar.ss.betting.service;

import ar.ss.betting.domain.GameRound;
import ar.ss.betting.domain.Match;
import ar.ss.betting.model.ModelSelectionResult;
import ar.ss.betting.persistence.entity.GameRoundEntity;
import ar.ss.betting.persistence.entity.MatchEntity;
import ar.ss.betting.persistence.entity.ModelRunEntity;
import ar.ss.betting.persistence.repo.GameRoundRepository;
import ar.ss.betting.persistence.repo.ModelRunRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class RoundPersistenceService {

    private static final int DEFAULT_ROUND_DURATION_HOURS = 2;

    private final GameRoundRepository gameRoundRepository;
    private final ModelRunRepository modelRunRepository;

    public RoundPersistenceService(GameRoundRepository gameRoundRepository,
                                   ModelRunRepository modelRunRepository) {
        this.gameRoundRepository = Objects.requireNonNull(gameRoundRepository);
        this.modelRunRepository = Objects.requireNonNull(modelRunRepository);
    }

    @Transactional
    public SavedRound saveRound(GameRound round) {
        Objects.requireNonNull(round, "round cannot be null");

        LocalDateTime endDate = round.getStartDate().plusHours(DEFAULT_ROUND_DURATION_HOURS);

        GameRoundEntity entity = new GameRoundEntity(
                round.getGameType(),
                round.getStartDate(),
                endDate
        );

        for (Match m : round.getMatches()) {
            MatchEntity matchEntity = new MatchEntity(
                    m.getMatchNumber(),
                    m.getStartDate(),
                    m.getHomeTeam().getName(),
                    m.getAwayTeam().getName()
            );
            entity.addMatch(matchEntity);
        }

        GameRoundEntity saved = gameRoundRepository.save(entity);
        return new SavedRound(saved.getId());
    }

    @Transactional
    public SavedModelRun saveModelRun(long gameRoundId,
                                      int budgetInSek,
                                      String trigger,
                                      ModelSelectionResult result,
                                      AdjustmentWeights weights,
                                      DecisionParameters decisionParameters) {

        Objects.requireNonNull(trigger, "trigger cannot be null");
        Objects.requireNonNull(result, "result cannot be null");
        Objects.requireNonNull(weights, "weights cannot be null");
        Objects.requireNonNull(decisionParameters, "decisionParameters cannot be null");

        if (trigger.isBlank()) {
            throw new IllegalArgumentException("trigger cannot be blank");
        }
        if (budgetInSek <= 0) {
            throw new IllegalArgumentException("budgetInSek must be positive");
        }

        GameRoundEntity roundEntity = gameRoundRepository.findById(gameRoundId)
                .orElseThrow(() -> new IllegalArgumentException("GameRound not found: " + gameRoundId));

        String selectionsJson = JsonUtil.toJson(toSelectionsDtoShape(result));
        String weightsJson = JsonUtil.toJson(toWeightsDtoShape(weights));
        String paramsJson = JsonUtil.toJson(toDecisionParamsDtoShape(decisionParameters));

        ModelRunEntity runEntity = new ModelRunEntity(
                roundEntity,
                result.getModelName(),
                result.getGeneratedAt(),
                budgetInSek,
                result.getTotalCostInSek(),
                result.getHalfGuardsCount(),
                result.getFullGuardsCount(),
                selectionsJson,
                weightsJson,
                paramsJson,
                trigger
        );

        ModelRunEntity saved = modelRunRepository.save(runEntity);
        return new SavedModelRun(saved.getId(), saved.getGeneratedAt());
    }

    private Map<String, List<String>> toSelectionsDtoShape(ModelSelectionResult result) {
        return result.getSelections().entrySet().stream()
                .collect(Collectors.toMap(
                        e -> String.valueOf(e.getKey()),
                        e -> e.getValue().stream().map(Enum::name).toList(),
                        (a, b) -> a,
                        TreeMap::new
                ));
    }

    private Map<String, Object> toWeightsDtoShape(AdjustmentWeights weights) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("recentFormWeight", weights.getRecentFormWeight());
        return map;
    }

    private Map<String, Object> toDecisionParamsDtoShape(DecisionParameters p) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("probabilityFloorTopptipset", p.getProbabilityFloorTopptipset());
        map.put("probabilityFloorStryktipset", p.getProbabilityFloorStryktipset());
        map.put("valueThresholdTopptipset", p.getValueThresholdTopptipset());
        map.put("valueThresholdStryktipset", p.getValueThresholdStryktipset());
        map.put("maxFullGuardsTopptipset", p.getMaxFullGuardsTopptipset());
        map.put("maxFullGuardsStryktipset", p.getMaxFullGuardsStryktipset());
        return map;
    }

    public record SavedRound(long id) { }

    public record SavedModelRun(long id, LocalDateTime generatedAt) { }
}