package ar.ss.betting.services;

import ar.ss.betting.domain.Round;
import ar.ss.betting.model.EnsembleModel;
import ar.ss.betting.model.ModelInput;
import ar.ss.betting.model.ModelSelectionResult;
import ar.ss.betting.security.UserEntity;
import ar.ss.betting.services.dto.ModelSelectionRequest;
import ar.ss.betting.services.dto.ModelSelectionResponse;
import ar.ss.betting.services.dto.ModelDtoMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Instant;
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
    private final Clock clock;

    public ModelService(ModelDtoMapper mapper, Clock clock) {
        this.mapper = Objects.requireNonNull(mapper);
        this.clock = Objects.requireNonNull(clock);
    }

    public ModelSelectionResponse runModel(UserEntity user, ModelSelectionRequest request) {
        requireAuthenticatedUser(user);
        Objects.requireNonNull(request, "request cannot be null");

        DomainRun domain = mapper.toDomain(request);

        EnsembleModel model = new EnsembleModel();

        Instant generatedAt = Instant.now(clock);
        ModelSelectionResult result =
                model.generateSelection(domain.round(), domain.modelInput(), domain.budgetInSek(), generatedAt);

        return mapper.toResponseDto(result);
    }

    private void requireAuthenticatedUser(UserEntity user) {
        if (user == null || user.getId() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
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
