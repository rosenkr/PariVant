package ar.ss.betting.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class CouponTest {

    private GameRound createValidRound() {

        List<Match> matches = List.of(
                new Match(1, LocalDateTime.now(), new Team("A"), new Team("B")),
                new Match(2, LocalDateTime.now(), new Team("C"), new Team("D")),
                new Match(3, LocalDateTime.now(), new Team("E"), new Team("F")),
                new Match(4, LocalDateTime.now(), new Team("G"), new Team("H")),
                new Match(5, LocalDateTime.now(), new Team("I"), new Team("J")),
                new Match(6, LocalDateTime.now(), new Team("K"), new Team("L")),
                new Match(7, LocalDateTime.now(), new Team("M"), new Team("N")),
                new Match(8, LocalDateTime.now(), new Team("O"), new Team("P"))
        );

        return new GameRound(
                LocalDateTime.now(),
                GameType.TOPPTIPSET,
                matches
        );
    }

    @Test
    void shouldCreateValidCoupon() {

        GameRound round = createValidRound();

        Map<Integer, Set<Outcome>> selections = new HashMap<>();

        for (Match match : round.getMatches()) {
            selections.put(match.getMatchNumber(), Set.of(Outcome.HOME_WIN));
        }

        Coupon coupon = new Coupon(
                round,
                LocalDateTime.now(),
                100,
                selections
        );

        assertEquals(8, coupon.getSelections().size());
    }

    @Test
    void shouldThrowIfBudgetIsNegative() {

        GameRound round = createValidRound();

        Map<Integer, Set<Outcome>> selections = new HashMap<>();

        assertThrows(IllegalArgumentException.class, () ->
                new Coupon(
                        round,
                        LocalDateTime.now(),
                        -10,
                        selections
                )
        );
    }

    @Test
    void shouldThrowIfSelectionsDoNotMatchGameRound() {

        GameRound round = createValidRound();

        Map<Integer, Set<Outcome>> selections = new HashMap<>();
        selections.put(99, Set.of(Outcome.HOME_WIN)); // invalid match number

        assertThrows(IllegalArgumentException.class, () ->
                new Coupon(
                        round,
                        LocalDateTime.now(),
                        100,
                        selections
                )
        );
    }
}
