package ar.ss.betting.model;

import ar.ss.betting.domain.GameRound;

/**
 * Strategy interface for different betting models.
 *
 * Note: returns ModelSelectionResult (not Coupon) to keep the model as a pure engine.
 */
public interface GameModel {

    ModelSelectionResult generateSelection(GameRound gameRound, int maxBudgetInSek);
}