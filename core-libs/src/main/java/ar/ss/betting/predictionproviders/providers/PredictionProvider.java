package ar.ss.betting.predictionproviders.providers;

import ar.ss.betting.predictionproviders.domain.MatchPrediction;

import java.util.List;

public interface PredictionProvider {

    String providerName();

    List<MatchPrediction> fetchAllUpcomingPredictions();
}