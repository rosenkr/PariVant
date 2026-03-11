package ar.ss.betting.service;

import ar.ss.betting.api.dto.ModelSelectionRequestDto;
import ar.ss.betting.domain.GameRound;
import ar.ss.betting.domain.GameType;
import ar.ss.betting.domain.Match;
import ar.ss.betting.domain.Team;
import ar.ss.betting.model.*;
import ar.ss.betting.persistence.entity.ModelRunEntity;
import ar.ss.betting.persistence.repo.ModelRunRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class RoundApiService {

    private static final String TRIGGER_MANUAL = "MANUAL";
    private static final List<Integer> PRESET_BUDGETS = List.of(32, 64, 128, 256);

    private final RoundPersistenceService roundPersistenceService;
    private final ModelRunRepository modelRunRepository;

    // Still using RuleBasedModel directly (as in your current file)
    private final GameModel model = new RuleBasedModel();

    public RoundApiService(RoundPersistenceService roundPersistenceService,
                           ModelRunRepository modelRunRepository) {
        this.roundPersistenceService = Objects.requireNonNull(roundPersistenceService);
        this.modelRunRepository = Objects.requireNonNull(modelRunRepository);
    }

    public long createRound(GameType gameType,
                            LocalDateTime roundStartDate,
                            List<ModelSelectionRequestDto.MatchDto> matches) {

        Objects.requireNonNull(gameType, "gameType");
        Objects.requireNonNull(roundStartDate, "roundStartDate");
        Objects.requireNonNull(matches, "matches");

        List<Match> domainMatches = new ArrayList<>(matches.size());
        for (ModelSelectionRequestDto.MatchDto m : matches) {
            domainMatches.add(new Match(
                    m.matchNumber(),
                    LocalDateTime.parse(m.startDate()),
                    new Team(m.homeTeamName()),
                    new Team(m.awayTeamName())
            ));
        }

        GameRound round = new GameRound(roundStartDate, gameType, domainMatches);
        return roundPersistenceService.saveRound(round).id();
    }

    /** Manual runs created via API are tagged MANUAL */
    public long runModelAndPersist(long roundId,
                                   int budgetInSek,
                                   Map<Integer, ModelSelectionRequestDto.MatchContextDto> contexts,
                                   ModelSelectionRequestDto.WeightsDto weightsDto,
                                   ModelSelectionRequestDto.DecisionParametersDto decisionParamsDto) {

        return runModelAndPersistWithTrigger(
                roundId,
                budgetInSek,
                contexts,
                weightsDto,
                decisionParamsDto,
                TRIGGER_MANUAL
        );
    }

    /**
     * Allows callers (scheduler/admin) to tag runs as OPENED / T_MINUS_15 etc.
     * (Tipzer ingest should NOT call this; ingest persists data, scheduler produces runs.)
     */
    public long runModelAndPersistWithTrigger(long roundId,
                                              int budgetInSek,
                                              Map<Integer, ModelSelectionRequestDto.MatchContextDto> contexts,
                                              ModelSelectionRequestDto.WeightsDto weightsDto,
                                              ModelSelectionRequestDto.DecisionParametersDto decisionParamsDto,
                                              String trigger) {

        Objects.requireNonNull(contexts, "contexts");
        Objects.requireNonNull(trigger, "trigger");

        AdjustmentWeights weights = (weightsDto == null)
                ? AdjustmentWeights.none()
                : new AdjustmentWeights(
                safeDouble(weightsDto.recentFormWeight(), 0.0)
        );

        DecisionParameters decisionParameters = (decisionParamsDto == null)
                ? DecisionParameters.defaults()
                : new DecisionParameters(
                safeDouble(decisionParamsDto.probabilityFloorTopptipset(), 0.12),
                safeDouble(decisionParamsDto.probabilityFloorStryktipset(), 0.18),
                safeDouble(decisionParamsDto.valueThresholdTopptipset(), 0.02),
                safeDouble(decisionParamsDto.valueThresholdStryktipset(), 0.04),
                safeInt(decisionParamsDto.maxFullGuardsTopptipset(), 1),
                safeInt(decisionParamsDto.maxFullGuardsStryktipset(), 2)
        );

        // Build ModelInput from DTO contexts (local mapping)
        Map<Integer, MatchContext> ctx = new HashMap<>();
        for (Map.Entry<Integer, ModelSelectionRequestDto.MatchContextDto> e : contexts.entrySet()) {
            Integer matchNumber = e.getKey();
            ModelSelectionRequestDto.MatchContextDto c = e.getValue();

            ProbabilityTriple market = ProbabilityTriple.fromProbabilities(
                    c.market().homeWin(), c.market().draw(), c.market().awayWin()
            );
            ProbabilityTriple pub = ProbabilityTriple.fromProbabilities(
                    c.publicPick().homeWin(), c.publicPick().draw(), c.publicPick().awayWin()
            );

            ctx.put(matchNumber, new MatchContext(
                    market,
                    pub,
                    c.homeRecentFormScore(),
                    c.awayRecentFormScore()
            ));
        }

        ModelInput input = new ModelInput(ctx);

        // IMPORTANT: RuleBasedModel expects GameRound, but we’re running by roundId.
        // Your old approach was: fetch round + matches from DB somewhere else.
        // For now (until Tipzer ingestion is wired end-to-end), this method is for MANUAL use only.
        // If you still use it, ensure the caller provides a GameRound or you add a DB lookup here.
        //
        // To avoid breaking compile now, we keep the “manual run” path as-is only for persistence shape.
        //
        // If you currently still use this in production flow, we should add a DB lookup in the next step.
        throw new UnsupportedOperationException(
                "runModelAndPersistWithTrigger requires round lookup to build GameRound. " +
                        "Use scheduler + persisted contexts for automatic runs; or add DB lookup here."
        );
    }

    /**
     * Public read-only endpoint helper used by PublicModelRunController:
     * returns latest run per preset budget (32/64/128/256), newest first per budget.
     */
    public List<ModelRunView> getLatestPresetModelRuns(long roundId) {
        List<ModelRunEntity> runs = modelRunRepository.findByGameRoundIdOrderByGeneratedAtDesc(roundId);

        Map<Integer, ModelRunEntity> latestByBudget = new LinkedHashMap<>();
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
            if (r != null) out.add(toView(r));
        }
        return out;
    }

    private ModelRunView toView(ModelRunEntity r) {
        return new ModelRunView(
                r.getId(),
                r.getModelName(),
                r.getGeneratedAt(),
                r.getBudgetInSek(),
                r.getTotalCostInSek(),
                r.getHalfGuardsCount(),
                r.getFullGuardsCount(),
                r.getTrigger(),
                r.getSelectionsJson(),
                r.getWeightsJson(),
                r.getDecisionParametersJson()
        );
    }

    public record ModelRunView(
            long id,
            String modelName,
            LocalDateTime generatedAt,
            int budgetInSek,
            int totalCostInSek,
            int halfGuardsCount,
            int fullGuardsCount,
            String trigger,
            String selectionsJson,
            String weightsJson,
            String decisionParametersJson
    ) { }

    private static double safeDouble(Double v, double defaultValue) {
        return v == null ? defaultValue : v;
    }

    private static int safeInt(Integer v, int defaultValue) {
        return v == null ? defaultValue : v;
    }
}