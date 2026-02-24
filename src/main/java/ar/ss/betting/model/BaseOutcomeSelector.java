package ar.ss.betting.model;

import ar.ss.betting.domain.GameType;
import ar.ss.betting.domain.Outcome;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Chooses a single base outcome for a match using a hybrid "value vs safety" rule.
 *
 * Definitions:
 * - market probability p_m(o)
 * - public probability p_p(o)
 * - value v(o) = p_i(o) - p_p(o)
 *   For Step C.1: p_i(o) := p_m(o) (market as proxy for internal probability)
 *
 * Hybrid rule:
 * - baseline = argmax p_m(o)
 * - candidates are outcomes with p_m(o) >= probabilityFloor
 * - bestValue = argmax v(o) among candidates
 * - if (v(bestValue) - v(baseline)) >= valueThreshold -> choose bestValue
 *   else choose baseline
 *
 * Later: p_i(o) will include adjustments (form, injuries, weather...) with user weights.
 */
public class BaseOutcomeSelector {

    public Outcome chooseBaseOutcome(GameType gameType, MatchContext context) {
        Objects.requireNonNull(gameType, "gameType cannot be null");
        Objects.requireNonNull(context, "context cannot be null");

        ProbabilityTriple market = context.getMarketProbabilities();
        ProbabilityTriple pub = context.getPublicProbabilities();

        double floor = probabilityFloor(gameType);
        double tau = valueThreshold(gameType);

        Outcome baseline = market.argMax();

        // Consider only outcomes above floor when picking "value" candidates
        List<Outcome> candidates = List.of(Outcome.HOME_WIN, Outcome.DRAW, Outcome.AWAY_WIN).stream()
                .filter(o -> market.get(o) >= floor)
                .toList();

        // If floor filters everything (unlikely), just fall back to baseline
        if (candidates.isEmpty()) {
            return baseline;
        }

        Outcome bestValue = candidates.stream()
                .max(Comparator.comparingDouble(o -> value(market, pub, o)))
                .orElse(baseline);

        double vBaseline = value(market, pub, baseline);
        double vBest = value(market, pub, bestValue);

        if ((vBest - vBaseline) >= tau) {
            return bestValue;
        }

        return baseline;
    }

    private double value(ProbabilityTriple market, ProbabilityTriple pub, Outcome outcome) {
        // Step C.1: internal probability = market probability
        return market.get(outcome) - pub.get(outcome);
    }

    private double probabilityFloor(GameType gameType) {
        // Defaults (tunable)
        // Topptipset: allow a bit more risk
        if (gameType == GameType.TOPPTIPSET) {
            return 0.12;
        }
        // Stryktipset & Europatipset: safer
        return 0.18;
    }

    private double valueThreshold(GameType gameType) {
        // Defaults (tunable)
        // Topptipset: lower threshold = more willingness to take value
        if (gameType == GameType.TOPPTIPSET) {
            return 0.02;
        }
        // Stryktipset & Europatipset: require stronger reason to deviate
        return 0.04;
    }
}