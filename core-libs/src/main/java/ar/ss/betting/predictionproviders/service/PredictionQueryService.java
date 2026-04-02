package ar.ss.betting.predictionproviders.service;

import ar.ss.betting.predictionproviders.domain.MatchPrediction;
import ar.ss.betting.predictionproviders.providers.PredictionProvider;
import ar.ss.betting.predictionproviders.service.model.*;
import lombok.RequiredArgsConstructor;
import ar.ss.betting.matchresolver.*;
import org.springframework.stereotype.Service;


import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PredictionQueryService {

    private final List<PredictionProvider> providers;
    private final MatchResolver matchResolver;

    public Map<String, List<MatchPrediction>> getUpcomingPredictionsDebug() {
        Map<String, List<MatchPrediction>> result = new LinkedHashMap<>();

        for (PredictionProvider provider : providers) {
            result.put(provider.providerName(), provider.fetchAllUpcomingPredictions());
        }

        return result;
    }

    public PredictionQueryResponse queryPredictions(List<PredictionMatchRequest> requestedMatches) {
        Map<String, ProviderFetchResult> providerResults = fetchProviderPredictions();

        List<MatchPredictionResult> results = requestedMatches.stream()
                .map(requestedMatch -> toMatchResult(requestedMatch, providerResults))
                .toList();

        return PredictionQueryResponse.builder()
                .results(results)
                .build();
    }

    private Map<String, ProviderFetchResult> fetchProviderPredictions() {
        Map<String, ProviderFetchResult> results = new LinkedHashMap<>();

        for (PredictionProvider provider : providers) {
            try {
                results.put(provider.providerName(), ProviderFetchResult.success(provider.fetchAllUpcomingPredictions()));
            } catch (Exception e) {
                results.put(provider.providerName(), ProviderFetchResult.failure(e.getMessage()));
            }
        }

        return results;
    }

    private MatchPredictionResult toMatchResult(PredictionMatchRequest requestedMatch,
                                                Map<String, ProviderFetchResult> providerResults) {
        List<ProviderPredictionResult> providerResultsForMatch = providerResults.entrySet().stream()
                .map(entry -> toProviderResult(requestedMatch, entry.getKey(), entry.getValue()))
                .toList();

        return MatchPredictionResult.builder()
                .clientMatchId(requestedMatch.getClientMatchId())
                .requestedHomeTeam(requestedMatch.getHomeTeam())
                .requestedAwayTeam(requestedMatch.getAwayTeam())
                .requestedStartTime(requestedMatch.getStartTime())
                .providers(providerResultsForMatch)
                .build();
    }

    private ProviderPredictionResult toProviderResult(PredictionMatchRequest requestedMatch,
                                                      String providerName,
                                                      ProviderFetchResult providerFetchResult) {
        if (providerFetchResult.errorMessage() != null) {
            return ProviderPredictionResult.builder()
                    .provider(providerName)
                    .status(ProviderPredictionStatus.ERROR)
                    .message(providerFetchResult.errorMessage())
                    .build();
        }

        RequestedMatchIdentity requestedIdentity = new RequestedMatchIdentity(
                requestedMatch.getHomeTeam(),
                requestedMatch.getAwayTeam(),
                requestedMatch.getStartTime()
        );

        Optional<MatchPrediction> resolved =
                matchResolver.resolve(requestedIdentity, providerFetchResult.predictions());

        if (resolved.isPresent()) {
            return ProviderPredictionResult.builder()
                    .provider(providerName)
                    .status(ProviderPredictionStatus.OK)
                    .prediction(resolved.get())
                    .build();
        }

        return ProviderPredictionResult.builder()
                .provider(providerName)
                .status(ProviderPredictionStatus.NOT_AVAILABLE)
                .message("No prediction available for requested match")
                .build();
    }

    private record ProviderFetchResult(List<MatchPrediction> predictions, String errorMessage) {
        private static ProviderFetchResult success(List<MatchPrediction> predictions) {
            return new ProviderFetchResult(predictions, null);
        }

        private static ProviderFetchResult failure(String errorMessage) {
            return new ProviderFetchResult(List.of(), errorMessage == null ? "Unknown provider error" : errorMessage);
        }
    }
}