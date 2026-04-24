package ar.ss.betting.services;

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
import ar.ss.betting.util.JsonUtil;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Service
public class RoundPersistenceService {

    private final RoundRepository gameRoundRepository;
    private final ModelRunRepository modelRunRepository;
    private final ModelRunProviderPredictionRepository modelRunProviderPredictionRepository;
    private final Clock clock;
    public RoundPersistenceService(RoundRepository gameRoundRepository,
                                   ModelRunRepository modelRunRepository,
                                   ModelRunProviderPredictionRepository modelRunProviderPredictionRepository,
                                   Clock clock) {
        this.gameRoundRepository = Objects.requireNonNull(gameRoundRepository);
        this.modelRunRepository = Objects.requireNonNull(modelRunRepository);
        this.modelRunProviderPredictionRepository = Objects.requireNonNull(modelRunProviderPredictionRepository);
        this.clock = Objects.requireNonNull(clock);
    }

    @Transactional
    public SavedRound saveRound(Round round) {
        Objects.requireNonNull(round, "round cannot be null");

        RoundEntity entity = new RoundEntity(
                round.getRoundType(),
                round.getStatus(),
                round.getStartTime()
        );

        for (Match m : round.getMatches()) {
            MatchEntity matchEntity = new MatchEntity(
                    m.getMatchNumber(),
                    m.getStartTime(),
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
        Objects.requireNonNull(predictionResults, "predictionResults cannot be null");

        if (trigger.isBlank()) {
            throw new IllegalArgumentException("trigger cannot be blank");
        }
        if (budgetInSek <= 0) {
            throw new IllegalArgumentException("budgetInSek must be positive");
        }

        RoundEntity roundEntity = gameRoundRepository.findById(roundId)
                .orElseThrow(() -> new IllegalArgumentException("Round not found: " + roundId));

        String selectionsJson = JsonUtil.toJson(toSelectionsDtoShape(result));
        String basePicksJson = JsonUtil.toJson(toBasePicksDtoShape(result));
        String internalProbabilitiesJson = JsonUtil.toJson(toInternalProbabilitiesDtoShape(result));

        ModelRunEntity runEntity = new ModelRunEntity(
                roundEntity,
                result.modelName(),
                result.generatedAt(),
                budgetInSek,
                result.totalCostInSek(),
                result.halfGuardsCount(),
                selectionsJson,
                basePicksJson,
                internalProbabilitiesJson,
                trigger
        );

        ModelRunEntity saved = modelRunRepository.save(runEntity);

        persistModelRunPredictions(saved, predictionResults);

        return new SavedModelRun(saved.getId(), saved.getGeneratedAt());
    }

    private Map<String, List<String>> toSelectionsDtoShape(ModelSelectionResult result) {
        return result.selections().entrySet().stream()
                .collect(Collectors.toMap(
                        e -> String.valueOf(e.getKey()),
                        e -> e.getValue().stream().map(Enum::name).toList(),
                        (a, b) -> a,
                        TreeMap::new
                ));
    }

    private Map<String, String> toBasePicksDtoShape(ModelSelectionResult result) {
        return result.basePicks().entrySet().stream()
                .collect(Collectors.toMap(
                        e -> String.valueOf(e.getKey()),
                        e -> e.getValue().name(),
                        (a, b) -> a,
                        TreeMap::new
                ));
    }

    private void persistModelRunPredictions(ModelRunEntity modelRun,
                                            List<MatchPredictionResult> predictionResults) {
        if (predictionResults == null || predictionResults.isEmpty()) {
            return;
        }

        List<ModelRunProviderPredictionEntity> rows = new ArrayList<>();

        for (MatchPredictionResult matchResult : predictionResults) {
            int matchNumber = parseRequiredMatchNumber(matchResult.getClientMatchId());

            if (matchResult.getProviders() == null || matchResult.getProviders().isEmpty()) {
                continue;
            }

            for (ProviderPredictionResult providerResult : matchResult.getProviders()) {
                rows.add(toProviderPredictionEntity(modelRun, matchNumber, matchResult, providerResult));
            }
        }

        if (!rows.isEmpty()) {
            modelRunProviderPredictionRepository.saveAll(rows);
        }
    }

    private ModelRunProviderPredictionEntity toProviderPredictionEntity(ModelRunEntity modelRun,
                                                                        int matchNumber,
                                                                        MatchPredictionResult matchResult,
                                                                        ProviderPredictionResult providerResult) {
        MatchPrediction prediction = providerResult.getPrediction();
        ProviderProbabilityTriple probabilities = prediction != null ? prediction.getProbabilities() : null;

        return new ModelRunProviderPredictionEntity(
                modelRun,
                matchNumber,
                providerResult.getProvider(),
                providerResult.getStatus().name(),
                providerResult.getMessage(),
                matchResult.getRequestedHomeTeam(),
                matchResult.getRequestedAwayTeam(),
                prediction != null ? prediction.getHomeTeam() : null,
                prediction != null ? prediction.getAwayTeam() : null,
                prediction != null ? prediction.getKickoff() : null,
                prediction != null ? prediction.getKickoffRaw() : null,
                probabilities != null ? probabilities.getHomeWin() : null,
                probabilities != null ? probabilities.getDraw() : null,
                probabilities != null ? probabilities.getAwayWin() : null,
                prediction != null ? prediction.getFetchedAt() : null,
                Instant.now(clock)
        );
    }

    private int parseRequiredMatchNumber(String clientMatchId) {
        if (clientMatchId == null || clientMatchId.isBlank()) {
            throw new IllegalArgumentException("Prediction response missing clientMatchId");
        }

        int colon = clientMatchId.indexOf(':');
        String matchNumberPart = colon >= 0 ? clientMatchId.substring(colon + 1) : clientMatchId;

        try {
            int parsed = Integer.parseInt(matchNumberPart);
            if (parsed <= 0) {
                throw new IllegalArgumentException("Prediction response clientMatchId must be positive: " + clientMatchId);
            }
            return parsed;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Prediction response clientMatchId is not numeric: " + clientMatchId, e);
        }
    }

    private Map<String, ProbabilityTripleDtoShape> toInternalProbabilitiesDtoShape(ModelSelectionResult result) {
        Map<String, ProbabilityTripleDtoShape> out = new TreeMap<>();

        for (Map.Entry<Integer, ProbabilityTriple> e : result.internalProbabilities().entrySet()) {
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

    public record SavedModelRun(long id, Instant generatedAt) { }
}