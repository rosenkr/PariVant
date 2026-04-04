package ar.ss.betting.predictionproviders.service.model;

import ar.ss.betting.predictionproviders.domain.MatchPrediction;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class ProviderRawPredictionSnapshot {
    String provider;
    List<MatchPrediction> predictions;
    String errorMessage;
}