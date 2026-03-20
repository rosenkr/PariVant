package ar.ss.betting.model;

import ar.ss.betting.domain.GameRound;

/**
 * Strategy interface for different betting models.
 *
 * ModelInput contains match-level probabilities from market and public distribution.
 * The domain (GameRound) stays clean and independent of data providers.
 */
public interface GameModel {

    ModelSelectionResult generateSelection(GameRound gameRound, ModelInput modelInput, int maxBudgetInSek);
}