package ar.ss.betting.persistence.entity;

import ar.ss.betting.domain.GameType;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "game_round")
public class GameRoundEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "game_type", nullable = false, length = 32)
    private GameType gameType;

    @Column(name = "start_date", nullable = false)
    private LocalDateTime startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDateTime endDate;

    @OneToMany(mappedBy = "gameRound", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("matchNumber ASC")
    private List<MatchEntity> matches = new ArrayList<>();

    protected GameRoundEntity() {
        // JPA
    }

    public GameRoundEntity(GameType gameType, LocalDateTime startDate, LocalDateTime endDate) {
        this.gameType = Objects.requireNonNull(gameType, "gameType cannot be null");
        this.startDate = Objects.requireNonNull(startDate, "startDate cannot be null");
        this.endDate = Objects.requireNonNull(endDate, "endDate cannot be null");

        if (!endDate.isAfter(startDate)) {
            throw new IllegalArgumentException("endDate must be after startDate");
        }
    }

    public void addMatch(MatchEntity match) {
        Objects.requireNonNull(match);
        matches.add(match);
        match.setGameRound(this);
    }

    public void removeMatch(MatchEntity match) {
        matches.remove(match);
        match.setGameRound(null);
    }

    public Long getId() {
        return id;
    }

    public GameType getGameType() {
        return gameType;
    }

    public LocalDateTime getStartDate() {
        return startDate;
    }

    public LocalDateTime getEndDate() {
        return endDate;
    }

    public List<MatchEntity> getMatches() {
        return matches;
    }
}