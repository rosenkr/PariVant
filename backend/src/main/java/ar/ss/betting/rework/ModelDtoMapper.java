package ar.ss.betting.rework;

import ar.ss.betting.domain.*;
import ar.ss.betting.model.MatchBuff;
import ar.ss.betting.model.MatchContext;
import ar.ss.betting.model.MatchInterventions;
import ar.ss.betting.model.MatchTag;
import ar.ss.betting.model.ModelInput;
import ar.ss.betting.model.ModelSelectionResult;
import ar.ss.betting.model.ProbabilityTriple;
import ar.ss.betting.model.Side;
import ar.ss.betting.model.TagType;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Maps API DTOs to domain/model objects and back.
 */
@Component
public class ModelDtoMapper {

    public ModelService.DomainRun toDomain(ModelSelectionRequestDto req) {

        if (req.roundType() == null || req.roundType().isBlank()) {
            throw new IllegalArgumentException("roundType is required");
        }
        if (req.roundStartDate() == null || req.roundStartDate().isBlank()) {
            throw new IllegalArgumentException("roundStartDate is required");
        }
        if (req.matches() == null || req.matches().isEmpty()) {
            throw new IllegalArgumentException("matches is required");
        }
        if (req.budgetInSek() == null || req.budgetInSek() <= 0) {
            throw new IllegalArgumentException("budgetInSek must be positive");
        }
        if (req.contexts() == null || req.contexts().isEmpty()) {
            throw new IllegalArgumentException("contexts is required (keyed by matchNumber)");
        }

        RoundType roundType = RoundType.valueOf(req.roundType());
        LocalDateTime roundStart = LocalDateTime.parse(req.roundStartDate());

        List<Match> matches = new ArrayList<>();
        for (ModelSelectionRequestDto.MatchDto m : req.matches()) {
            if (m.matchNumber() == null || m.matchNumber() <= 0) {
                throw new IllegalArgumentException("matchNumber must be positive");
            }
            if (m.startDate() == null || m.startDate().isBlank()) {
                throw new IllegalArgumentException("startDate is required for match " + m.matchNumber());
            }
            if (m.homeTeamName() == null || m.homeTeamName().isBlank()) {
                throw new IllegalArgumentException("homeTeamName is required for match " + m.matchNumber());
            }
            if (m.awayTeamName() == null || m.awayTeamName().isBlank()) {
                throw new IllegalArgumentException("awayTeamName is required for match " + m.matchNumber());
            }

            matches.add(new Match(
                    m.matchNumber(),
                    LocalDateTime.parse(m.startDate()),
                    new Team(m.homeTeamName()),
                    new Team(m.awayTeamName())
            ));
        }

        Round round = new Round(roundStart, roundType, matches);

        Map<Integer, MatchContext> contexts = new HashMap<>();
        for (Map.Entry<Integer, ModelSelectionRequestDto.MatchContextDto> e : req.contexts().entrySet()) {
            Integer matchNumber = e.getKey();
            ModelSelectionRequestDto.MatchContextDto c = e.getValue();

            if (matchNumber == null || matchNumber <= 0) {
                throw new IllegalArgumentException("contexts keys must be positive matchNumbers");
            }
            if (c == null) {
                throw new IllegalArgumentException("context is null for match " + matchNumber);
            }
            if (c.market() == null || c.publicPick() == null) {
                throw new IllegalArgumentException("market and publicPick are required for match " + matchNumber);
            }

            ProbabilityTriple market = toProbabilityTriple(c.market());
            ProbabilityTriple publicPick = toProbabilityTriple(c.publicPick());

            List<ProbabilityTriple> providers = new ArrayList<>();
            if (c.providers() != null) {
                for (ModelSelectionRequestDto.ProbabilityTripleDto providerDto : c.providers()) {
                    providers.add(toProbabilityTriple(providerDto));
                }
            }

            contexts.put(matchNumber, new MatchContext(market, publicPick, providers));
        }

        Map<Integer, MatchInterventions> interventions = new HashMap<>();
        if (req.interventions() != null) {
            for (Map.Entry<Integer, ModelSelectionRequestDto.MatchInterventionsDto> e : req.interventions().entrySet()) {
                Integer matchNumber = e.getKey();
                ModelSelectionRequestDto.MatchInterventionsDto dto = e.getValue();

                if (matchNumber == null || matchNumber <= 0) {
                    throw new IllegalArgumentException("interventions keys must be positive matchNumbers");
                }
                if (dto == null) {
                    throw new IllegalArgumentException("intervention is null for match " + matchNumber);
                }

                interventions.put(matchNumber, toMatchInterventions(dto));
            }
        }

        ModelInput modelInput = new ModelInput(contexts, interventions);

        return new ModelService.DomainRun(round, modelInput, req.budgetInSek());
    }

