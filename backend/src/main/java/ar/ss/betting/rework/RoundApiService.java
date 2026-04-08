package ar.ss.betting.rework;

import ar.ss.betting.domain.Match;
import ar.ss.betting.domain.Round;
import ar.ss.betting.domain.RoundType;
import ar.ss.betting.domain.Team;
import ar.ss.betting.model.*;
import ar.ss.betting.persistence.entity.MatchEntity;
import ar.ss.betting.persistence.entity.ModelRunEntity;
import ar.ss.betting.persistence.entity.RoundEntity;
import ar.ss.betting.persistence.repo.MatchRepository;
import ar.ss.betting.persistence.repo.ModelRunRepository;
import ar.ss.betting.persistence.repo.RoundRepository;
import ar.ss.betting.predictionproviders.domain.MatchPrediction;
import ar.ss.betting.predictionproviders.domain.ProviderProbabilityTriple;
import ar.ss.betting.predictionproviders.service.PredictionQueryService;
import ar.ss.betting.predictionproviders.service.model.MatchPredictionResult;
import ar.ss.betting.predictionproviders.service.model.PredictionMatchRequest;
import ar.ss.betting.predictionproviders.service.model.PredictionQueryResponse;
import ar.ss.betting.predictionproviders.service.model.ProviderPredictionResult;
import ar.ss.betting.predictionproviders.service.model.ProviderPredictionStatus;
import ar.ss.betting.predictionproviders.service.model.ProviderRawPredictionSnapshot;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class RoundApiService {

    private static final String TRIGGER_MANUAL = "MANUAL";
    private static final List<Integer> PRESET_BUDGETS = List.of(32, 64, 128, 256);

    private final RoundPersistenceService roundPersistenceService;
    private final RoundRepository gameRoundRepository;
    private final MatchRepository matchRepository;
    private final ModelRunRepository modelRunRepository;
    private final PredictionQueryService predictionQueryService;
    private final PredictionResolutionService predictionResolutionService;
    private final Clock clock;

    public RoundApiService(RoundPersistenceService roundPersistenceService,
                           RoundRepository gameRoundRepository,
                           MatchRepository matchRepository,
                           ModelRunRepository modelRunRepository,
                           PredictionQueryService predictionQueryService,
                           PredictionResolutionService predictionResolutionService,
                           Clock clock) {
        this.roundPersistenceService = Objects.requireNonNull(roundPersistenceService);
        this.gameRoundRepository = Objects.requireNonNull(gameRoundRepository);
        this.matchRepository = Objects.requireNonNull(matchRepository);
        this.modelRunRepository = Objects.requireNonNull(modelRunRepository);
        this.predictionQueryService = Objects.requireNonNull(predictionQueryService);
        this.predictionResolutionService = Objects.requireNonNull(predictionResolutionService);
        this.clock = Objects.requireNonNull(clock);
    }

    public long createRound(RoundType roundType,
                            Instant roundStartTime,
                            List<ModelSelectionRequestDto.MatchDto> matches) {

        Objects.requireNonNull(roundType, "roundType");
        Objects.requireNonNull(roundStartTime, "roundStartTime");
        Objects.requireNonNull(matches, "matches");

        List<Match> domainMatches = new ArrayList<>(matches.size());
        for (ModelSelectionRequestDto.MatchDto m : matches) {
            domainMatches.add(new Match(
                    m.matchNumber(),
                    Instant.parse(m.startDate()),
                    new Team(m.homeTeamName()),
                    new Team(m.awayTeamName())
            ));
        }

        Round round = new Round(roundStartTime, roundType, domainMatches);
        return roundPersistenceService.saveRound(round).id();
    }

    public List<CreatedModelRun> runPresetModelRuns(long roundId,
                                                    Map<Integer, ModelSelectionRequestDto.MatchContextDto> contexts,
                                                    Map<Integer, ModelSelectionRequestDto.MatchInterventionsDto> interventions) {
        return runPresetModelRunsWithTrigger(roundId, contexts, interventions, TRIGGER_MANUAL);
    }

    public List<CreatedModelRun> runPresetModelRunsWithTrigger(long roundId,
                                                               Map<Integer, ModelSelectionRequestDto.MatchContextDto> contexts,
                                                               Map<Integer, ModelSelectionRequestDto.MatchInterventionsDto> interventions,
                                                               String trigger) {

        Objects.requireNonNull(contexts, "contexts");
        Objects.requireNonNull(trigger, "trigger");

        Round round = loadRound(roundId);

        List<ProviderRawPredictionSnapshot> providerSnapshots =
                predictionQueryService.fetchProviderSnapshots();

        PredictionQueryResponse predictionQueryResponse = predictionResolutionService.resolve(
                toRequests(round),
                providerSnapshots
        );

        Map<Integer, List<ModelSelectionRequestDto.ProbabilityTripleDto>> fetchedProvidersByMatch =
                toFetchedProviderDtosByMatch(predictionQueryResponse);

        List<CreatedModelRun> createdRuns = new ArrayList<>();

        for (int presetBudget : PRESET_BUDGETS) {
            ModelInput modelInput = new ModelInput(
                    toMatchContexts(contexts, fetchedProvidersByMatch),
                    toMatchInterventions(interventions)
            );

            EnsembleModel model = new EnsembleModel();
            Instant generatedAt = Instant.now(clock);
            ModelSelectionResult result = model.generateSelection(round, modelInput, presetBudget, generatedAt);

            RoundPersistenceService.SavedModelRun saved = roundPersistenceService.saveModelRun(
                    roundId,
                    presetBudget,
                    trigger,
                    result,
                    predictionQueryResponse.getResults()
            );

            createdRuns.add(new CreatedModelRun(
                    saved.id(),
                    presetBudget,
                    saved.generatedAt()
            ));
        }

        return createdRuns;
    }

    public List<ModelRunView> getLatestPresetModelRuns(long roundId) {
        List<ModelRunEntity> runs = modelRunRepository.findByRoundIdOrderByGeneratedAtDesc(roundId);

        Map<Integer, ModelRunEntity> latestByBudget = new HashMap<>();
        for (Integer b : PRESET_BUDGETS) {
            for (ModelRunEntity r : runs) {
                if (r.getBudgetInSek() == b) {
                    latestByBudget.put(b, r);
                    break;
                }
            }
        }

        List<ModelRunView> out = new ArrayList<>();
        for (Integer b : PRESET_BUDGETS) {
            ModelRunEntity r = latestByBudget.get(b);
            if (r != null) {
                out.add(toView(r));
            }
        }
        return out;
    }

    private Round loadRound(long roundId) {
        RoundEntity roundEntity = gameRoundRepository.findById(roundId)
                .orElseThrow(() -> new IllegalArgumentException("Round not found: " + roundId));

        List<MatchEntity> matchEntities = matchRepository.findByRoundIdOrderByMatchNumberAsc(roundId);

        List<Match> matches = new ArrayList<>(matchEntities.size());
        for (MatchEntity m : matchEntities) {
            matches.add(new Match(
                    m.getMatchNumber(),
                    m.getStartTime(),
                    new Team(m.getHomeTeamName()),
                    new Team(m.getAwayTeamName())
            ));
        }

        return new Round(
                roundEntity.getStartTime(),
                roundEntity.getRoundType(),
                matches
        );
    }

    private List<PredictionMatchRequest> toRequests(Round round) {
        List<PredictionMatchRequest> requests = new ArrayList<>();

        for (Match match : round.getMatches()) {
            requests.add(new PredictionMatchRequest(
                    String.valueOf(match.getMatchNumber()),
                    match.getHomeTeam().getName(),
                    match.getAwayTeam().getName(),
                    match.getStartTime()
            ));
        }

        return requests;
    }

    private Map<Integer, List<ModelSelectionRequestDto.ProbabilityTripleDto>> toFetchedProviderDtosByMatch(
            PredictionQueryResponse predictionQueryResponse) {

        Map<Integer, List<ModelSelectionRequestDto.ProbabilityTripleDto>> out = new HashMap<>();

        if (predictionQueryResponse == null || predictionQueryResponse.getResults() == null) {
            return out;
        }

        for (MatchPredictionResult matchResult : predictionQueryResponse.getResults()) {
            int matchNumber = parseRequiredMatchNumber(matchResult.getClientMatchId());

            List<ModelSelectionRequestDto.ProbabilityTripleDto> providerDtos = new ArrayList<>();

            if (matchResult.getProviders() != null) {
                for (ProviderPredictionResult providerResult : matchResult.getProviders()) {
                    if (providerResult.getStatus() != ProviderPredictionStatus.OK) {
                        continue;
                    }

                    MatchPrediction prediction = providerResult.getPrediction();
                    if (prediction == null || prediction.getProbabilities() == null) {
                        continue;
                    }

                    ProviderProbabilityTriple probabilities = prediction.getProbabilities();

                    providerDtos.add(new ModelSelectionRequestDto.ProbabilityTripleDto(
                            probabilities.getHomeWin(),
                            probabilities.getDraw(),
                            probabilities.getAwayWin()
                    ));
                }
            }

            out.put(matchNumber, providerDtos);
        }

        return out;
    }

    private int parseRequiredMatchNumber(String clientMatchId) {
        if (clientMatchId == null || clientMatchId.isBlank()) {
            throw new IllegalArgumentException("Prediction response missing clientMatchId");
        }

        try {
            int parsed = Integer.parseInt(clientMatchId);
            if (parsed <= 0) {
                throw new IllegalArgumentException("Prediction response clientMatchId must be positive: " + clientMatchId);
            }
            return parsed;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Prediction response clientMatchId is not numeric: " + clientMatchId, e);
        }
    }

    private Map<Integer, MatchContext> toMatchContexts(
            Map<Integer, ModelSelectionRequestDto.MatchContextDto> contexts,
            Map<Integer, List<ModelSelectionRequestDto.ProbabilityTripleDto>> fetchedProvidersByMatch) {

        Map<Integer, MatchContext> out = new HashMap<>();

        for (Map.Entry<Integer, ModelSelectionRequestDto.MatchContextDto> e : contexts.entrySet()) {
            Integer matchNumber = e.getKey();
            ModelSelectionRequestDto.MatchContextDto dto = e.getValue();

            ProbabilityTriple market = ProbabilityTriple.fromProbabilities(
                    dto.market().homeWin(),
                    dto.market().draw(),
                    dto.market().awayWin()
            );

            ProbabilityTriple publicPick = ProbabilityTriple.fromProbabilities(
                    dto.publicPick().homeWin(),
                    dto.publicPick().draw(),
                    dto.publicPick().awayWin()
            );

            List<ProbabilityTriple> providers = new ArrayList<>();

            if (dto.providers() != null) {
                for (ModelSelectionRequestDto.ProbabilityTripleDto p : dto.providers()) {
                    providers.add(ProbabilityTriple.fromProbabilities(
                            p.homeWin(), p.draw(), p.awayWin()
                    ));
                }
            }

            List<ModelSelectionRequestDto.ProbabilityTripleDto> fetchedProviders =
                    fetchedProvidersByMatch.getOrDefault(matchNumber, List.of());

            for (ModelSelectionRequestDto.ProbabilityTripleDto p : fetchedProviders) {
                providers.add(ProbabilityTriple.fromProbabilities(
                        p.homeWin(), p.draw(), p.awayWin()
                ));
            }

            out.put(matchNumber, new MatchContext(market, publicPick, providers));
        }

        return out;
    }

    private Map<Integer, MatchInterventions> toMatchInterventions(
            Map<Integer, ModelSelectionRequestDto.MatchInterventionsDto> interventions) {

        Map<Integer, MatchInterventions> out = new HashMap<>();
        if (interventions == null) {
            return out;
        }

        for (Map.Entry<Integer, ModelSelectionRequestDto.MatchInterventionsDto> e : interventions.entrySet()) {
            Integer matchNumber = e.getKey();
            ModelSelectionRequestDto.MatchInterventionsDto dto = e.getValue();

            List<MatchTag> tags = new ArrayList<>();
            if (dto != null && dto.tags() != null) {
                for (ModelSelectionRequestDto.TagDto tagDto : dto.tags()) {
                    tags.add(toTag(tagDto));
                }
            }

            MatchBuff buff = null;
            if (dto != null && dto.buff() != null) {
                buff = new MatchBuff(
                        ar.ss.betting.domain.Outcome.valueOf(dto.buff().targetOutcome()),
                        dto.buff().points()
                );
            }

            out.put(matchNumber, new MatchInterventions(tags, buff));
        }

        return out;
    }

    private MatchTag toTag(ModelSelectionRequestDto.TagDto tagDto) {
        TagType type = TagType.valueOf(tagDto.type());

        return switch (type) {
            case NEUTRAL_VENUE -> MatchTag.neutralVenue();
            case KEY_ABSENCE -> MatchTag.keyAbsence(Side.valueOf(tagDto.affectedSide()));
            case SHORT_REST -> MatchTag.shortRest(Side.valueOf(tagDto.affectedSide()));
            case INCENTIVE_LACK -> MatchTag.incentiveLack(Side.valueOf(tagDto.affectedSide()));
        };
    }

    private ModelRunView toView(ModelRunEntity r) {
        return new ModelRunView(
                r.getId(),
                r.getModelName(),
                r.getGeneratedAt(),
                r.getBudgetInSek(),
                r.getTotalCostInSek(),
                r.getHalfGuardsCount(),
                r.getTrigger(),
                r.getSelectionsJson(),
                r.getBasePicksJson(),
                r.getInternalProbabilitiesJson()
        );
    }

    public record CreatedModelRun(
            long id,
            int budgetInSek,
            Instant generatedAt
    ) { }

    public record ModelRunView(
            long id,
            String modelName,
            Instant generatedAt,
            int budgetInSek,
            int totalCostInSek,
            int halfGuardsCount,
            String trigger,
            String selectionsJson,
            String basePicksJson,
            String internalProbabilitiesJson
    ) { }
}