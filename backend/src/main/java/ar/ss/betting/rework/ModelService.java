package ar.ss.betting.rework;

import ar.ss.betting.domain.Round;
import ar.ss.betting.model.EnsembleModel;
import ar.ss.betting.model.ModelInput;
import ar.ss.betting.model.ModelSelectionResult;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * Service responsible for running the model given an API request.
 *
 * Current scope:
 * - Pure in-memory orchestration (no persistence)
 * - Uses the current domain/model source of truth
 */
@Service
public class ModelService {

    private final ModelDtoMapper mapper;

    public ModelService(ModelDtoMapper mapper) {
        this.mapper = Objects.requireNonNull(mapper);
    }

    public ModelSelectionResponseDto runModel(ModelSelectionRequestDto request) {
        Objects.requireNonNull(request, "request cannot be null");

        DomainRun domain = mapper.toDomain(request);

        EnsembleModel model = new EnsembleModel();

        ModelSelectionResult result =
                model.generateSelection(domain.round(), domain.modelInput(), domain.budgetInSek());

        return mapper.toResponseDto(result);
    }

    /**
     * Internal container for mapped domain/model inputs.
     */
    public record DomainRun(
            Round round,
            ModelInput modelInput,
            int budgetInSek
    ) { }
}