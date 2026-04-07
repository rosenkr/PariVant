package ar.ss.betting.persistence.entity;

import ar.ss.betting.domain.RoundStatus;
import ar.ss.betting.domain.RoundType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "round")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RoundEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "round_type", nullable = false, length = 32)
    private RoundType roundType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private RoundStatus status;

    @Column(name = "start_date", nullable = false)
    private LocalDateTime startDate;

    @OneToMany(mappedBy = "round", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("matchNumber ASC")
    private List<MatchEntity> matches = new ArrayList<>();

    public RoundEntity(RoundType roundType, LocalDateTime startDate) {
        this(roundType, RoundStatus.UPCOMING, startDate);
    }

    public RoundEntity(RoundType roundType, RoundStatus status, LocalDateTime startDate) {
        this.roundType = Objects.requireNonNull(roundType, "roundType cannot be null");
        this.status = Objects.requireNonNull(status, "status cannot be null");
        this.startDate = Objects.requireNonNull(startDate, "startDate cannot be null");
    }

    public void addMatch(MatchEntity match) {
        Objects.requireNonNull(match);
        matches.add(match);
        match.setRound(this);
    }

    public void removeMatch(MatchEntity match) {
        matches.remove(match);
        match.setRound(null);
    }

    public void setStatus(RoundStatus status) {
        this.status = Objects.requireNonNull(status, "status cannot be null");
    }
    public List<MatchEntity> getMatches() {
        return Collections.unmodifiableList(matches);
    }
}