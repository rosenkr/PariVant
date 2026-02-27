package ar.ss.betting.service;

import ar.ss.betting.domain.GameRound;
import ar.ss.betting.domain.Match;
import ar.ss.betting.domain.Team;
import ar.ss.betting.model.*;
import ar.ss.betting.persistence.entity.GameRoundEntity;
import ar.ss.betting.persistence.entity.MatchEntity;
import ar.ss.betting.persistence.repo.GameRoundRepository;
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

    // Placeholder until we have real provider ingestion:
    private static final int DEFAULT_FORM_SCORE = 5;
    private static final ProbabilityTriple DEFAULT_MARKET = new ProbabilityTriple(0.50, 0.25, 0.25);
    private static final ProbabilityTriple DEFAULT_PUBLIC = new ProbabilityTriple(0.50, 0.25, 0.25);

    private final GameRoundRepository gameRoundRepository;
    private final MatchRepository matchRepository;
    private final ModelRunRepository modelRunRepository;
    private final RoundPersistenceService roundPersistenceService;

    public ModelRunScheduler(GameRoundRepository gameRoundRepository,
                             MatchRepository matchRepository,
                             ModelRunRepository modelRunRepository,
                             RoundPersistenceService roundPersistenceService) {
        this.gameRoundRepository = Objects.requireNonNull(gameRoundRepository);
        this.matchRepository = Objects.requireNonNull(matchRepository);
        this.modelRunRepository = Objects.requireNonNull(modelRunRepository);
        this.roundPersistenceService = Objects.requireNonNull(roundPersistenceService);
    }

    /**
     * Scheduler job:
     * - Only considers UPCOMING rounds (startDate > now). Once a round starts, betting is closed → no more runs.
     * - Ensures OPENED runs exist for preset budgets.
     * - Ensures T_MINUS_15 runs exist for preset budgets in [start-15min, start).
     */
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
        Long roundId = round.getId();

        for (int budget : PRESET_BUDGETS) {
            if (!modelRunRepository.existsByGameRoundIdAndBudgetInSekAndTrigger(roundId, budget, TRIGGER_OPENED)) {
                createRun(roundId, budget, TRIGGER_OPENED);
            }
        }
    }

    private void ensureTMinus15Runs(GameRoundEntity round, LocalDateTime now) {
        LocalDateTime start = round.getStartDate();

        boolean inWindow = now.isAfter(start.minusMinutes(T_MINUS_15_MINUTES)) && now.isBefore(start);
        if (!inWindow) {
            return;
        }

        Long roundId = round.getId();

        for (int budget : PRESET_BUDGETS) {
            if (!modelRunRepository.existsByGameRoundIdAndBudgetInSekAndTrigger(roundId, budget, TRIGGER_T_MINUS_15)) {
                createRun(roundId, budget, TRIGGER_T_MINUS_15);
            }
        }
    }

    private void createRun(Long roundId, int budget, String trigger) {
        GameRound round = loadRoundDomain(roundId);
        ModelInput input = defaultInput(round);

        AdjustmentWeights weights = AdjustmentWeights.none();
        DecisionParameters params = DecisionParameters.defaults();

        RuleBasedModel model = new RuleBasedModel(weights, params);
        ModelSelectionResult result = model.generateSelection(round, input, budget);

        roundPersistenceService.saveModelRun(roundId, budget, trigger, result, weights, params);
    }

    private GameRound loadRoundDomain(Long roundId) {
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

    private ModelInput defaultInput(GameRound round) {
        Map<Integer, MatchContext> ctx = new HashMap<>();
        for (Match m : round.getMatches()) {
            ctx.put(m.getMatchNumber(), new MatchContext(
                    DEFAULT_MARKET,
                    DEFAULT_PUBLIC,
                    DEFAULT_FORM_SCORE,
                    DEFAULT_FORM_SCORE
            ));
        }
        return new ModelInput(ctx);
    }
}