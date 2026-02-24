package ar.ss.betting.persistence.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(
        name = "match",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_match_round_matchnumber",
                columnNames = {"game_round_id", "match_number"}
        )
)
public class MatchEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "game_round_id", nullable = false)
    private GameRoundEntity gameRound;

    @Column(name = "match_number", nullable = false)
    private int matchNumber;

    @Column(name = "start_date", nullable = false)
    private LocalDateTime startDate;

    @Column(name = "home_team_name", nullable = false, length = 80)
    private String homeTeamName;

    @Column(name = "away_team_name", nullable = false, length = 80)
    private String awayTeamName;

    protected MatchEntity() {
        // JPA
    }

    public MatchEntity(int matchNumber,
                       LocalDateTime startDate,
                       String homeTeamName,
                       String awayTeamName) {

        if (matchNumber <= 0) {
            throw new IllegalArgumentException("matchNumber must be positive");
        }

        this.matchNumber = matchNumber;
        this.startDate = Objects.requireNonNull(startDate, "startDate cannot be null");
        this.homeTeamName = requireNonBlank(homeTeamName, "homeTeamName");
        this.awayTeamName = requireNonBlank(awayTeamName, "awayTeamName");
    }

    private String requireNonBlank(String s, String name) {
        Objects.requireNonNull(s, name + " cannot be null");
        if (s.isBlank()) {
            throw new IllegalArgumentException(name + " cannot be blank");
        }
        return s;
    }

    void setGameRound(GameRoundEntity gameRound) {
        this.gameRound = gameRound;
    }

    public Long getId() {
        return id;
    }

    public GameRoundEntity getGameRound() {
        return gameRound;
    }

    public int getMatchNumber() {
        return matchNumber;
    }

    public LocalDateTime getStartDate() {
        return startDate;
    }

    public String getHomeTeamName() {
        return homeTeamName;
    }

    public String getAwayTeamName() {
        return awayTeamName;
    }
}