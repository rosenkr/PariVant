package ar.ss.betting.domain;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CouponTest {

    @Test
    void newCouponShouldBeUndeterminedAndSupportDoublesAndTriples() {
        Coupon coupon = new Coupon(1L, 10L, RoundType.TOPPTIPSET, selections(RoundType.TOPPTIPSET));

        assertEquals(CouponStatus.UNDETERMINED, coupon.getStatus());
        assertEquals(6, coupon.getTotalCost());
        assertFalse(coupon.isWinningCoupon());
    }

    @Test
    void shouldResolveTopptipsetAsWinOnlyWhenAllPicksAreCorrect() {
        Coupon coupon = new Coupon(1L, 10L, RoundType.TOPPTIPSET, selections(RoundType.TOPPTIPSET));

        Coupon resolved = coupon.resolve(actualOutcomes(RoundType.TOPPTIPSET, Outcome.HOME_WIN));

        assertEquals(CouponStatus.WIN, resolved.getStatus());
        assertEquals(8, resolved.getCorrectPickCount());
        assertTrue(resolved.isWinningCoupon());
    }

    @Test
    void shouldResolveStryktipsetAsLoseBelowTenCorrectPicks() {
        Coupon coupon = new Coupon(1L, 10L, RoundType.STRYKTIPSET, selections(RoundType.STRYKTIPSET));

        Map<Integer, Outcome> actual = actualOutcomes(RoundType.STRYKTIPSET, Outcome.HOME_WIN);
        actual.put(10, Outcome.AWAY_WIN);
        actual.put(11, Outcome.AWAY_WIN);
        actual.put(12, Outcome.AWAY_WIN);
        actual.put(13, Outcome.AWAY_WIN);

        Coupon resolved = coupon.resolve(actual);

        assertEquals(CouponStatus.LOSE, resolved.getStatus());
        assertEquals(9, resolved.getCorrectPickCount());
        assertFalse(resolved.isWinningCoupon());
    }

    @Test
    void shouldRejectCorrectPickCountOnUndeterminedCoupon() {
        Map<Integer, Set<Outcome>> selections = selections(RoundType.TOPPTIPSET);

        assertThrows(
                IllegalArgumentException.class,
                () -> new Coupon(1L, 10L, RoundType.TOPPTIPSET, CouponStatus.UNDETERMINED, 0, selections)
        );
    }

    private Map<Integer, Set<Outcome>> selections(RoundType roundType) {
        Map<Integer, Set<Outcome>> selections = new LinkedHashMap<>();

        for (int matchNumber = 1; matchNumber <= roundType.getNumberOfMatches(); matchNumber++) {
            selections.put(matchNumber, EnumSet.of(Outcome.HOME_WIN));
        }

        selections.put(1, EnumSet.of(Outcome.HOME_WIN, Outcome.DRAW));
        selections.put(2, EnumSet.allOf(Outcome.class));

        return selections;
    }

    private Map<Integer, Outcome> actualOutcomes(RoundType roundType, Outcome defaultOutcome) {
        Map<Integer, Outcome> actual = new LinkedHashMap<>();

        for (int matchNumber = 1; matchNumber <= roundType.getNumberOfMatches(); matchNumber++) {
            actual.put(matchNumber, defaultOutcome);
        }

        return actual;
    }
}
