package ar.ss.betting.rework;

import ar.ss.betting.domain.Match;
import ar.ss.betting.domain.Outcome;
import ar.ss.betting.domain.Round;
import ar.ss.betting.model.ModelSelectionResult;
import ar.ss.betting.model.ProbabilityTriple;
import ar.ss.betting.persistence.entity.RoundEntity;
import ar.ss.betting.persistence.entity.MatchEntity;
import ar.ss.betting.persistence.entity.ModelRunEntity;
import ar.ss.betting.persistence.repo.ModelRunRepository;
import ar.ss.betting.persistence.repo.RoundRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class RoundPersistenceService {

    private static final int DEFAULT_ROUND_DURATION_HOURS = 2;

    private final RoundRepository gameRoundRepository;
    private final ModelRunRepository modelRunRepository;

    public RoundPersistenceService(RoundRepository gameRoundRepository,
                                   ModelRunRepository modelRunRepository) {
        this.gameRoundRepository = Objects.requireNonNull(gameRoundRepository);
        this.modelRunRepository = Objects.requireNonNull(modelRunRepository);
    }

    @Transactional
    public SavedRound saveRound(Round round) {
        Objects.requireNonNull(round, "round cannot be null");

        RoundEntity entity = new RoundEntity(
                round.getRoundType(),
                round.getStatus(),
                round.getStartDate()
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

        RoundEntity saved = gameRoundRepository.save(entity);
        return new SavedRound(saved.getId());
    }

    @Transactional
    public SavedModelRun saveModelRun(long roundId,
                                      int budgetInSek,
                                      String trigger,
                                      ModelSelectionResult result) {

        Objects.requireNonNull(trigger, "trigger cannot be null");
        Objects.requireNonNull(result, "result cannot be null");

        if (trigger.isBlank()) {
            throw new IllegalArgumentException("trigger cannot be blank");
        }
        if (budgetInSek <= 0) {
            throw new IllegalArgumentException("budgetInSek must be positive");
        }

        RoundEntity roundEntity = gameRoundRepository.findById(roundId)
                .orElseThrow(() -> new IllegalArgumentException("Round not found: " + roundId));

        String selectionsJson = JsonUtil.toJson(toSelectionsDtoShape(result));
        String internalProbabilitiesJson = JsonUtil.toJson(toInternalProbabilitiesDtoShape(result));

        ModelRunEntity runEntity = new ModelRunEntity(
                roundEntity,
                result.getModelName(),
                result.getGeneratedAt(),
                budgetInSek,
                result.getTotalCostInSek(),
                result.getHalfGuardsCount(),
                selectionsJson,
                internalProbabilitiesJson,
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

    private Map<String, ProbabilityTripleDtoShape> toInternalProbabilitiesDtoShape(ModelSelectionResult result) {
        Map<String, ProbabilityTripleDtoShape> out = new TreeMap<>();

        for (Map.Entry<Integer, ProbabilityTriple> e : result.getInternalProbabilities().entrySet()) {
            ProbabilityTriple triple = e.getValue();
            out.put(
                    String.valueOf(e.getKey()),
                    new ProbabilityTripleDtoShape(
                            triple.get(Outcome.HOME_WIN),
                            triple.get(Outcome.DRAW),
                            triple.get(Outcome.AWAY_WIN)
                    )
            );
        }

        return out;
    }

    public record ProbabilityTripleDtoShape(
            double homeWin,
            double draw,
            double awayWin
    ) { }

    public record SavedRound(long id) { }

    public record SavedModelRun(long id, LocalDateTime generatedAt) { }
}