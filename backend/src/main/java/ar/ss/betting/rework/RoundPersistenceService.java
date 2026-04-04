package ar.ss.betting.rework;

import ar.ss.betting.domain.Match;
import ar.ss.betting.domain.Outcome;
import ar.ss.betting.domain.Round;
import ar.ss.betting.model.ModelSelectionResult;
import ar.ss.betting.model.ProbabilityTriple;
import ar.ss.betting.persistence.entity.MatchEntity;
import ar.ss.betting.persistence.entity.ModelRunEntity;
import ar.ss.betting.persistence.entity.ModelRunProviderPredictionEntity;
import ar.ss.betting.persistence.entity.RoundEntity;
import ar.ss.betting.persistence.repo.ModelRunProviderPredictionRepository;
import ar.ss.betting.persistence.repo.ModelRunRepository;
import ar.ss.betting.persistence.repo.RoundRepository;
import ar.ss.betting.predictionproviders.domain.MatchPrediction;
import ar.ss.betting.predictionproviders.domain.ProviderProbabilityTriple;
import ar.ss.betting.predictionproviders.service.model.MatchPredictionResult;
import ar.ss.betting.predictionproviders.service.model.ProviderPredictionResult;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class RoundPersistenceService {

    private final RoundRepository gameRoundRepository;
    private final ModelRunRepository modelRunRepository;
    private final ModelRunProviderPredictionRepository modelRunProviderPredictionRepository;

    public RoundPersistenceService(RoundRepository gameRoundRepository,
                                   ModelRunRepository modelRunRepository,
                                   ModelRunProviderPredictionRepository modelRunProviderPredictionRepository) {
        this.gameRoundRepository = Objects.requireNonNull(gameRoundRepository);
        this.modelRunRepository = Objects.requireNonNull(modelRunRepository);
        this.modelRunProviderPredictionRepository = Objects.requireNonNull(modelRunProviderPredictionRepository);
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
                                      ModelSelectionResult result,
                                      List<MatchPredictionResult> predictionResults) {

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
        saveProviderPredictions(saved, predictionResults);

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

    private void saveProviderPredictions(ModelRunEntity modelRunEntity,
                                         List<MatchPredictionResult> predictionResults) {
        if (predictionResults == null || predictionResults.isEmpty()) {
            return;
        }

        List<ModelRunProviderPredictionEntity> rows = new ArrayList<>();
        LocalDateTime createdAt = LocalDateTime.now();

        for (MatchPredictionResult matchResult : predictionResults) {
            int matchNumber = parseRequiredMatchNumber(matchResult.getClientMatchId());

            if (matchResult.getProviders() == null) {
                continue;
            }

            for (ProviderPredictionResult providerResult : matchResult.getProviders()) {
                rows.add(toProviderPredictionEntity(
                        modelRunEntity,
                        matchNumber,
                        matchResult,
                        providerResult,
                        createdAt
                ));
            }
        }

        if (!rows.isEmpty()) {
            modelRunProviderPredictionRepository.saveAll(rows);
        }
    }

    private ModelRunProviderPredictionEntity toProviderPredictionEntity(ModelRunEntity modelRunEntity,
                                                                        int matchNumber,
                                                                        MatchPredictionResult matchResult,
                                                                        ProviderPredictionResult providerResult,
                                                                        LocalDateTime createdAt) {
        MatchPrediction prediction = providerResult.getPrediction();

        String resolvedHome = null;
        String resolvedAway = null;
        LocalDateTime kickoff = null;
        String kickoffRaw = null;
        Double probabilityHome = null;
        Double probabilityDraw = null;
        Double probabilityAway = null;
        LocalDateTime fetchedAt = null;

        if (prediction != null) {
            resolvedHome = prediction.getHomeTeam();
            resolvedAway = prediction.getAwayTeam();
            kickoff = toUtcLocalDateTime(prediction.getKickoff());
            kickoffRaw = prediction.getKickoffRaw();

            ProviderProbabilityTriple probabilities = prediction.getProbabilities();
            if (probabilities != null) {
                probabilityHome = probabilities.getHomeWin();
                probabilityDraw = probabilities.getDraw();
                probabilityAway = probabilities.getAwayWin();
            }

            fetchedAt = toUtcLocalDateTime(prediction.getFetchedAt());
        }

        return new ModelRunProviderPredictionEntity(
                modelRunEntity,
                matchNumber,
                providerResult.getProvider(),
                providerResult.getStatus().name(),
                providerResult.getMessage(),
                matchResult.getRequestedHomeTeam(),
                matchResult.getRequestedAwayTeam(),
                resolvedHome,
                resolvedAway,
                kickoff,
                kickoffRaw,
                probabilityHome,
                probabilityDraw,
                probabilityAway,
                fetchedAt,
                createdAt
        );
    }

    private int parseRequiredMatchNumber(String clientMatchId) {
        if (clientMatchId == null || clientMatchId.isBlank()) {
            throw new IllegalArgumentException("clientMatchId is missing in prediction results");
        }

        String matchNumberPart = clientMatchId;
        int colon = clientMatchId.indexOf(':');
        if (colon >= 0) {
            matchNumberPart = clientMatchId.substring(colon + 1);
        }

        try {
            int parsed = Integer.parseInt(matchNumberPart);
            if (parsed <= 0) {
                throw new IllegalArgumentException("matchNumber must be positive: " + clientMatchId);
            }
            return parsed;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("clientMatchId is not a valid match number: " + clientMatchId, e);
        }
    }

    private LocalDateTime toUtcLocalDateTime(OffsetDateTime value) {
        if (value == null) {
            return null;
        }
        return value.atZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
    }

    private LocalDateTime toUtcLocalDateTime(Instant value) {
        if (value == null) {
            return null;
        }
        return value.atOffset(ZoneOffset.UTC).toLocalDateTime();
    }

    public record ProbabilityTripleDtoShape(
            double homeWin,
            double draw,
            double awayWin
    ) { }

    public record SavedRound(long id) { }

    public record SavedModelRun(long id, LocalDateTime generatedAt) { }
}