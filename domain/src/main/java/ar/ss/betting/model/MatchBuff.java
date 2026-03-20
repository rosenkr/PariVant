package ar.ss.betting.model;

import ar.ss.betting.domain.Outcome;

import java.util.Objects;

public class MatchBuff {

    private final Outcome targetOutcome;
    private final int points;

    public MatchBuff(Outcome targetOutcome, int points) {
        this.targetOutcome = Objects.requireNonNull(targetOutcome, "targetOutcome cannot be null");

        if (points <= 0) {
            throw new IllegalArgumentException("Buff points must be positive");
        }
        if (points > ModelConstants.MAX_BUFF_POINTS_PER_MATCH) {
            throw new IllegalArgumentException("Buff points exceed per-match cap");
        }

        this.points = points;
    }

    public Outcome getTargetOutcome() {
        return targetOutcome;
    }

    public int getPoints() {
        return points;
    }
}
