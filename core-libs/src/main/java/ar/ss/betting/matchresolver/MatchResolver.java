package ar.ss.betting.matchresolver;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Component
public class MatchResolver {

    private static final Duration MAX_KICKOFF_DIFFERENCE = Duration.ofDays(1);

    private final TeamNameNormalizer teamNameNormalizer;

    public MatchResolver(TeamNameNormalizer teamNameNormalizer) {
        this.teamNameNormalizer = teamNameNormalizer;
    }

    public <T extends MatchIdentityCandidate> Optional<T> resolve(RequestedMatchIdentity requestedMatch,
                                                                  List<T> candidates) {
        String requestedHome = teamNameNormalizer.normalize(requestedMatch.getHomeTeam());
        String requestedAway = teamNameNormalizer.normalize(requestedMatch.getAwayTeam());
        OffsetDateTime requestedStartTime = requestedMatch.getStartTime();

        return candidates.stream()
                .filter(candidate -> sameTeams(requestedHome, requestedAway, candidate))
                .filter(candidate -> withinTolerance(requestedStartTime, candidate.getKickoff()))
                .min(Comparator.comparing(candidate ->
                        absoluteSecondsBetweenOrZero(requestedStartTime, candidate.getKickoff())));
    }

    private boolean sameTeams(String requestedHome,
                              String requestedAway,
                              MatchIdentityCandidate candidate) {
        String candidateHome = teamNameNormalizer.normalize(candidate.getHomeTeam());
        String candidateAway = teamNameNormalizer.normalize(candidate.getAwayTeam());

        return requestedHome.equals(candidateHome) && requestedAway.equals(candidateAway);
    }

    private boolean withinTolerance(OffsetDateTime requestedStartTime, OffsetDateTime candidateKickoff) {
        // For now, if either side lacks kickoff data, allow team-name match to decide.
        if (requestedStartTime == null || candidateKickoff == null) {
            return true;
        }

        return Duration.between(requestedStartTime, candidateKickoff).abs()
                .compareTo(MAX_KICKOFF_DIFFERENCE) <= 0;
    }

    private long absoluteSecondsBetweenOrZero(OffsetDateTime a, OffsetDateTime b) {
        if (a == null || b == null) {
            return 0L;
        }
        return Duration.between(a, b).abs().getSeconds();
    }
}