package ar.ss.betting.controllers.authenticated;

import ar.ss.betting.security.UserEntity;
import ar.ss.betting.services.dto.ModelSelectionRequest;
import ar.ss.betting.services.dto.ModelSelectionResponse;
import ar.ss.betting.services.ModelService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;

/**
 * REST API for running the betting model.
 *
 * Current scope:
 * - POST /model/selection : run model in-memory and return result
 */
@RestController
@RequestMapping("/model")
public class ModelController {

    private final ModelService modelService;

    public ModelController(ModelService modelService) {
        this.modelService = Objects.requireNonNull(modelService);
    }

    @PostMapping("/selection")
    public ResponseEntity<ModelSelectionResponse> generateSelection(
            @AuthenticationPrincipal UserEntity user,
            @RequestBody ModelSelectionRequest request
    ) {
        return ResponseEntity.ok(modelService.runModel(user, request));
    }
}
