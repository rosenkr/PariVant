package ar.ss.betting.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GameRoundTest {

    private GameRound createValidRound() {

        return new GameRound(
                LocalDateTime.now(),
                GameType.TOPPTIPSET,
                List.of(
                        new Match(1, LocalDateTime.now(), new Team("A"), new Team("B")),
                        new Match(2, LocalDateTime.now(), new Team("C"), new Team("D")),
                        new Match(3, LocalDateTime.now(), new Team("E"), new Team("F")),
                        new Match(4, LocalDateTime.now(), new Team("G"), new Team("H")),
                        new Match(5, LocalDateTime.now(), new Team("I"), new Team("J")),
                        new Match(6, LocalDateTime.now(), new Team("K"), new Team("L")),
                        new Match(7, LocalDateTime.now(), new Team("M"), new Team("N")),
                        new Match(8, LocalDateTime.now(), new Team("O"), new Team("P"))
                )
        );
    }

    @Test
    void shouldCreateValidGameRound() {
        GameRound round = createValidRound();
        assertEquals(8, round.getMatches().size());
    }

    @Test
    void shouldThrowIfWrongNumberOfMatches() {

        assertThrows(IllegalArgumentException.class, () ->
                new GameRound(
                        LocalDateTime.now(),
                        GameType.TOPPTIPSET,
                        List.of(
                                new Match(1, LocalDateTime.now(), new Team("A"), new Team("B"))
                        )
                )
        );
    }

    @Test
    void shouldThrowIfMatchNumbersAreDuplicate() {

        List<Match> matches = List.of(
                new Match(1, LocalDateTime.now(), new Team("A"), new Team("B")),
                new Match(1, LocalDateTime.now(), new Team("C"), new Team("D")), // duplicate
                new Match(3, LocalDateTime.now(), new Team("E"), new Team("F")),
                new Match(4, LocalDateTime.now(), new Team("G"), new Team("H")),
                new Match(5, LocalDateTime.now(), new Team("I"), new Team("J")),
                new Match(6, LocalDateTime.now(), new Team("K"), new Team("L")),
                new Match(7, LocalDateTime.now(), new Team("M"), new Team("N")),
                new Match(8, LocalDateTime.now(), new Team("O"), new Team("P"))
        );

        assertThrows(IllegalArgumentException.class, () ->
                new GameRound(
                        LocalDateTime.now(),
                        GameType.TOPPTIPSET,
                        matches
                )
        );
    }

    @Test
    void shouldThrowIfMatchNumbersAreNotConsecutive() {

        List<Match> matches = List.of(
                new Match(1, LocalDateTime.now(), new Team("A"), new Team("B")),
                new Match(2, LocalDateTime.now(), new Team("C"), new Team("D")),
                new Match(4, LocalDateTime.now(), new Team("E"), new Team("F")), // missing 3
                new Match(5, LocalDateTime.now(), new Team("G"), new Team("H")),
                new Match(6, LocalDateTime.now(), new Team("I"), new Team("J")),
                new Match(7, LocalDateTime.now(), new Team("K"), new Team("L")),
                new Match(8, LocalDateTime.now(), new Team("M"), new Team("N")),
                new Match(9, LocalDateTime.now(), new Team("O"), new Team("P"))
        );

        assertThrows(IllegalArgumentException.class, () ->
                new GameRound(
                        LocalDateTime.now(),
                        GameType.TOPPTIPSET,
                        matches
                )
        );
    }

    @Test
    void shouldThrowIfTeamAppearsTwiceInRound() {

        Team repeated = new Team("Repeated");

        List<Match> matches = List.of(
                new Match(1, LocalDateTime.now(), repeated, new Team("B")),
                new Match(2, LocalDateTime.now(), repeated, new Team("C")), // repeated team
                new Match(3, LocalDateTime.now(), new Team("D"), new Team("E")),
                new Match(4, LocalDateTime.now(), new Team("F"), new Team("G")),
                new Match(5, LocalDateTime.now(), new Team("H"), new Team("I")),
                new Match(6, LocalDateTime.now(), new Team("J"), new Team("K")),
                new Match(7, LocalDateTime.now(), new Team("L"), new Team("M")),
                new Match(8, LocalDateTime.now(), new Team("N"), new Team("O"))
        );

        assertThrows(IllegalArgumentException.class, () ->
                new GameRound(
                        LocalDateTime.now(),
                        GameType.TOPPTIPSET,
                        matches
                )
        );
    }
}
