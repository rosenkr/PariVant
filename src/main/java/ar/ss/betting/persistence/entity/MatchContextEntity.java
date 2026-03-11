package ar.ss.betting.persistence.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "match_context",
        uniqueConstraints = @UniqueConstraint(name = "uq_match_context_round_match", columnNames = {"game_round_id", "match_number"})
)
public class MatchContextEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "game_round_id", nullable = false)
    private Long gameRoundId;

    @Column(name = "match_number", nullable = false)
    private Integer matchNumber;

    @Column(name = "market_home", nullable = false)
    private Double marketHome;

    @Column(name = "market_draw", nullable = false)
    private Double marketDraw;

    @Column(name = "market_away", nullable = false)
    private Double marketAway;

    @Column(name = "public_home", nullable = false)
    private Double publicHome;

    @Column(name = "public_draw", nullable = false)
    private Double publicDraw;

    @Column(name = "public_away", nullable = false)
    private Double publicAway;

    @Column(name = "home_recent_form_score", nullable = false)
    private Integer homeRecentFormScore;

    @Column(name = "away_recent_form_score", nullable = false)
    private Integer awayRecentFormScore;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected MatchContextEntity() { }

    public MatchContextEntity(
            Long gameRoundId,
            Integer matchNumber,
            Double marketHome,
            Double marketDraw,
            Double marketAway,
            Double publicHome,
            Double publicDraw,
            Double publicAway,
            Integer homeRecentFormScore,
            Integer awayRecentFormScore
    ) {
        this.gameRoundId = gameRoundId;
        this.matchNumber = matchNumber;
        this.marketHome = marketHome;
        this.marketDraw = marketDraw;
        this.marketAway = marketAway;
        this.publicHome = publicHome;
        this.publicDraw = publicDraw;
        this.publicAway = publicAway;
        this.homeRecentFormScore = homeRecentFormScore;
        this.awayRecentFormScore = awayRecentFormScore;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Long getGameRoundId() { return gameRoundId; }
    public Integer getMatchNumber() { return matchNumber; }

    public Double getMarketHome() { return marketHome; }
    public Double getMarketDraw() { return marketDraw; }
    public Double getMarketAway() { return marketAway; }

    public Double getPublicHome() { return publicHome; }
    public Double getPublicDraw() { return publicDraw; }
    public Double getPublicAway() { return publicAway; }

    public Integer getHomeRecentFormScore() { return homeRecentFormScore; }
    public Integer getAwayRecentFormScore() { return awayRecentFormScore; }
}