package ar.ss.betting.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class MatchTest {

    @Test
    void shouldDefaultScoreAndStatus() {
        Match match = new Match(
                1,
                Instant.now(),
                new Team("A"),
                new Team("B")
        );

        assertEquals(0, match.getHomeScore());
        assertEquals(0, match.getAwayScore());
        assertEquals(MatchStatus.UPCOMING, match.getStatus());
    }

    @Test
    void shouldSupportExplicitScoreAndStatus() {
        Match match = new Match(
                1,
                Instant.now(),
                new Team("A"),
                new Team("B"),
                2,
                1,
                MatchStatus.FINISHED
        );

        assertEquals(2, match.getHomeScore());
        assertEquals(1, match.getAwayScore());
        assertEquals(MatchStatus.FINISHED, match.getStatus());
    }

    @Test
    void shouldThrowIfTeamsAreSame() {
        Team t1 = new Team("A");

        assertThrows(IllegalArgumentException.class, () ->
                new Match(1, Instant.now(), t1, t1)
        );
    }

    @Test
    void shouldThrowIfScoresAreNegative() {
        assertThrows(IllegalArgumentException.class, () ->
                new Match(1, Instant.now(), new Team("A"), new Team("B"), -1, 0, MatchStatus.RUNNING)
        );

        assertThrows(IllegalArgumentException.class, () ->
                new Match(1, Instant.now(), new Team("A"), new Team("B"), 0, -1, MatchStatus.RUNNING)
        );
    }
}