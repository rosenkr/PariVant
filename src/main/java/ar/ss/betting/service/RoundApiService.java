package ar.ss.betting.service;

import ar.ss.betting.domain.*;
import ar.ss.betting.model.*;
import ar.ss.betting.persistence.entity.GameRoundEntity;
import ar.ss.betting.persistence.entity.MatchEntity;
import ar.ss.betting.persistence.repo.GameRoundRepository;
import ar.ss.betting.persistence.repo.MatchRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class RoundApiService {

    private final GameRoundRepository gameRoundRepository;
    private final MatchRepository matchRepository;
    private final RoundPersistenceService roundPersistenceService;

    public RoundApiService(GameRoundRepository gameRoundRepository,
                           MatchRepository matchRepository,
                           RoundPersistenceService roundPersistenceService) {
        this.gameRoundRepository = Objects.requireNonNull(gameRoundRepository);
        this.matchRepository = Objects.requireNonNull(matchRepository);
        this.roundPersistenceService = Objects.requireNonNull(roundPersistenceService);
    }

    public long createRound(GameType gameType, LocalDateTime roundStart, List<ar.ss.betting.api.dto.ModelSelectionRequestDto.MatchDto> matchesDto) {
        Objects.requireNonNull(gameType);
        Objects.requireNonNull(roundStart);
        Objects.requireNonNull(matchesDto);

        List<Match> matches = new ArrayList<>(matchesDto.size());
        for (var m : matchesDto) {
            matches.add(new Match(
                    m.matchNumber(),
                    LocalDateTime.parse(m.startDate()),
                    new Team(m.homeTeamName()),
                    new Team(m.awayTeamName())
            ));
        }

        GameRound round = new GameRound(roundStart, gameType, matches);
        return roundPersistenceService.saveRound(round).id();
    }

    public long runModelAndPersist(long roundId,
                                   int budgetInSek,
                                   Map<Integer, ar.ss.betting.api.dto.ModelSelectionRequestDto.MatchContextDto> contexts,
                                   ar.ss.betting.api.dto.ModelSelectionRequestDto.WeightsDto weightsDto,
                                   ar.ss.betting.api.dto.ModelSelectionRequestDto.DecisionParametersDto paramsDto) {

        if (budgetInSek <= 0) throw new IllegalArgumentException("budgetInSek must be positive");
        Objects.requireNonNull(contexts, "contexts cannot be null");

        GameRound round = loadRoundDomain(roundId);

        AdjustmentWeights weights = (weightsDto == null)
                ? AdjustmentWeights.none()
                : new AdjustmentWeights(defaultIfNull(weightsDto.recentFormWeight(), 0.0));

        DecisionParameters params = (paramsDto == null)
                ? DecisionParameters.defaults()
                : new DecisionParameters(
                defaultIfNull(paramsDto.probabilityFloorTopptipset(), DecisionParameters.DEFAULT_PROBABILITY_FLOOR_TOPPTIPSET),
                defaultIfNull(paramsDto.probabilityFloorStryktipset(), DecisionParameters.DEFAULT_PROBABILITY_FLOOR_STRYKTIPSET),
                defaultIfNull(paramsDto.valueThresholdTopptipset(), DecisionParameters.DEFAULT_VALUE_THRESHOLD_TOPPTIPSET),
                defaultIfNull(paramsDto.valueThresholdStryktipset(), DecisionParameters.DEFAULT_VALUE_THRESHOLD_STRYKTIPSET),
                defaultIfNull(paramsDto.maxFullGuardsTopptipset(), DecisionParameters.DEFAULT_MAX_FULL_GUARDS_TOPPTIPSET),
                defaultIfNull(paramsDto.maxFullGuardsStryktipset(), DecisionParameters.DEFAULT_MAX_FULL_GUARDS_STRYKTIPSET)
        );

        ModelInput modelInput = toModelInput(contexts);

        RuleBasedModel model = new RuleBasedModel(weights, params);
        ModelSelectionResult result = model.generateSelection(round, modelInput, budgetInSek);

        return roundPersistenceService.saveModelRun(roundId, budgetInSek, result, weights, params).id();
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

    private ModelInput toModelInput(Map<Integer, ar.ss.betting.api.dto.ModelSelectionRequestDto.MatchContextDto> contexts) {
        Map<Integer, MatchContext> ctx = new HashMap<>();
        for (var e : contexts.entrySet()) {
            Integer matchNumber = e.getKey();
            var dto = e.getValue();
            if (matchNumber == null || matchNumber <= 0) {
                throw new IllegalArgumentException("contexts keys must be positive matchNumbers");
            }
            if (dto == null) {
                throw new IllegalArgumentException("context is null for match " + matchNumber);
            }

            ProbabilityTriple market = new ProbabilityTriple(dto.market().homeWin(), dto.market().draw(), dto.market().awayWin());
            ProbabilityTriple pub = new ProbabilityTriple(dto.publicPick().homeWin(), dto.publicPick().draw(), dto.publicPick().awayWin());

            ctx.put(matchNumber, new MatchContext(
                    market,
                    pub,
                    dto.homeRecentFormScore(),
                    dto.awayRecentFormScore()
            ));
        }
        return new ModelInput(ctx);
    }

    private double defaultIfNull(Double v, double def) {
        return (v == null) ? def : v;
    }

    private int defaultIfNull(Integer v, int def) {
        return (v == null) ? def : v;
    }
}