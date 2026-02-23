package ar.ss.betting.domain;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Represents a match between two teams in a GameRound.
 */
public class Match {

    private final int matchNumber;
    private final LocalDateTime startDate;
    private final Team homeTeam;
    private final Team awayTeam;

    public Match(int matchNumber,
                 LocalDateTime startDate,
                 Team homeTeam,
                 Team awayTeam) {

        if (matchNumber <= 0) {
            throw new IllegalArgumentException("Match number must be positive");
        }

        this.matchNumber = matchNumber;
        this.startDate = Objects.requireNonNull(startDate, "Start date cannot be null");
        this.homeTeam = Objects.requireNonNull(homeTeam, "Home team cannot be null");
        this.awayTeam = Objects.requireNonNull(awayTeam, "Away team cannot be null");

        if (homeTeam.equals(awayTeam)) {
            throw new IllegalArgumentException("Home and away team cannot be the same");
        }
    }

    public int getMatchNumber() {
        return matchNumber;
    }

    public LocalDateTime getStartDate() {
        return startDate;
    }

    public Team getHomeTeam() {
        return homeTeam;
    }

    public Team getAwayTeam() {
        return awayTeam;
    }
}

