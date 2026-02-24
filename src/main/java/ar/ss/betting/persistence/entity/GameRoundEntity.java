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

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "gameRound", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("matchNumber ASC")
    private List<MatchEntity> matches = new ArrayList<>();

    protected GameRoundEntity() {
        // JPA
    }

    public GameRoundEntity(GameType gameType, LocalDateTime startDate) {
        this.gameType = Objects.requireNonNull(gameType, "gameType cannot be null");
        this.startDate = Objects.requireNonNull(startDate, "startDate cannot be null");
        this.createdAt = LocalDateTime.now();
    }

    public void addMatch(MatchEntity match) {
        Objects.requireNonNull(match, "match cannot be null");
        match.setGameRound(this);
        this.matches.add(match);
    }

    public void removeMatch(MatchEntity match) {
        Objects.requireNonNull(match, "match cannot be null");
        this.matches.remove(match);
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public List<MatchEntity> getMatches() {
        return matches;
    }
}