package ar.ss.betting.model;

import ar.ss.betting.domain.Coupon;
import ar.ss.betting.domain.GameRound;

/**
 * Strategy interface for different betting models.
 */
public interface GameModel {

    Coupon generateCoupon(GameRound gameRound, int budgetInSek);
}
