package ar.ss.betting.rework;

import ar.ss.betting.domain.Match;
import ar.ss.betting.domain.RoundStatus;
import ar.ss.betting.domain.Team;
import ar.ss.betting.domain.Round;
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
    private static final int OPENED_GRACE_MINUTES_AFTER_START = 2;

    private static final ProbabilityTriple DEFAULT_MARKET = ProbabilityTriple.fromProbabilities(0.5, 0.25, 0.25);
    private static final ProbabilityTriple DEFAULT_PUBLIC = ProbabilityTriple.fromProbabilities(0.5, 0.25, 0.25);

    private final RoundRepository roundRepository;
    private final MatchRepository matchRepository;
    private final MatchContextRepository matchContextRepository;
    private final ModelRunRepository modelRunRepository;
    private final RoundPersistenceService roundPersistenceService;

    public ModelRunScheduler(RoundRepository roundRepository,
                             MatchRepository matchRepository,
                             MatchContextRepository matchContextRepository,
                             ModelRunRepository modelRunRepository,
                             RoundPersistenceService roundPersistenceService) {
        this.roundRepository = Objects.requireNonNull(roundRepository);
        this.matchRepository = Objects.requireNonNull(matchRepository);
        this.matchContextRepository = Objects.requireNonNull(matchContextRepository);
        this.modelRunRepository = Objects.requireNonNull(modelRunRepository);
        this.roundPersistenceService = Objects.requireNonNull(roundPersistenceService);
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

        for (RoundEntity round : upcoming) {
            ensureOpenedRuns(round, now);
            ensureTMinus15Runs(round, now);
        }
    }

    private void ensureOpenedRuns(RoundEntity round, LocalDateTime now) {
        LocalDateTime start = round.getStartDate();

        boolean allowed =
                now.isBefore(start) ||
                        (!now.isBefore(start) && now.isBefore(start.plusMinutes(OPENED_GRACE_MINUTES_AFTER_START)));

        if (!allowed) return;

        long roundId = round.getId();
        for (int budget : PRESET_BUDGETS) {
            if (!modelRunRepository.existsByRoundIdAndBudgetInSekAndTrigger(roundId, budget, TRIGGER_OPENED)) {
                createRun(roundId, budget, TRIGGER_OPENED);
            }
        }
    }

    private void ensureTMinus15Runs(RoundEntity round, LocalDateTime now) {
        LocalDateTime start = round.getStartDate();
        boolean inWindow = now.isAfter(start.minusMinutes(T_MINUS_15_MINUTES)) && now.isBefore(start);
        if (!inWindow) return;

        long roundId = round.getId();
        for (int budget : PRESET_BUDGETS) {
            if (!modelRunRepository.existsByRoundIdAndBudgetInSekAndTrigger(roundId, budget, TRIGGER_T_MINUS_15)) {
                createRun(roundId, budget, TRIGGER_T_MINUS_15);
            }
        }
    }

    private void createRun(long roundId, int budget, String trigger) {
        Round round = loadRoundDomain(roundId);
        ModelInput input = loadInputFromDbOrDefault(roundId, round);

        EnsembleModel model = new EnsembleModel();
        ModelSelectionResult result = model.generateSelection(round, input, budget);

        roundPersistenceService.saveModelRun(roundId, budget, trigger, result);
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

    private ModelInput loadInputFromDbOrDefault(long roundId, Round round) {
        List<MatchContextEntity> ctxRows = matchContextRepository.findByRoundIdOrderByMatchNumberAsc(roundId);
        Map<Integer, MatchContextEntity> byMatch = new HashMap<>();
        for (MatchContextEntity c : ctxRows) {
            byMatch.put(c.getMatchNumber(), c);
        }

        Map<Integer, MatchContext> ctx = new HashMap<>();
        for (Match m : round.getMatches()) {
            MatchContextEntity row = byMatch.get(m.getMatchNumber());
            if (row == null) {
                ctx.put(m.getMatchNumber(), new MatchContext(
                        DEFAULT_MARKET,
                        DEFAULT_PUBLIC,
                        List.of()
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
                    List.of()
            ));
        }

        return new ModelInput(ctx);
    }
}