package ar.ss.betting.predictionproviders.service.model;

import lombok.Builder;
import lombok.Value;

import java.time.OffsetDateTime;
import java.util.List;

@Value
@Builder
public class MatchPredictionResult {
    String clientMatchId;
    String requestedHomeTeam;
    String requestedAwayTeam;
    OffsetDateTime requestedStartTime;
    List<ProviderPredictionResult> providers;
}