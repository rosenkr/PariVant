package ar.ss.betting.service;

import ar.ss.betting.api.dto.ModelSelectionRequestDto;
import ar.ss.betting.api.dto.ModelSelectionResponseDto;
import ar.ss.betting.model.*;

import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * Service responsible for running the model given an API request.
 *
 * For now:
 * - Pure in-memory orchestration (no persistence).
 * - Builds domain objects + model input, runs RuleBasedModel, maps result back to DTO.
 */
@Service
public class ModelService {

    private final ModelDtoMapper mapper;

    public ModelService(ModelDtoMapper mapper) {
        this.mapper = Objects.requireNonNull(mapper);
    }

    public ModelSelectionResponseDto runModel(ModelSelectionRequestDto request) {
        Objects.requireNonNull(request, "request cannot be null");

        var domain = mapper.toDomain(request);

        RuleBasedModel model = new RuleBasedModel(domain.weights(), domain.decisionParameters());

        ModelSelectionResult result =
                model.generateSelection(domain.gameRound(), domain.modelInput(), domain.budgetInSek());

        return mapper.toResponseDto(result);
    }

    /**
     * Internal container for the mapped domain/model inputs.
     */
    public record DomainRun(
            ar.ss.betting.domain.GameRound gameRound,
            ModelInput modelInput,
            int budgetInSek,
            AdjustmentWeights weights,
            DecisionParameters decisionParameters
    ) { }
}