package ar.ss.betting.model;

import ar.ss.betting.domain.Outcome;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ModelSelectionResultTest {

    @Test
    void shouldExposeConstructorValues() {
        LocalDateTime generatedAt = LocalDateTime.of(2026, 3, 20, 12, 0);

        ModelSelectionResult result = new ModelSelectionResult(
                "EnsembleModel",
                generatedAt,
                Map.of(
                        1, Outcome.HOME_WIN,
                        2, Outcome.DRAW
                ),
                Map.of(
                        1, Set.of(Outcome.HOME_WIN),
                        2, Set.of(Outcome.DRAW, Outcome.AWAY_WIN)
                ),
                Map.of(
                        1, ProbabilityTriple.fromProbabilities(0.60, 0.25, 0.15),
                        2, ProbabilityTriple.fromProbabilities(0.20, 0.30, 0.50)
                ),
                2,
                1,
                0
        );

        assertEquals("EnsembleModel", result.getModelName());
        assertEquals(generatedAt, result.getGeneratedAt());
        assertEquals(Outcome.HOME_WIN, result.getBasePicks().get(1));
        assertEquals(Outcome.DRAW, result.getBasePicks().get(2));
        assertEquals(Set.of(Outcome.HOME_WIN), result.getSelections().get(1));
        assertEquals(Set.of(Outcome.DRAW, Outcome.AWAY_WIN), result.getSelections().get(2));
        assertEquals(0.60, result.getInternalProbabilities().get(1).get(Outcome.HOME_WIN), 1e-9);
        assertEquals(0.50, result.getInternalProbabilities().get(2).get(Outcome.AWAY_WIN), 1e-9);
        assertEquals(2, result.getTotalCostInSek());
        assertEquals(1, result.getHalfGuardsCount());
        assertEquals(0, result.getFullGuardsCount());
    }

    @Test
    void shouldRejectNonPositiveTotalCost() {
        assertThrows(IllegalArgumentException.class, () -> new ModelSelectionResult(
                "EnsembleModel",
                LocalDateTime.now(),
                Map.of(1, Outcome.HOME_WIN),
                Map.of(1, Set.of(Outcome.HOME_WIN)),
                Map.of(),
                0,
                0,
                0
        ));
    }

    @Test
    void shouldRejectNegativeGuardCounts() {
        assertThrows(IllegalArgumentException.class, () -> new ModelSelectionResult(
                "EnsembleModel",
                LocalDateTime.now(),
                Map.of(1, Outcome.HOME_WIN),
                Map.of(1, Set.of(Outcome.HOME_WIN)),
                Map.of(),
                1,
                -1,
                0
        ));

        assertThrows(IllegalArgumentException.class, () -> new ModelSelectionResult(
                "EnsembleModel",
                LocalDateTime.now(),
                Map.of(1, Outcome.HOME_WIN),
                Map.of(1, Set.of(Outcome.HOME_WIN)),
                Map.of(),
                1,
                0,
                -1
        ));
    }

    @Test
    void shouldReturnAnUnmodifiableBasePicksMap() {
        ModelSelectionResult result = new ModelSelectionResult(
                "EnsembleModel",
                LocalDateTime.now(),
                Map.of(1, Outcome.HOME_WIN),
                Map.of(1, Set.of(Outcome.HOME_WIN)),
                Map.of(),
                1,
                0,
                0
        );

        assertThrows(UnsupportedOperationException.class, () ->
                result.getBasePicks().put(2, Outcome.DRAW));
    }

    @Test
    void shouldReturnAnUnmodifiableSelectionsMap() {
        ModelSelectionResult result = new ModelSelectionResult(
                "EnsembleModel",
                LocalDateTime.now(),
                Map.of(1, Outcome.HOME_WIN),
                Map.of(1, Set.of(Outcome.HOME_WIN)),
                Map.of(),
                1,
                0,
                0
        );

        assertThrows(UnsupportedOperationException.class, () ->
                result.getSelections().put(2, Set.of(Outcome.DRAW)));
    }

    @Test
    void shouldReturnAnUnmodifiableInternalProbabilitiesMap() {
        ModelSelectionResult result = new ModelSelectionResult(
                "EnsembleModel",
                LocalDateTime.now(),
                Map.of(1, Outcome.HOME_WIN),
                Map.of(1, Set.of(Outcome.HOME_WIN)),
                Map.of(1, ProbabilityTriple.fromProbabilities(0.60, 0.25, 0.15)),
                1,
                0,
                0
        );

        assertThrows(UnsupportedOperationException.class, () ->
                result.getInternalProbabilities().put(2, ProbabilityTriple.fromProbabilities(0.20, 0.30, 0.50)));
    }
}