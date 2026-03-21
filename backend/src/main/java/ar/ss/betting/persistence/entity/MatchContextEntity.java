package ar.ss.betting.persistence.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "match_context")
public class MatchContextEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "round_id", nullable = false)
    private Long roundId;

    @Column(name = "match_number", nullable = false)
    private int matchNumber;

    @Column(name = "market_home", nullable = false)
    private double marketHome;

    @Column(name = "market_draw", nullable = false)
    private double marketDraw;

    @Column(name = "market_away", nullable = false)
    private double marketAway;

    @Column(name = "public_home", nullable = false)
    private double publicHome;

    @Column(name = "public_draw", nullable = false)
    private double publicDraw;

    @Column(name = "public_away", nullable = false)
    private double publicAway;

    protected MatchContextEntity() {
        // JPA
    }

    public MatchContextEntity(Long roundId,
                              int matchNumber,
                              double marketHome,
                              double marketDraw,
                              double marketAway,
                              double publicHome,
                              double publicDraw,
                              double publicAway) {
        this.roundId = roundId;
        this.matchNumber = matchNumber;
        this.marketHome = marketHome;
        this.marketDraw = marketDraw;
        this.marketAway = marketAway;
        this.publicHome = publicHome;
        this.publicDraw = publicDraw;
        this.publicAway = publicAway;
    }

    public Long getId() {
        return id;
    }

    public Long getRoundId() {
        return roundId;
    }

    public int getMatchNumber() {
        return matchNumber;
    }

    public double getMarketHome() {
        return marketHome;
    }

    public double getMarketDraw() {
        return marketDraw;
    }

    public double getMarketAway() {
        return marketAway;
    }

    public double getPublicHome() {
        return publicHome;
    }

    public double getPublicDraw() {
        return publicDraw;
    }

    public double getPublicAway() {
        return publicAway;
    }
}