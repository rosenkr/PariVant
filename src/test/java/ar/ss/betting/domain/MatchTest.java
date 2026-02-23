package ar.ss.betting.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class MatchTest {

    @Test
    void shouldThrowIfTeamsAreSame() {
        Team t1 = new Team("A");

        assertThrows(IllegalArgumentException.class, () ->
                new Match(1, LocalDateTime.now(), t1, t1)
        );
    }
}
