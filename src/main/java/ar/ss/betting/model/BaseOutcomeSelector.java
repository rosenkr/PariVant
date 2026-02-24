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
 * - internal probabilities p_i(o): model belief (after truth adjustments)
 * - public probabilities p_p(o): crowd picks (Svenska folket)
 *
 * Value:
 * - v(o) = p_i(o) - p_p(o)
 *
 * Hybrid rule:
 * - baseline = argmax p_i(o)
 * - candidates are outcomes with p_i(o) >= probabilityFloor(gameType)
 * - bestValue = argmax v(o) among candidates
 * - if (v(bestValue) - v(baseline)) >= valueThreshold(gameType) -> choose bestValue
 *   else choose baseline
 */
public class BaseOutcomeSelector {

    public Outcome chooseBaseOutcome(GameType gameType,
                                     ProbabilityTriple internalProbabilities,
                                     ProbabilityTriple publicProbabilities,
                                     DecisionParameters params) {

        Objects.requireNonNull(gameType, "gameType cannot be null");
        Objects.requireNonNull(internalProbabilities, "internalProbabilities cannot be null");
        Objects.requireNonNull(publicProbabilities, "publicProbabilities cannot be null");
        Objects.requireNonNull(params, "params cannot be null");

        double floor = params.probabilityFloor(gameType);
        double tau = params.valueThreshold(gameType);

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
}