package ar.ss.betting.model;

import ar.ss.betting.domain.Outcome;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BaseOutcomeSelectorTest {

    private static final double EPSILON = 1e-9;

    private final BaseOutcomeSelector selector = new BaseOutcomeSelector();

    @Test
    void shouldChooseOutcomeWithHighestScore() {
        ProbabilityTriple internal = ProbabilityTriple.fromProbabilities(0.55, 0.25, 0.20);
        ProbabilityTriple pub = ProbabilityTriple.fromProbabilities(0.80, 0.10, 0.10);

        Outcome result = selector.chooseBaseOutcome(internal, pub);

        assertEquals(Outcome.DRAW, result);
    }

    @Test
    void shouldChooseHighestKlContributionNotHighestRawValueGap() {
        ProbabilityTriple internal = ProbabilityTriple.fromProbabilities(0.55, 0.30, 0.15);
        ProbabilityTriple pub = ProbabilityTriple.fromProbabilities(0.82, 0.17, 0.01);

        double drawValue = internal.get(Outcome.DRAW) - pub.get(Outcome.DRAW);
        double awayValue = internal.get(Outcome.AWAY_WIN) - pub.get(Outcome.AWAY_WIN);

        assertEquals(0.13, drawValue, EPSILON);
        assertEquals(0.14, awayValue, EPSILON);

        double drawScore = BaseOutcomeSelector.score(
                internal.get(Outcome.DRAW),
                pub.get(Outcome.DRAW)
        );
        double awayScore = BaseOutcomeSelector.score(
                internal.get(Outcome.AWAY_WIN),
                pub.get(Outcome.AWAY_WIN)
        );

        assertTrue(awayScore > drawScore);

        Outcome result = selector.chooseBaseOutcome(internal, pub);

        assertEquals(Outcome.AWAY_WIN, result);
    }

    @Test
    void shouldChooseHighestScoreNotHighestInternalProbability() {
        ProbabilityTriple internal = ProbabilityTriple.fromProbabilities(0.60, 0.25, 0.15);
        ProbabilityTriple pub = ProbabilityTriple.fromProbabilities(0.90, 0.05, 0.05);

        Outcome result = selector.chooseBaseOutcome(internal, pub);

        assertEquals(Outcome.DRAW, result);
    }

    @Test
    void shouldUseDeterministicTieBreakWhenScoresAreEqual() {
        ProbabilityTriple internal = ProbabilityTriple.fromProbabilities(0.34, 0.33, 0.33);
        ProbabilityTriple pub = ProbabilityTriple.fromProbabilities(0.34, 0.33, 0.33);

        Outcome result = selector.chooseBaseOutcome(internal, pub);

        assertEquals(Outcome.HOME_WIN, result);
    }

    @Test
    void scoreShouldMatchKlContributionFormula() {
        double score = BaseOutcomeSelector.score(0.30, 0.17);
        double expected = 0.30 * Math.log(0.30 / 0.17);

        assertEquals(expected, score, EPSILON);
    }
}