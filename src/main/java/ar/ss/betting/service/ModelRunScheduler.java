package ar.ss.betting.service;

import ar.ss.betting.domain.GameRound;
import ar.ss.betting.domain.Match;
import ar.ss.betting.domain.Team;
import ar.ss.betting.model.*;
import ar.ss.betting.persistence.entity.GameRoundEntity;
import ar.ss.betting.persistence.entity.MatchContextEntity;
import ar.ss.betting.persistence.entity.MatchEntity;
import ar.ss.betting.persistence.repo.GameRoundRepository;
import ar.ss.betting.persistence.repo.MatchContextRepository;
import ar.ss.betting.persistence.repo.MatchRepository;
import ar.ss.betting.persistence.repo.ModelRunRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class ModelRunScheduler {

    private static final List<Integer> PRESET_BUDGETS = List.of(32, 64, 128, 256);

    private static final String TRIGGER_OPENED = "OPENED";
    private static final String TRIGGER_T_MINUS_15 = "T_MINUS_15";

    private static final int T_MINUS_15_MINUTES = 15;
    private static final int LOOKAHEAD_HOURS = 72;

    private static final int DEFAULT_FORM_SCORE = 5;
    private static final ProbabilityTriple DEFAULT_MARKET = ProbabilityTriple.fromProbabilities(0.5, 0.25, 0.25);
    private static final ProbabilityTriple DEFAULT_PUBLIC = ProbabilityTriple.fromProbabilities(0.5, 0.25, 0.25);

    private final GameRoundRepository gameRoundRepository;
    private final MatchRepository matchRepository;
    private final MatchContextRepository matchContextRepository;
    private final ModelRunRepository modelRunRepository;
    private final RoundPersistenceService roundPersistenceService;

    public ModelRunScheduler(GameRoundRepository gameRoundRepository,
                             MatchRepository matchRepository,
                             MatchContextRepository matchContextRepository,
                             ModelRunRepository modelRunRepository,
                             RoundPersistenceService roundPersistenceService) {
        this.gameRoundRepository = Objects.requireNonNull(gameRoundRepository);
        this.matchRepository = Objects.requireNonNull(matchRepository);
        this.matchContextRepository = Objects.requireNonNull(matchContextRepository);
        this.modelRunRepository = Objects.requireNonNull(modelRunRepository);
        this.roundPersistenceService = Objects.requireNonNull(roundPersistenceService);
    }

    @Scheduled(fixedDelay = 60_000)
    public void tick() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime horizon = now.plusHours(LOOKAHEAD_HOURS);

        List<GameRoundEntity> upcoming = gameRoundRepository.findUpcomingRounds(now, horizon);

        for (GameRoundEntity round : upcoming) {
            ensureOpenedRuns(round);
            ensureTMinus15Runs(round, now);
        }
    }

    private void ensureOpenedRuns(GameRoundEntity round) {
        long roundId = round.getId();
        for (int budget : PRESET_BUDGETS) {
            if (!modelRunRepository.existsByGameRoundIdAndBudgetInSekAndTrigger(roundId, budget, TRIGGER_OPENED)) {
                createRun(roundId, budget, TRIGGER_OPENED);
            }
        }
    }

    private void ensureTMinus15Runs(GameRoundEntity round, LocalDateTime now) {
        LocalDateTime start = round.getStartDate();
        boolean inWindow = now.isAfter(start.minusMinutes(T_MINUS_15_MINUTES)) && now.isBefore(start);
        if (!inWindow) return;

        long roundId = round.getId();
        for (int budget : PRESET_BUDGETS) {
            if (!modelRunRepository.existsByGameRoundIdAndBudgetInSekAndTrigger(roundId, budget, TRIGGER_T_MINUS_15)) {
                createRun(roundId, budget, TRIGGER_T_MINUS_15);
            }
        }
    }

    private void createRun(long roundId, int budget, String trigger) {
        GameRound round = loadRoundDomain(roundId);
        ModelInput input = loadInputFromDbOrDefault(roundId, round);

        AdjustmentWeights weights = AdjustmentWeights.none();
        DecisionParameters params = DecisionParameters.defaults();

        RuleBasedModel model = new RuleBasedModel(weights, params);
        ModelSelectionResult result = model.generateSelection(round, input, budget);

        roundPersistenceService.saveModelRun(roundId, budget, trigger, result, weights, params);
    }

    private GameRound loadRoundDomain(long roundId) {
        GameRoundEntity roundEntity = gameRoundRepository.findById(roundId)
                .orElseThrow(() -> new IllegalArgumentException("Round not found: " + roundId));

        List<MatchEntity> matchEntities = matchRepository.findByGameRoundIdOrderByMatchNumberAsc(roundId);

        List<Match> matches = new ArrayList<>(matchEntities.size());
        for (MatchEntity m : matchEntities) {
            matches.add(new Match(
                    m.getMatchNumber(),
                    m.getStartDate(),
                    new Team(m.getHomeTeamName()),
                    new Team(m.getAwayTeamName())
            ));
        }

        return new GameRound(roundEntity.getStartDate(), roundEntity.getGameType(), matches);
    }

    private ModelInput loadInputFromDbOrDefault(long roundId, GameRound round) {
        List<MatchContextEntity> ctxRows = matchContextRepository.findByGameRoundIdOrderByMatchNumberAsc(roundId);
        Map<Integer, MatchContextEntity> byMatch = new HashMap<>();
        for (MatchContextEntity c : ctxRows) {
            byMatch.put(c.getMatchNumber(), c);
        }

        Map<Integer, MatchContext> ctx = new HashMap<>();
        for (Match m : round.getMatches()) {
            MatchContextEntity row = byMatch.get(m.getMatchNumber());
            if (row == null) {
                ctx.put(m.getMatchNumber(), new MatchContext(
                        DEFAULT_MARKET, DEFAULT_PUBLIC, DEFAULT_FORM_SCORE, DEFAULT_FORM_SCORE
                ));
                continue;
            }

            ProbabilityTriple market = ProbabilityTriple.fromProbabilities(
                    row.getMarketHome(), row.getMarketDraw(), row.getMarketAway()
            );
            ProbabilityTriple pub = ProbabilityTriple.fromProbabilities(
                    row.getPublicHome(), row.getPublicDraw(), row.getPublicAway()
            );

            ctx.put(m.getMatchNumber(), new MatchContext(
                    market,
                    pub,
                    row.getHomeRecentFormScore(),
                    row.getAwayRecentFormScore()
            ));
        }

        return new ModelInput(ctx);
    }
}