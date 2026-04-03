package ar.ss.betting.predictionproviders.domain;

import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public final class ProviderProbabilityTriple {

    private final double homeWin;
    private final double draw;
    private final double awayWin;

    private ProviderProbabilityTriple(double homeWin, double draw, double awayWin) {
        this.homeWin = homeWin;
        this.draw = draw;
        this.awayWin = awayWin;
    }

    public static ProviderProbabilityTriple of(double homeWin, double draw, double awayWin) {
        validateNonNegative(homeWin, draw, awayWin);

        double sum = homeWin + draw + awayWin;
        if (sum <= 0.0) {
            throw new IllegalArgumentException("Probability triple sum must be > 0");
        }

        return new ProviderProbabilityTriple(
                homeWin / sum,
                draw / sum,
                awayWin / sum
        );
    }

    private static void validateNonNegative(double homeWin, double draw, double awayWin) {
        if (homeWin < 0 || draw < 0 || awayWin < 0) {
            throw new IllegalArgumentException("Probabilities must be non-negative");
        }
    }
}