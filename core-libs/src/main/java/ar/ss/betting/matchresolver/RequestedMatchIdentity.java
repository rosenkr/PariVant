package ar.ss.betting.matchresolver;
import java.time.OffsetDateTime;
import java.util.Objects;

public record RequestedMatchIdentity(
        String homeTeam,
        String awayTeam,
        OffsetDateTime startTime
) {
    public RequestedMatchIdentity {
        homeTeam = Objects.requireNonNull(homeTeam, "homeTeam cannot be null");
        awayTeam = Objects.requireNonNull(awayTeam, "awayTeam cannot be null");
    }
}