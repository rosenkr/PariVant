package ar.ss.betting.model;

import ar.ss.betting.domain.Outcome;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * Holds probabilities for HOME_WIN / DRAW / AWAY_WIN.
 *
 * IMPORTANT:
 * - This class represents PROBABILITIES, not odds.
 * - Use factories:
 *     - fromProbabilities(...) when your input already represents probabilities (may include overround)
 *     - fromDecimalOdds(...) when your input is decimal odds (e.g., 1.55, 4.50, 6.25)
 *
 * We normalize inputs so the stored values always sum to 1.0 (within rounding tolerance).
 */
public final class ProbabilityTriple {

    private static final double MIN_SUM = 1e-12;

    private final EnumMap<Outcome, Double> probs;

    private ProbabilityTriple(double homeWin, double draw, double awayWin) {
        this.probs = new EnumMap<>(Outcome.class);
        probs.put(Outcome.HOME_WIN, homeWin);
        probs.put(Outcome.DRAW, draw);
        probs.put(Outcome.AWAY_WIN, awayWin);
    }

    /**
     * Creates a ProbabilityTriple from probability-like numbers.
     *
     * Accepts inputs that do NOT necessarily sum to 1 due to bookmaker margin (overround),
     * e.g. 0.645 + 0.222 + 0.160 = 1.027.
     *
     * We normalize by dividing each value by the sum.
     */
    public static ProbabilityTriple fromProbabilities(double homeWin, double draw, double awayWin) {
        validateFiniteNonNegative(homeWin, Outcome.HOME_WIN);
        validateFiniteNonNegative(draw, Outcome.DRAW);
        validateFiniteNonNegative(awayWin, Outcome.AWAY_WIN);

        double sum = homeWin + draw + awayWin;
        if (sum < MIN_SUM) {
            throw new IllegalArgumentException("Probabilities must have positive sum, got: " + sum);
        }

        double nh = homeWin / sum;
        double nd = draw / sum;
        double na = awayWin / sum;

        // Tiny rounding guard: force into [0,1]
        nh = clamp01(nh);
        nd = clamp01(nd);
        na = clamp01(na);

        // Re-normalize again if clamp changed sum slightly
        double sum2 = nh + nd + na;
        if (sum2 < MIN_SUM) {
            throw new IllegalArgumentException("Probabilities became invalid after normalization");
        }
        nh /= sum2;
        nd /= sum2;
        na /= sum2;

        return new ProbabilityTriple(nh, nd, na);
    }

    /**
     * Creates a ProbabilityTriple from DECIMAL odds (e.g. 1.55, 4.50, 6.25).
     *
     * We convert odds -> implied probabilities using 1/odds, then normalize to sum 1.
     * This automatically handles bookmaker margin (overround).
     */
    public static ProbabilityTriple fromDecimalOdds(double homeWinOdds, double drawOdds, double awayWinOdds) {
        validateFinitePositive(homeWinOdds, "HOME_WIN odds");
        validateFinitePositive(drawOdds, "DRAW odds");
        validateFinitePositive(awayWinOdds, "AWAY_WIN odds");

        double ih = 1.0 / homeWinOdds;
        double id = 1.0 / drawOdds;
        double ia = 1.0 / awayWinOdds;

        return fromProbabilities(ih, id, ia);
    }

    private static void validateFiniteNonNegative(double v, Outcome outcome) {
        if (!Double.isFinite(v)) {
            throw new IllegalArgumentException("Non-finite probability for " + outcome + ": " + v);
        }
        if (v < 0.0) {
            throw new IllegalArgumentException("Negative probability for " + outcome + ": " + v);
        }
        // NOTE: we intentionally do NOT require v <= 1.0 here,
        // because callers might pass probability-like numbers that sum > 1 due to overround
        // (e.g., 0.645 + 0.222 + 0.160 = 1.027). We normalize those.
    }

    private static void validateFinitePositive(double v, String label) {
        if (!Double.isFinite(v)) {
            throw new IllegalArgumentException("Non-finite value for " + label + ": " + v);
        }
        if (v <= 0.0) {
            throw new IllegalArgumentException("Value must be > 0 for " + label + ": " + v);
        }
    }

    private static double clamp01(double v) {
        if (v < 0.0) return 0.0;
        if (v > 1.0) return 1.0;
        return v;
    }

    public double get(Outcome outcome) {
        Objects.requireNonNull(outcome, "outcome cannot be null");
        return probs.get(outcome);
    }

    public Outcome argMax() {
        Outcome best = null;
        double bestV = Double.NEGATIVE_INFINITY;

        for (Map.Entry<Outcome, Double> e : probs.entrySet()) {
            if (e.getValue() > bestV) {
                bestV = e.getValue();
                best = e.getKey();
            }
        }
        return best;
    }

    public EnumMap<Outcome, Double> asMap() {
        return new EnumMap<>(probs);
    }

    @Override
    public String toString() {
        return "ProbabilityTriple" + probs;
    }
}