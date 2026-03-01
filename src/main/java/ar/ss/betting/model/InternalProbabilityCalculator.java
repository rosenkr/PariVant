package ar.ss.betting.model;

import ar.ss.betting.domain.Outcome;

import java.util.EnumMap;

/**
 * TruthEngine (v1).
 *
 * Responsible for adjusting market probabilities using weighted signals.
 *
 * Current signals:
 * - Recent form (last 5 matches)
 *
 * Philosophy:
 * - Start from market probabilities.
 * - Apply adjustments based on normalized signal values.
 * - Renormalize to ensure probabilities sum to 1.
 *
 * IMPORTANT:
 * - This class does NOT consider public distribution.
 * - Public distribution belongs to the decision layer (value logic).
 */
public class InternalProbabilityCalculator {

    public ProbabilityTriple calculateInternalProbabilities(MatchContext ctx,
                                                            AdjustmentWeights weights) {

        ProbabilityTriple market = ctx.getMarketProbabilities();

        // Start from market baseline
        double home = market.get(Outcome.HOME_WIN);
        double draw = market.get(Outcome.DRAW);
        double away = market.get(Outcome.AWAY_WIN);

        // ---- Apply recent form adjustment ----
        double formWeight = weights.getRecentFormWeight();

        if (formWeight > 0.0) {

            int homeForm = ctx.getHomeRecentFormScore();
            int awayForm = ctx.getAwayRecentFormScore();

            // Difference range: [-10, +10]
            int diff = homeForm - awayForm;

            // Normalize to [-1.0, +1.0]
            double normalized = diff / 10.0;

            // Linear shift
            double delta = formWeight * normalized;

            // Adjust win probabilities only
            home += delta;
            away -= delta;
        }

        return renormalize(home, draw, away);
    }

    private ProbabilityTriple renormalize(double home, double draw, double away) {

        // Prevent negative values (can happen with strong adjustments)
        home = Math.max(0.0001, home);
        draw = Math.max(0.0001, draw);
        away = Math.max(0.0001, away);

        double sum = home + draw + away;

        home /= sum;
        draw /= sum;
        away /= sum;

        return ProbabilityTriple.fromProbabilities(home, draw, away);
    }
}