package ar.ss.betting.controllers;

import ar.ss.betting.services.dto.ModelSelectionRequestDto;
import ar.ss.betting.services.dto.ModelSelectionResponseDto;
import ar.ss.betting.services.ModelService;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<ModelSelectionResponseDto> generateSelection(@RequestBody ModelSelectionRequestDto request) {
        return ResponseEntity.ok(modelService.runModel(request));
    }
}