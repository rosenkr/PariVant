package ar.ss.betting.predictionproviders.service.model;

import ar.ss.betting.predictionproviders.domain.MatchPrediction;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ProviderPredictionResult {
    String provider;
    ProviderPredictionStatus status;
    MatchPrediction prediction;
    String message;
}