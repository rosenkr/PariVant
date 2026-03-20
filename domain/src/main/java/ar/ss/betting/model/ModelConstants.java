package ar.ss.betting.model;
import ar.ss.betting.domain.Outcome;

public final class ModelConstants {

    private ModelConstants() {
    }

    public static final double BASE_PICK_AGGRESSIVENESS_K = 3.0;

    /**
     * Used when applying tag/buff shifts so that all three outcomes remain positive
     * before final normalization.
     */
    public static final double MIN_PROBABILITY_COMPONENT = 1e-6;

    /**
     * Buff quota:
     * total available buff points in a round = roundSize * BUFF_POINTS_PER_MATCH_IN_ROUND
     */
    public static final int BUFF_POINTS_PER_MATCH_IN_ROUND = 5;

    /**
     * Explicit per-match cap.
     */
    public static final int MAX_BUFF_POINTS_PER_MATCH = 5;

    /**
     * One buff point shifts 1 percentage point toward the selected outcome.
     */
    public static final double BUFF_SHIFT_PER_POINT = 0.01;

    public static ProbabilityShift neutralVenueShift() {
        return new ProbabilityShift(-0.03, 0.01, 0.02);
    }

    public static ProbabilityShift keyAbsenceShift(Side affectedSide) {
        return shiftAgainstAffectedSide(affectedSide, 0.03, 0.01, 0.02);
    }

    public static ProbabilityShift shortRestShift(Side affectedSide) {
        return shiftAgainstAffectedSide(affectedSide, 0.02, 0.01, 0.01);
    }

    public static ProbabilityShift incentiveLackShift(Side affectedSide) {
        return shiftAgainstAffectedSide(affectedSide, 0.03, 0.02, 0.01);
    }

    public static ProbabilityShift buffShift(Outcome targetOutcome, int points) {
        if (points <= 0) {
            throw new IllegalArgumentException("Buff points must be positive");
        }
        if (points > MAX_BUFF_POINTS_PER_MATCH) {
            throw new IllegalArgumentException("Buff points exceed per-match cap");
        }

        double toward = points * BUFF_SHIFT_PER_POINT;
        double awayEach = toward / 2.0;

        return switch (targetOutcome) {
            case HOME_WIN -> new ProbabilityShift(toward, -awayEach, -awayEach);
            case DRAW -> new ProbabilityShift(-awayEach, toward, -awayEach);
            case AWAY_WIN -> new ProbabilityShift(-awayEach, -awayEach, toward);
        };
    }

    private static ProbabilityShift shiftAgainstAffectedSide(Side affectedSide,
                                                             double affectedWinLoss,
                                                             double drawGain,
                                                             double opponentGain) {
        return switch (affectedSide) {
            case HOME -> new ProbabilityShift(-affectedWinLoss, drawGain, opponentGain);
            case AWAY -> new ProbabilityShift(opponentGain, drawGain, -affectedWinLoss);
        };
    }
}