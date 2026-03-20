package ar.ss.betting.service;

import ar.ss.betting.api.dto.ModelSelectionRequestDto;
import ar.ss.betting.api.dto.ModelSelectionResponseDto;
import ar.ss.betting.domain.*;
import ar.ss.betting.model.*;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Maps API DTOs (records) to domain/model objects and back.
 *
 * Keeping mapping logic out of controller/service keeps things clean and testable.
 */
@Component
public class ModelDtoMapper {

    public ModelService.DomainRun toDomain(ModelSelectionRequestDto req) {

        if (req.gameType() == null || req.gameType().isBlank()) {
            throw new IllegalArgumentException("gameType is required");
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

        GameType gameType = GameType.valueOf(req.gameType());
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

        GameRound round = new GameRound(roundStart, gameType, matches);

        // Build ModelInput
        Map<Integer, MatchContext> ctx = new HashMap<>();
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
            if (c.homeRecentFormScore() == null || c.awayRecentFormScore() == null) {
                throw new IllegalArgumentException("homeRecentFormScore and awayRecentFormScore are required for match " + matchNumber);
            }

            ProbabilityTriple market = toProbabilityTriple(c.market());
            ProbabilityTriple pub = toProbabilityTriple(c.publicPick());

            ctx.put(matchNumber, new MatchContext(
                    market,
                    pub,
                    c.homeRecentFormScore(),
                    c.awayRecentFormScore()
            ));
        }

        ModelInput modelInput = new ModelInput(ctx);

        AdjustmentWeights weights = (req.weights() == null)
                ? AdjustmentWeights.none()
                : new AdjustmentWeights(
                defaultIfNull(req.weights().recentFormWeight(), 0.0)
        );

        DecisionParameters decisionParameters = (req.decisionParameters() == null)
                ? DecisionParameters.defaults()
                : new DecisionParameters(
                defaultIfNull(req.decisionParameters().probabilityFloorTopptipset(), 0.12),
                defaultIfNull(req.decisionParameters().probabilityFloorStryktipset(), 0.18),
                defaultIfNull(req.decisionParameters().valueThresholdTopptipset(), 0.02),
                defaultIfNull(req.decisionParameters().valueThresholdStryktipset(), 0.04),
                defaultIfNull(req.decisionParameters().maxFullGuardsTopptipset(), 1),
                defaultIfNull(req.decisionParameters().maxFullGuardsStryktipset(), 2)
        );

        return new ModelService.DomainRun(round, modelInput, req.budgetInSek(), weights, decisionParameters);
    }

    public ModelSelectionResponseDto toResponseDto(ModelSelectionResult result) {

        Map<Integer, List<String>> selections = new TreeMap<>();
        for (Map.Entry<Integer, Set<Outcome>> e : result.getSelections().entrySet()) {
            List<String> outcomes = e.getValue().stream().map(Enum::name).toList();
            selections.put(e.getKey(), outcomes);
        }

        return new ModelSelectionResponseDto(
                result.getModelName(),
                result.getGeneratedAt().toString(),
                result.getTotalCostInSek(),
                result.getHalfGuardsCount(),
                result.getFullGuardsCount(),
                selections
        );
    }

    private ProbabilityTriple toProbabilityTriple(ModelSelectionRequestDto.ProbabilityTripleDto dto) {
        if (dto.homeWin() == null || dto.draw() == null || dto.awayWin() == null) {
            throw new IllegalArgumentException("ProbabilityTriple must include homeWin, draw, awayWin");
        }
        return ProbabilityTriple.fromProbabilities(dto.homeWin(), dto.draw(), dto.awayWin());
    }

    private double defaultIfNull(Double value, double defaultValue) {
        return (value == null) ? defaultValue : value;
    }

    private int defaultIfNull(Integer value, int defaultValue) {
        return (value == null) ? defaultValue : value;
    }
}