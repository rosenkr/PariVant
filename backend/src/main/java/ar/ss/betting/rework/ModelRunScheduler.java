package ar.ss.betting.rework;

import ar.ss.betting.domain.Match;
import ar.ss.betting.domain.Round;
import ar.ss.betting.domain.RoundStatus;
import ar.ss.betting.domain.Team;
import ar.ss.betting.model.EnsembleModel;
import ar.ss.betting.model.MatchContext;
import ar.ss.betting.model.ModelInput;
import ar.ss.betting.model.ModelSelectionResult;
import ar.ss.betting.model.ProbabilityTriple;
import ar.ss.betting.persistence.entity.MatchContextEntity;
import ar.ss.betting.persistence.entity.MatchEntity;
import ar.ss.betting.persistence.entity.RoundEntity;
import ar.ss.betting.persistence.repo.MatchContextRepository;
import ar.ss.betting.persistence.repo.MatchRepository;
import ar.ss.betting.persistence.repo.ModelRunRepository;
import ar.ss.betting.persistence.repo.RoundRepository;
import ar.ss.betting.predictionproviders.domain.MatchPrediction;
import ar.ss.betting.predictionproviders.domain.ProviderProbabilityTriple;
import ar.ss.betting.predictionproviders.service.model.MatchPredictionResult;
import ar.ss.betting.predictionproviders.service.model.PredictionMatchRequest;
import ar.ss.betting.predictionproviders.service.model.PredictionQueryResponse;
import ar.ss.betting.predictionproviders.service.model.ProviderPredictionResult;
import ar.ss.betting.predictionproviders.service.model.ProviderPredictionStatus;
import ar.ss.betting.predictionproviders.service.model.ProviderRawPredictionSnapshot;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;

@Service
public class ModelRunScheduler {

    private static final List<Integer> PRESET_BUDGETS = List.of(32, 64, 128, 256);

    private static final String TRIGGER_OPENED = "OPENED";
    private static final String TRIGGER_T_MINUS_15 = "T_MINUS_15";

    private static final int T_MINUS_15_MINUTES = 15;
    private static final int LOOKAHEAD_HOURS = 72;
    private static final int OPENED_GRACE_MINUTES_AFTER_START = 2;

    private static final ProbabilityTriple DEFAULT_MARKET = ProbabilityTriple.fromProbabilities(0.5, 0.25, 0.25);
    private static final ProbabilityTriple DEFAULT_PUBLIC = ProbabilityTriple.fromProbabilities(0.5, 0.25, 0.25);

    private final RoundRepository roundRepository;
    private final MatchRepository matchRepository;
    private final MatchContextRepository matchContextRepository;
    private final ModelRunRepository modelRunRepository;
    private final RoundPersistenceService roundPersistenceService;
    private final PredictionSnapshotCacheService predictionSnapshotCacheService;
    private final PredictionResolutionService predictionResolutionService;

    public ModelRunScheduler(RoundRepository roundRepository,
                             MatchRepository matchRepository,
                             MatchContextRepository matchContextRepository,
                             ModelRunRepository modelRunRepository,
                             RoundPersistenceService roundPersistenceService,
                             PredictionSnapshotCacheService predictionSnapshotCacheService,
                             PredictionResolutionService predictionResolutionService) {
        this.roundRepository = Objects.requireNonNull(roundRepository);
        this.matchRepository = Objects.requireNonNull(matchRepository);
        this.matchContextRepository = Objects.requireNonNull(matchContextRepository);
        this.modelRunRepository = Objects.requireNonNull(modelRunRepository);
        this.roundPersistenceService = Objects.requireNonNull(roundPersistenceService);
        this.predictionSnapshotCacheService = Objects.requireNonNull(predictionSnapshotCacheService);
        this.predictionResolutionService = Objects.requireNonNull(predictionResolutionService);
    }

    @Scheduled(fixedDelay = 60_000)
    public void tick() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime horizon = now.plusHours(LOOKAHEAD_HOURS);

        List<RoundEntity> upcoming = roundRepository.findByStatusInAndStartDateBetweenOrderByStartDateAsc(
                List.of(RoundStatus.UPCOMING, RoundStatus.RUNNING),
                now,
                horizon
        );

        if (upcoming.isEmpty()) {
            return;
        }

        Map<Long, Round> roundsById = new LinkedHashMap<>();
        for (RoundEntity roundEntity : upcoming) {
            roundsById.put(roundEntity.getId(), loadRoundDomain(roundEntity.getId()));
        }

