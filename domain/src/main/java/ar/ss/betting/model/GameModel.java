package ar.ss.betting.model;

import ar.ss.betting.domain.Round;

import java.time.Instant;

public interface GameModel {

    ModelSelectionResult generateSelection(Round round, ModelInput modelInput, int maxBudgetInSek, Instant generatedAt);
}