package ar.ss.betting.persistence.entity;

import ar.ss.betting.domain.MatchStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(
        name = "match",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_match_round_matchnumber",
                columnNames = {"round_id", "match_number"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MatchEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JoinColumn(name = "round_id", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private RoundEntity round;

    @Column(name = "match_number", nullable = false)
    private int matchNumber;

    @Column(name = "start_date", nullable = false)
    private LocalDateTime startDate;

    @Column(name = "home_team_name", nullable = false, length = 80)
    private String homeTeamName;

    @Column(name = "away_team_name", nullable = false, length = 80)
    private String awayTeamName;

    @Column(name = "home_score", nullable = false)
    private int homeScore;

    @Column(name = "away_score", nullable = false)
    private int awayScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private MatchStatus status;

    public MatchEntity(int matchNumber,
                       LocalDateTime startDate,
                       String homeTeamName,
                       String awayTeamName) {
        this(matchNumber, startDate, homeTeamName, awayTeamName, 0, 0, MatchStatus.UPCOMING);
    }

    public MatchEntity(int matchNumber,
                       LocalDateTime startDate,
                       String homeTeamName,
                       String awayTeamName,
                       int homeScore,
                       int awayScore,
                       MatchStatus status) {
        if (matchNumber <= 0) {
            throw new IllegalArgumentException("matchNumber must be positive");
        }
        if (homeScore < 0) {
            throw new IllegalArgumentException("homeScore cannot be negative");
        }
        if (awayScore < 0) {
            throw new IllegalArgumentException("awayScore cannot be negative");
        }

        this.matchNumber = matchNumber;
        this.startDate = Objects.requireNonNull(startDate, "startDate cannot be null");
        this.homeTeamName = requireNonBlank(homeTeamName, "homeTeamName");
        this.awayTeamName = requireNonBlank(awayTeamName, "awayTeamName");
        this.homeScore = homeScore;
        this.awayScore = awayScore;
        this.status = Objects.requireNonNull(status, "status cannot be null");
    }

    private static String requireNonBlank(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " cannot be blank");
        }
        return value;
    }

    void setRound(RoundEntity round) {
        this.round = round;
    }

    public void setStatus(MatchStatus status) {
        this.status = Objects.requireNonNull(status, "status cannot be null");
    }

    public void setHomeScore(int homeScore) {
        if (homeScore < 0) {
            throw new IllegalArgumentException("homeScore cannot be negative");
        }
        this.homeScore = homeScore;
    }

    public void setAwayScore(int awayScore) {
        if (awayScore < 0) {
            throw new IllegalArgumentException("awayScore cannot be negative");
        }
        this.awayScore = awayScore;
    }
}