        List<ProviderRawPredictionSnapshot> providerSnapshots =
                predictionSnapshotCacheService.getLatestSnapshot().snapshots();

        for (RoundEntity roundEntity : upcoming) {
            Round round = roundsById.get(roundEntity.getId());
            List<PredictionMatchRequest> requests = toScopedRequests(roundEntity.getId(), round);

            PredictionQueryResponse resolved =
                    predictionResolutionService.resolve(requests, providerSnapshots);

            List<MatchPredictionResult> roundPredictionResults =
                    extractResultsForRound(resolved.getResults(), roundEntity.getId());

            ensureOpenedRuns(roundEntity, round, roundPredictionResults, now);
            ensureTMinus15Runs(roundEntity, round, roundPredictionResults, now);
        }
    }

    private void ensureOpenedRuns(RoundEntity roundEntity,
                                  Round round,
                                  List<MatchPredictionResult> roundPredictionResults,
                                  LocalDateTime now) {
        LocalDateTime start = roundEntity.getStartDate();

        boolean allowed =
                now.isBefore(start) ||
                        (!now.isBefore(start) && now.isBefore(start.plusMinutes(OPENED_GRACE_MINUTES_AFTER_START)));

        if (!allowed) {
            return;
        }

        long roundId = roundEntity.getId();
        for (int budget : PRESET_BUDGETS) {
            if (!modelRunRepository.existsByRoundIdAndBudgetInSekAndTrigger(roundId, budget, TRIGGER_OPENED)) {
                createRun(roundId, round, budget, TRIGGER_OPENED, roundPredictionResults);
            }
        }
    }

    private void ensureTMinus15Runs(RoundEntity roundEntity,
                                    Round round,
                                    List<MatchPredictionResult> roundPredictionResults,
                                    LocalDateTime now) {
        LocalDateTime start = roundEntity.getStartDate();
        boolean inWindow = now.isAfter(start.minusMinutes(T_MINUS_15_MINUTES)) && now.isBefore(start);
        if (!inWindow) {
            return;
        }

        long roundId = roundEntity.getId();
        for (int budget : PRESET_BUDGETS) {
            if (!modelRunRepository.existsByRoundIdAndBudgetInSekAndTrigger(roundId, budget, TRIGGER_T_MINUS_15)) {
                createRun(roundId, round, budget, TRIGGER_T_MINUS_15, roundPredictionResults);
            }
        }
    }

    private void createRun(long roundId,
                           Round round,
                           int budget,
                           String trigger,
                           List<MatchPredictionResult> roundPredictionResults) {
        ModelInput input = loadInputFromDbOrDefault(roundId, round, roundPredictionResults);

        EnsembleModel model = new EnsembleModel();
        ModelSelectionResult result = model.generateSelection(round, input, budget);

        roundPersistenceService.saveModelRun(roundId, budget, trigger, result, roundPredictionResults);
    }

    private Round loadRoundDomain(long roundId) {
        RoundEntity roundEntity = roundRepository.findById(roundId)
                .orElseThrow(() -> new IllegalArgumentException("Round not found: " + roundId));

        List<MatchEntity> matchEntities = matchRepository.findByRoundIdOrderByMatchNumberAsc(roundId);

        List<Match> matches = new ArrayList<>(matchEntities.size());
        for (MatchEntity m : matchEntities) {
            matches.add(new Match(
                    m.getMatchNumber(),
                    m.getStartDate(),
                    new Team(m.getHomeTeamName()),
                    new Team(m.getAwayTeamName())
            ));
        }

        return new Round(roundEntity.getStartDate(), roundEntity.getRoundType(), matches);
    }

    private ModelInput loadInputFromDbOrDefault(long roundId,
                                                Round round,
                                                List<MatchPredictionResult> roundPredictionResults) {
        List<MatchContextEntity> ctxRows = matchContextRepository.findByRoundIdOrderByMatchNumberAsc(roundId);
        Map<Integer, MatchContextEntity> byMatch = new HashMap<>();
        for (MatchContextEntity c : ctxRows) {
            byMatch.put(c.getMatchNumber(), c);
        }

        Map<Integer, List<ProbabilityTriple>> fetchedProvidersByMatch =
                toFetchedProviderTriplesByMatch(roundPredictionResults);

        Map<Integer, MatchContext> ctx = new HashMap<>();
        for (Match m : round.getMatches()) {
            MatchContextEntity row = byMatch.get(m.getMatchNumber());

            ProbabilityTriple market;
            ProbabilityTriple pub;

            if (row == null) {
                market = DEFAULT_MARKET;
                pub = DEFAULT_PUBLIC;
            } else {
                market = ProbabilityTriple.fromProbabilities(
                        row.getMarketHome(), row.getMarketDraw(), row.getMarketAway()
                );
                pub = ProbabilityTriple.fromProbabilities(
                        row.getPublicHome(), row.getPublicDraw(), row.getPublicAway()
                );
            }

            ctx.put(m.getMatchNumber(), new MatchContext(
                    market,
                    pub,
                    fetchedProvidersByMatch.getOrDefault(m.getMatchNumber(), List.of())
            ));
        }

        return new ModelInput(ctx);
    }

    private List<PredictionMatchRequest> toScopedRequests(long roundId, Round round) {
        List<PredictionMatchRequest> requests = new ArrayList<>();

        for (Match match : round.getMatches()) {
            requests.add(new PredictionMatchRequest(
                    toScopedClientMatchId(roundId, match.getMatchNumber()),
                    match.getHomeTeam().getName(),
                    match.getAwayTeam().getName(),
                    toUtcOffset(match.getStartDate())
            ));
        }

        return requests;
    }

    private List<MatchPredictionResult> extractResultsForRound(List<MatchPredictionResult> batchedResults,
                                                               long roundId) {
        if (batchedResults == null || batchedResults.isEmpty()) {
            return List.of();
        }

        List<MatchPredictionResult> out = new ArrayList<>();

        for (MatchPredictionResult result : batchedResults) {
            ScopedClientMatchId scoped = parseScopedClientMatchId(result.getClientMatchId());
            if (scoped.roundId() == roundId) {
                out.add(result);
            }
        }

        return out;
    }

    private Map<Integer, List<ProbabilityTriple>> toFetchedProviderTriplesByMatch(
            List<MatchPredictionResult> predictionResults) {

        Map<Integer, List<ProbabilityTriple>> out = new HashMap<>();

        if (predictionResults == null || predictionResults.isEmpty()) {
            return out;
        }

        for (MatchPredictionResult matchResult : predictionResults) {
            int matchNumber = parseMatchNumber(matchResult.getClientMatchId());

            List<ProbabilityTriple> providers = new ArrayList<>();

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

                    providers.add(ProbabilityTriple.fromProbabilities(
                            probabilities.getHomeWin(),
                            probabilities.getDraw(),
                            probabilities.getAwayWin()
                    ));
                }
            }

            out.put(matchNumber, providers);
        }

        return out;
    }

    private String toScopedClientMatchId(long roundId, int matchNumber) {
        return roundId + ":" + matchNumber;
    }

    private ScopedClientMatchId parseScopedClientMatchId(String clientMatchId) {
        if (clientMatchId == null || clientMatchId.isBlank()) {
            throw new IllegalArgumentException("Missing clientMatchId");
        }

        int colon = clientMatchId.indexOf(':');
        if (colon < 0) {
            throw new IllegalArgumentException("Expected scoped clientMatchId '<roundId>:<matchNumber>', got: " + clientMatchId);
        }

        try {
            long roundId = Long.parseLong(clientMatchId.substring(0, colon));
            int matchNumber = Integer.parseInt(clientMatchId.substring(colon + 1));

            if (roundId <= 0 || matchNumber <= 0) {
                throw new IllegalArgumentException("roundId and matchNumber must be positive: " + clientMatchId);
            }

            return new ScopedClientMatchId(roundId, matchNumber);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid scoped clientMatchId: " + clientMatchId, e);
        }
    }

    private int parseMatchNumber(String clientMatchId) {
        int colon = clientMatchId.indexOf(':');
        String matchNumberPart = colon >= 0 ? clientMatchId.substring(colon + 1) : clientMatchId;

        try {
            int parsed = Integer.parseInt(matchNumberPart);
            if (parsed <= 0) {
                throw new IllegalArgumentException("matchNumber must be positive: " + clientMatchId);
            }
            return parsed;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid clientMatchId match number: " + clientMatchId, e);
        }
    }

    private OffsetDateTime toUtcOffset(LocalDateTime value) {
        if (value == null) {
            return null;
        }
        return value.atOffset(ZoneOffset.UTC);
    }

    private record ScopedClientMatchId(long roundId, int matchNumber) { }
}