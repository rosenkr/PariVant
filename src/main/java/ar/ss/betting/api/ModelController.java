package ar.ss.betting.api;

import ar.ss.betting.api.dto.ModelSelectionRequestDto;
import ar.ss.betting.api.dto.ModelSelectionResponseDto;
import ar.ss.betting.service.ModelService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;

/**
 * REST API for running the betting model.
 *
 * B2 scope:
 * - POST /model/selection : run model in-memory and return result.
 *
 * Persistence will come in step C.
 */
@RestController
@RequestMapping("/model")
public class ModelController {

    private final ModelService modelService;

    public ModelController(ModelService modelService) {
        this.modelService = Objects.requireNonNull(modelService);
    }

    @PostMapping("/selection")
    public ResponseEntity<ModelSelectionResponseDto> generateSelection(@RequestBody ModelSelectionRequestDto request) {
        return ResponseEntity.ok(modelService.runModel(request));
    }
}
