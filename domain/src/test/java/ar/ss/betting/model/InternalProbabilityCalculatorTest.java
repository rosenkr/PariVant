package ar.ss.betting.model;

import ar.ss.betting.domain.Outcome;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InternalProbabilityCalculatorTest {

    private static final double EPSILON = 1e-9;

    private final InternalProbabilityCalculator calculator = new InternalProbabilityCalculator();

    @Test
    void shouldReturnMarketProbabilitiesUnchangedWhenNoProvidersOrInterventionsArePresent() {
        MatchContext context = new MatchContext(
                ProbabilityTriple.fromProbabilities(0.50, 0.30, 0.20),
                ProbabilityTriple.fromProbabilities(0.40, 0.35, 0.25),
                List.of()
        );

        ProbabilityTriple result = calculator.calculateInternalProbabilities(context);

        assertEquals(0.50, result.get(Outcome.HOME_WIN), EPSILON);
        assertEquals(0.30, result.get(Outcome.DRAW), EPSILON);
        assertEquals(0.20, result.get(Outcome.AWAY_WIN), EPSILON);
    }

    @Test
    void shouldAverageMarketAndMultipleProvidersWithEqualWeights() {
        MatchContext context = new MatchContext(
                ProbabilityTriple.fromProbabilities(0.60, 0.20, 0.20),
                ProbabilityTriple.fromProbabilities(0.40, 0.35, 0.25),
                List.of(
                        ProbabilityTriple.fromProbabilities(0.30, 0.30, 0.40),
                        ProbabilityTriple.fromProbabilities(0.15, 0.25, 0.60)
                )
        );

        ProbabilityTriple result = calculator.calculateInternalProbabilities(context);

        assertEquals((0.60 + 0.30 + 0.15) / 3.0, result.get(Outcome.HOME_WIN), EPSILON);
        assertEquals((0.20 + 0.30 + 0.25) / 3.0, result.get(Outcome.DRAW), EPSILON);
        assertEquals((0.20 + 0.40 + 0.60) / 3.0, result.get(Outcome.AWAY_WIN), EPSILON);
    }

    @Test
    void neutralVenueTagShouldShiftProbabilitiesDeterministically() {
        MatchContext context = new MatchContext(
                ProbabilityTriple.fromProbabilities(0.60, 0.20, 0.20),
                ProbabilityTriple.fromProbabilities(0.60, 0.20, 0.20),
                List.of()
        );

        MatchInterventions interventions = new MatchInterventions(
                List.of(MatchTag.neutralVenue()),
                null
        );

        ProbabilityTriple result = calculator.calculateInternalProbabilities(context, interventions);

        assertEquals(0.57, result.get(Outcome.HOME_WIN), EPSILON);
        assertEquals(0.21, result.get(Outcome.DRAW), EPSILON);
        assertEquals(0.22, result.get(Outcome.AWAY_WIN), EPSILON);
    }

    @Test
    void sideDependentTagsShouldCompose() {
        MatchContext context = new MatchContext(
                ProbabilityTriple.fromProbabilities(0.50, 0.25, 0.25),
                ProbabilityTriple.fromProbabilities(0.50, 0.25, 0.25),
                List.of()
        );

        MatchInterventions interventions = new MatchInterventions(
                List.of(
                        MatchTag.keyAbsence(Side.HOME),
                        MatchTag.shortRest(Side.HOME)
                ),
                null
        );

        ProbabilityTriple result = calculator.calculateInternalProbabilities(context, interventions);

        assertEquals(0.45, result.get(Outcome.HOME_WIN), EPSILON);
        assertEquals(0.27, result.get(Outcome.DRAW), EPSILON);
        assertEquals(0.28, result.get(Outcome.AWAY_WIN), EPSILON);
    }

    @Test
    void buffShouldPushProbabilityTowardSelectedOutcome() {
        MatchContext context = new MatchContext(
                ProbabilityTriple.fromProbabilities(0.40, 0.30, 0.30),
                ProbabilityTriple.fromProbabilities(0.40, 0.30, 0.30),
                List.of()
        );

        MatchInterventions interventions = new MatchInterventions(
                List.of(),
                new MatchBuff(Outcome.AWAY_WIN, 2)
        );

        ProbabilityTriple result = calculator.calculateInternalProbabilities(context, interventions);

        assertEquals(0.39, result.get(Outcome.HOME_WIN), EPSILON);
        assertEquals(0.29, result.get(Outcome.DRAW), EPSILON);
        assertEquals(0.32, result.get(Outcome.AWAY_WIN), EPSILON);
    }

    @Test
    void largeNegativeShiftShouldStillProduceValidNormalizedProbabilities() {
        MatchContext context = new MatchContext(
                ProbabilityTriple.fromProbabilities(0.02, 0.08, 0.90),
                ProbabilityTriple.fromProbabilities(0.02, 0.08, 0.90),
                List.of()
        );

        MatchInterventions interventions = new MatchInterventions(
                List.of(
                        MatchTag.keyAbsence(Side.HOME),
                        MatchTag.shortRest(Side.HOME),
                        MatchTag.incentiveLack(Side.HOME)
                ),
                null
        );

        ProbabilityTriple result = calculator.calculateInternalProbabilities(context, interventions);

        double sum = result.get(Outcome.HOME_WIN)
                + result.get(Outcome.DRAW)
                + result.get(Outcome.AWAY_WIN);

        assertTrue(result.get(Outcome.HOME_WIN) > 0.0);
        assertTrue(result.get(Outcome.DRAW) > 0.0);
        assertTrue(result.get(Outcome.AWAY_WIN) > 0.0);
        assertEquals(1.0, sum, EPSILON);
    }
}