package ar.ss.betting.rework;

import ar.ss.betting.domain.Match;
import ar.ss.betting.domain.Round;
import ar.ss.betting.domain.RoundType;
import ar.ss.betting.domain.Team;
import ar.ss.betting.model.*;
import ar.ss.betting.persistence.entity.RoundEntity;
import ar.ss.betting.persistence.entity.MatchEntity;
import ar.ss.betting.persistence.entity.ModelRunEntity;
import ar.ss.betting.persistence.repo.MatchRepository;
import ar.ss.betting.persistence.repo.ModelRunRepository;
import ar.ss.betting.persistence.repo.RoundRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class RoundApiService {

    private static final String TRIGGER_MANUAL = "MANUAL";
    private static final List<Integer> PRESET_BUDGETS = List.of(32, 64, 128, 256);

    private final RoundPersistenceService roundPersistenceService;
    private final RoundRepository gameRoundRepository;
    private final MatchRepository matchRepository;
    private final ModelRunRepository modelRunRepository;

    public RoundApiService(RoundPersistenceService roundPersistenceService,
                           RoundRepository gameRoundRepository,
                           MatchRepository matchRepository,
                           ModelRunRepository modelRunRepository) {
        this.roundPersistenceService = Objects.requireNonNull(roundPersistenceService);
        this.gameRoundRepository = Objects.requireNonNull(gameRoundRepository);
        this.matchRepository = Objects.requireNonNull(matchRepository);
        this.modelRunRepository = Objects.requireNonNull(modelRunRepository);
    }

    public long createRound(RoundType roundType,
                            LocalDateTime roundStartDate,
                            List<ModelSelectionRequestDto.MatchDto> matches) {

        Objects.requireNonNull(roundType, "roundType");
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

        Round round = new Round(roundStartDate, roundType, domainMatches);
        return roundPersistenceService.saveRound(round).id();
    }

    public long runModelAndPersist(long roundId,
                                   int budgetInSek,
                                   Map<Integer, ModelSelectionRequestDto.MatchContextDto> contexts,
                                   Map<Integer, ModelSelectionRequestDto.MatchInterventionsDto> interventions) {

        return runModelAndPersistWithTrigger(
                roundId,
                budgetInSek,
                contexts,
                interventions,
                TRIGGER_MANUAL
        );
    }

    public long runModelAndPersistWithTrigger(long roundId,
                                              int budgetInSek,
                                              Map<Integer, ModelSelectionRequestDto.MatchContextDto> contexts,
                                              Map<Integer, ModelSelectionRequestDto.MatchInterventionsDto> interventions,
                                              String trigger) {

        Objects.requireNonNull(contexts, "contexts");
        Objects.requireNonNull(trigger, "trigger");

        Round round = loadRound(roundId);

        ModelInput modelInput = new ModelInput(
                toMatchContexts(contexts),
                toMatchInterventions(interventions)
        );

        EnsembleModel model = new EnsembleModel();
        ModelSelectionResult result = model.generateSelection(round, modelInput, budgetInSek);

        return roundPersistenceService.saveModelRun(roundId, budgetInSek, trigger, result).id();
    }

    public List<ModelRunView> getLatestPresetModelRuns(long roundId) {
        List<ModelRunEntity> runs = modelRunRepository.findByRoundIdOrderByGeneratedAtDesc(roundId);

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

    private Round loadRound(long roundId) {
        RoundEntity roundEntity = gameRoundRepository.findById(roundId)
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

        return new Round(
                roundEntity.getStartDate(),
                roundEntity.getRoundType(),
                matches
        );
    }

    private Map<Integer, MatchContext> toMatchContexts(Map<Integer, ModelSelectionRequestDto.MatchContextDto> contexts) {
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
                r.getInternalProbabilitiesJson()
        );
    }

    public record ModelRunView(
            long id,
            String modelName,
            LocalDateTime generatedAt,
            int budgetInSek,
            int totalCostInSek,
            int halfGuardsCount,
            String trigger,
            String selectionsJson,
            String internalProbabilitiesJson
    ) { }
}