package ar.ss.betting.model;

import java.util.Objects;

/**
 * Model-side data for one match.
 *
 * Contains:
 * - Market implied probabilities
 * - Public pick distribution
 * - Recent form scores for home and away teams
 *
 * Recent form definition (v1):
 * - Last 5 matches
 * - Win = 2
 * - Draw = 1
 * - Loss = 0
 *
 * Raw form range: [0, 10]
 */
public class MatchContext {

    private final ProbabilityTriple marketProbabilities;
    private final ProbabilityTriple publicProbabilities;

    private final int homeRecentFormScore; // 0-10
    private final int awayRecentFormScore; // 0-10

    public MatchContext(ProbabilityTriple marketProbabilities,
                        ProbabilityTriple publicProbabilities,
                        int homeRecentFormScore,
                        int awayRecentFormScore) {

        this.marketProbabilities = Objects.requireNonNull(marketProbabilities);
        this.publicProbabilities = Objects.requireNonNull(publicProbabilities);

        validateForm(homeRecentFormScore);
        validateForm(awayRecentFormScore);

        this.homeRecentFormScore = homeRecentFormScore;
        this.awayRecentFormScore = awayRecentFormScore;
    }

    private void validateForm(int score) {
        if (score < 0 || score > 10) {
            throw new IllegalArgumentException("Recent form score must be between 0 and 10");
        }
    }

    public ProbabilityTriple getMarketProbabilities() {
        return marketProbabilities;
    }

    public ProbabilityTriple getPublicProbabilities() {
        return publicProbabilities;
    }

    public int getHomeRecentFormScore() {
        return homeRecentFormScore;
    }

    public int getAwayRecentFormScore() {
        return awayRecentFormScore;
    }
}