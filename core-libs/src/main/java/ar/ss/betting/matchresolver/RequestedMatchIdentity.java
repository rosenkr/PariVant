package ar.ss.betting.matchresolver;
import java.time.OffsetDateTime;
import java.util.Objects;

public class RequestedMatchIdentity {

    private final String homeTeam;
    private final String awayTeam;
    private final OffsetDateTime startTime;

    public RequestedMatchIdentity(String homeTeam, String awayTeam, OffsetDateTime startTime) {
        this.homeTeam = Objects.requireNonNull(homeTeam, "homeTeam cannot be null");
        this.awayTeam = Objects.requireNonNull(awayTeam, "awayTeam cannot be null");
        this.startTime = startTime;
    }

    public String getHomeTeam() {
        return homeTeam;
    }

    public String getAwayTeam() {
        return awayTeam;
    }

    public OffsetDateTime getStartTime() {
        return startTime;
    }
}