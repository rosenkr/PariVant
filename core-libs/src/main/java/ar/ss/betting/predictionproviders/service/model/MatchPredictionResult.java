package ar.ss.betting.predictionproviders.service.model;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.List;

@Value
@Builder
public class MatchPredictionResult {
    String clientMatchId;
    String requestedHomeTeam;
    String requestedAwayTeam;
    Instant requestedStartTime;
    List<ProviderPredictionResult> providers;
}