    public ModelSelectionResponseDto toResponseDto(ModelSelectionResult result) {
        Map<Integer, List<String>> selections = new TreeMap<>();
        for (Map.Entry<Integer, Set<Outcome>> e : result.getSelections().entrySet()) {
            List<String> outcomes = e.getValue().stream().map(Enum::name).toList();
            selections.put(e.getKey(), outcomes);
        }

        Map<Integer, ModelSelectionResponseDto.ProbabilityTripleDto> internalProbabilities = new TreeMap<>();
        result.getInternalProbabilities().forEach((matchNumber, triple) ->
                internalProbabilities.put(matchNumber, new ModelSelectionResponseDto.ProbabilityTripleDto(
                        triple.get(Outcome.HOME_WIN),
                        triple.get(Outcome.DRAW),
                        triple.get(Outcome.AWAY_WIN)
                ))
        );

        return new ModelSelectionResponseDto(
                result.getModelName(),
                result.getGeneratedAt().toString(),
                result.getTotalCostInSek(),
                result.getHalfGuardsCount(),
                selections,
                internalProbabilities
        );
    }

    private MatchInterventions toMatchInterventions(ModelSelectionRequestDto.MatchInterventionsDto dto) {
        List<MatchTag> tags = new ArrayList<>();
        if (dto.tags() != null) {
            for (ModelSelectionRequestDto.TagDto tagDto : dto.tags()) {
                tags.add(toMatchTag(tagDto));
            }
        }

        MatchBuff buff = null;
        if (dto.buff() != null) {
            buff = toMatchBuff(dto.buff());
        }

        return new MatchInterventions(tags, buff);
    }

    private MatchTag toMatchTag(ModelSelectionRequestDto.TagDto dto) {
        if (dto == null) {
            throw new IllegalArgumentException("tag cannot be null");
        }
        if (dto.type() == null || dto.type().isBlank()) {
            throw new IllegalArgumentException("tag.type is required");
        }

        TagType type = TagType.valueOf(dto.type());

        return switch (type) {
            case NEUTRAL_VENUE -> MatchTag.neutralVenue();
            case KEY_ABSENCE -> MatchTag.keyAbsence(parseRequiredSide(dto.affectedSide(), type));
            case SHORT_REST -> MatchTag.shortRest(parseRequiredSide(dto.affectedSide(), type));
            case INCENTIVE_LACK -> MatchTag.incentiveLack(parseRequiredSide(dto.affectedSide(), type));
        };
    }

    private Side parseRequiredSide(String value, TagType type) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("affectedSide is required for tag type " + type);
        }
        return Side.valueOf(value);
    }

    private MatchBuff toMatchBuff(ModelSelectionRequestDto.BuffDto dto) {
        if (dto.targetOutcome() == null || dto.targetOutcome().isBlank()) {
            throw new IllegalArgumentException("buff.targetOutcome is required");
        }
        if (dto.points() == null) {
            throw new IllegalArgumentException("buff.points is required");
        }

        Outcome outcome = Outcome.valueOf(dto.targetOutcome());
        return new MatchBuff(outcome, dto.points());
    }

    private ProbabilityTriple toProbabilityTriple(ModelSelectionRequestDto.ProbabilityTripleDto dto) {
        if (dto == null) {
            throw new IllegalArgumentException("ProbabilityTriple cannot be null");
        }
        if (dto.homeWin() == null || dto.draw() == null || dto.awayWin() == null) {
            throw new IllegalArgumentException("ProbabilityTriple must include homeWin, draw, awayWin");
        }
        return ProbabilityTriple.fromProbabilities(dto.homeWin(), dto.draw(), dto.awayWin());
    }
}