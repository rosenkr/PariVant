package ar.ss.betting.matchresolver;
import java.time.Instant;
import java.time.OffsetDateTime;

public interface MatchIdentityCandidate {
    String getHomeTeam();
    String getAwayTeam();
    Instant getKickoff();
}