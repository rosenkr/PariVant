package ar.ss.betting.persistence.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "match_context")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
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

    @Column(name = "market_fallback_used", nullable = false)
    private boolean marketFallbackUsed;

    @Column(name = "market_fallback_reason")
    private String marketFallbackReason;

    public MatchContextEntity(Long roundId,
                              int matchNumber,
                              double marketHome,
                              double marketDraw,
                              double marketAway,
                              double publicHome,
                              double publicDraw,
                              double publicAway) {
        this(
                roundId,
                matchNumber,
                marketHome,
                marketDraw,
                marketAway,
                publicHome,
                publicDraw,
                publicAway,
                false,
                null
        );
    }

    public MatchContextEntity(Long roundId,
                              int matchNumber,
                              double marketHome,
                              double marketDraw,
                              double marketAway,
                              double publicHome,
                              double publicDraw,
                              double publicAway,
                              boolean marketFallbackUsed,
                              String marketFallbackReason) {
        this.roundId = roundId;
        this.matchNumber = matchNumber;
        this.marketHome = marketHome;
        this.marketDraw = marketDraw;
        this.marketAway = marketAway;
        this.publicHome = publicHome;
        this.publicDraw = publicDraw;
        this.publicAway = publicAway;
        this.marketFallbackUsed = marketFallbackUsed;
        this.marketFallbackReason = marketFallbackReason;
    }
}