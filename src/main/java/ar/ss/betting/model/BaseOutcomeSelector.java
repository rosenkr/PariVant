package ar.ss.betting.model;

import ar.ss.betting.domain.GameType;
import ar.ss.betting.domain.Outcome;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * DecisionEngine: chooses a single base outcome for a match.
 *
 * Inputs:
 * - internal probabilities p_i(o): what the model believes is true probability (after truth adjustments)
 * - public probabilities p_p(o): how the crowd picks (Svenska folket)
 *
 * Value:
 * - v(o) = p_i(o) - p_p(o)
 *
 * Hybrid rule:
 * - baseline = argmax p_i(o)
 * - candidates are outcomes with p_i(o) >= probabilityFloor
 * - bestValue = argmax v(o) among candidates
 * - if (v(bestValue) - v(baseline)) >= valueThreshold -> choose bestValue
 *   else choose baseline
 *
 * Note: public distribution is not a truth-signal; it belongs here in the decision layer.
 */
public class BaseOutcomeSelector {

    public Outcome chooseBaseOutcome(GameType gameType,
                                     ProbabilityTriple internalProbabilities,
                                     ProbabilityTriple publicProbabilities) {

        Objects.requireNonNull(gameType, "gameType cannot be null");
        Objects.requireNonNull(internalProbabilities, "internalProbabilities cannot be null");
        Objects.requireNonNull(publicProbabilities, "publicProbabilities cannot be null");

        double floor = probabilityFloor(gameType);
        double tau = valueThreshold(gameType);

        Outcome baseline = internalProbabilities.argMax();

        List<Outcome> candidates = List.of(Outcome.HOME_WIN, Outcome.DRAW, Outcome.AWAY_WIN).stream()
                .filter(o -> internalProbabilities.get(o) >= floor)
                .toList();

        if (candidates.isEmpty()) {
            return baseline;
        }

        Outcome bestValue = candidates.stream()
                .max(Comparator.comparingDouble(o -> value(internalProbabilities, publicProbabilities, o)))
                .orElse(baseline);

        double vBaseline = value(internalProbabilities, publicProbabilities, baseline);
        double vBest = value(internalProbabilities, publicProbabilities, bestValue);

        if ((vBest - vBaseline) >= tau) {
            return bestValue;
        }

        return baseline;
    }

    private double value(ProbabilityTriple internal, ProbabilityTriple pub, Outcome outcome) {
        return internal.get(outcome) - pub.get(outcome);
    }

    private double probabilityFloor(GameType gameType) {
        // Defaults (tunable)
        if (gameType == GameType.TOPPTIPSET) {
            return 0.12;
        }
        return 0.18;
    }

    private double valueThreshold(GameType gameType) {
        // Defaults (tunable)
        if (gameType == GameType.TOPPTIPSET) {
            return 0.02;
        }
        return 0.04;
    }
}