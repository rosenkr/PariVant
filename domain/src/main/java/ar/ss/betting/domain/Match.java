package ar.ss.betting.domain;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Represents one betting match in a round.
 */
public class Match {

    private final int matchNumber;
    private final LocalDateTime startDate;
    private final Team homeTeam;
    private final Team awayTeam;
    private final int homeScore;
    private final int awayScore;
    private final MatchStatus status;

    public Match(int matchNumber,
                 LocalDateTime startDate,
                 Team homeTeam,
                 Team awayTeam) {
        this(matchNumber, startDate, homeTeam, awayTeam, 0, 0, MatchStatus.UPCOMING);
    }

    public Match(int matchNumber,
                 LocalDateTime startDate,
                 Team homeTeam,
                 Team awayTeam,
                 int homeScore,
                 int awayScore,
                 MatchStatus status) {

        if (matchNumber <= 0) {
            throw new IllegalArgumentException("Match number must be positive");
        }
        if (homeScore < 0) {
            throw new IllegalArgumentException("Home score cannot be negative");
        }
        if (awayScore < 0) {
            throw new IllegalArgumentException("Away score cannot be negative");
        }

        this.matchNumber = matchNumber;
        this.startDate = Objects.requireNonNull(startDate, "Start date cannot be null");
        this.homeTeam = Objects.requireNonNull(homeTeam, "Home team cannot be null");
        this.awayTeam = Objects.requireNonNull(awayTeam, "Away team cannot be null");
        this.status = Objects.requireNonNull(status, "Status cannot be null");
        this.homeScore = homeScore;
        this.awayScore = awayScore;

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

    public int getHomeScore() {
        return homeScore;
    }

    public int getAwayScore() {
        return awayScore;
    }

    public MatchStatus getStatus() {
        return status;
    }
}