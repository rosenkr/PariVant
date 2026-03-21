package ar.ss.betting.model;

import ar.ss.betting.domain.Round;
public interface GameModel {

    ModelSelectionResult generateSelection(Round round,
                                           ModelInput modelInput,
                                           int maxBudgetInSek);
}