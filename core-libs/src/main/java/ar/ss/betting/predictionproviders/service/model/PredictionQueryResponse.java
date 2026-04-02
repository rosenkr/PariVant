package ar.ss.betting.predictionproviders.service.model;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class PredictionQueryResponse {
    List<MatchPredictionResult> results;
}