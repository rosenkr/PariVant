package ar.ss.betting.model;

import ar.ss.betting.domain.Outcome;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ProbabilityTripleTest {

    private static final double EPS = 1e-9;

    @Test
    void fromDecimalOddsShouldNormalizeAndPreserveOrdering() {
        // Example odds (decimal)
        ProbabilityTriple triple = ProbabilityTriple.fromDecimalOdds(1.55, 4.50, 6.25);

        double h = triple.get(Outcome.HOME_WIN);
        double d = triple.get(Outcome.DRAW);
        double a = triple.get(Outcome.AWAY_WIN);

        // Should be normalized
        assertEquals(1.0, h + d + a, EPS);

        // HOME_WIN should be largest for these odds
        assertTrue(h > d, "Expected HOME_WIN prob > DRAW prob");
        assertTrue(h > a, "Expected HOME_WIN prob > AWAY_WIN prob");

        // With 4.50 vs 6.25, draw should be more likely than away win
        assertTrue(d > a, "Expected DRAW prob > AWAY_WIN prob");

        assertEquals(Outcome.HOME_WIN, triple.argMax());
    }

    @Test
    void fromProbabilitiesShouldNormalizeWhenSumIsGreaterThanOne() {
        // "Probability-like" numbers with overround: sum = 1.027
        ProbabilityTriple triple = ProbabilityTriple.fromProbabilities(0.645, 0.222, 0.160);

        double h = triple.get(Outcome.HOME_WIN);
        double d = triple.get(Outcome.DRAW);
        double a = triple.get(Outcome.AWAY_WIN);

        // Normalized sum
        assertEquals(1.0, h + d + a, EPS);

        // Should preserve ratios: normalized value = raw / sum
        double sum = 0.645 + 0.222 + 0.160;
        assertEquals(0.645 / sum, h, EPS);
        assertEquals(0.222 / sum, d, EPS);
        assertEquals(0.160 / sum, a, EPS);

        assertEquals(Outcome.HOME_WIN, triple.argMax());
    }

    @Test
    void fromProbabilitiesShouldThrowIfSumIsZeroOrNearZero() {
        assertThrows(IllegalArgumentException.class,
                () -> ProbabilityTriple.fromProbabilities(0.0, 0.0, 0.0));

        assertThrows(IllegalArgumentException.class,
                () -> ProbabilityTriple.fromProbabilities(1e-20, 0.0, 0.0));
    }

    @Test
    void fromProbabilitiesShouldThrowOnNegativeOrNonFiniteInputs() {
        assertThrows(IllegalArgumentException.class,
                () -> ProbabilityTriple.fromProbabilities(-0.1, 0.5, 0.6));

        assertThrows(IllegalArgumentException.class,
                () -> ProbabilityTriple.fromProbabilities(Double.NaN, 0.5, 0.5));

        assertThrows(IllegalArgumentException.class,
                () -> ProbabilityTriple.fromProbabilities(Double.POSITIVE_INFINITY, 0.5, 0.5));
    }

    @Test
    void fromDecimalOddsShouldThrowOnNonPositiveOrNonFiniteOdds() {
        assertThrows(IllegalArgumentException.class,
                () -> ProbabilityTriple.fromDecimalOdds(0.0, 4.5, 6.25));

        assertThrows(IllegalArgumentException.class,
                () -> ProbabilityTriple.fromDecimalOdds(-1.0, 4.5, 6.25));

        assertThrows(IllegalArgumentException.class,
                () -> ProbabilityTriple.fromDecimalOdds(Double.NaN, 4.5, 6.25));

        assertThrows(IllegalArgumentException.class,
                () -> ProbabilityTriple.fromDecimalOdds(Double.POSITIVE_INFINITY, 4.5, 6.25));
    }
}