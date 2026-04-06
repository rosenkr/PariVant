package ar.ss.betting.roundingest;

import java.time.OffsetDateTime;
import java.util.Objects;

public record IngestedMatch(
        int matchNumber,
        OffsetDateTime kickoff,
        String homeTeamName,
        String awayTeamName,
        ProbabilityTriple market,
        ProbabilityTriple publicPick,
        boolean marketFallbackUsed,
        String marketFallbackReason
) {
    public IngestedMatch {
        if (matchNumber <= 0) {
            throw new IllegalArgumentException("matchNumber must be positive");
        }
        Objects.requireNonNull(kickoff, "kickoff");
        Objects.requireNonNull(homeTeamName, "homeTeamName");
        Objects.requireNonNull(awayTeamName, "awayTeamName");
        Objects.requireNonNull(market, "market");
        Objects.requireNonNull(publicPick, "publicPick");

        if (homeTeamName.isBlank()) {
            throw new IllegalArgumentException("homeTeamName cannot be blank");
        }
        if (awayTeamName.isBlank()) {
            throw new IllegalArgumentException("awayTeamName cannot be blank");
        }

        if (!marketFallbackUsed && marketFallbackReason != null && !marketFallbackReason.isBlank()) {
            throw new IllegalArgumentException("marketFallbackReason must be null/blank when fallback is not used");
        }
    }

    public IngestedMatch(
            int matchNumber,
            OffsetDateTime kickoff,
            String homeTeamName,
            String awayTeamName,
            ProbabilityTriple market,
            ProbabilityTriple publicPick
    ) {
        this(matchNumber, kickoff, homeTeamName, awayTeamName, market, publicPick, false, null);
    }

    public record ProbabilityTriple(
            double homeWin,
            double draw,
            double awayWin
    ) {
        public ProbabilityTriple {
            validateFiniteNonNegative(homeWin, "homeWin");
            validateFiniteNonNegative(draw, "draw");
            validateFiniteNonNegative(awayWin, "awayWin");

            double sum = homeWin + draw + awayWin;
            if (sum <= 1e-12) {
                throw new IllegalArgumentException("probability triple sum must be > 0");
            }
        }

        public static ProbabilityTriple normalized(double homeWin, double draw, double awayWin) {
            validateFiniteNonNegative(homeWin, "homeWin");
            validateFiniteNonNegative(draw, "draw");
            validateFiniteNonNegative(awayWin, "awayWin");

            double sum = homeWin + draw + awayWin;
            if (sum <= 1e-12) {
                throw new IllegalArgumentException("probability triple sum must be > 0");
            }

            return new ProbabilityTriple(
                    homeWin / sum,
                    draw / sum,
                    awayWin / sum
            );
        }

        private static void validateFiniteNonNegative(double value, String label) {
            if (!Double.isFinite(value)) {
                throw new IllegalArgumentException(label + " must be finite");
            }
            if (value < 0.0) {
                throw new IllegalArgumentException(label + " must be non-negative");
            }
        }
    }
}