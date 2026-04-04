package ar.ss.betting.rework;

import ar.ss.betting.matchresolver.MatchResolver;
import ar.ss.betting.matchresolver.RequestedMatchIdentity;
import ar.ss.betting.predictionproviders.domain.MatchPrediction;
import ar.ss.betting.predictionproviders.service.model.MatchPredictionResult;
import ar.ss.betting.predictionproviders.service.model.PredictionMatchRequest;
import ar.ss.betting.predictionproviders.service.model.PredictionQueryResponse;
import ar.ss.betting.predictionproviders.service.model.ProviderPredictionResult;
import ar.ss.betting.predictionproviders.service.model.ProviderPredictionStatus;
import ar.ss.betting.predictionproviders.service.model.ProviderRawPredictionSnapshot;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class PredictionResolutionService {

    private final MatchResolver matchResolver;

    public PredictionResolutionService(MatchResolver matchResolver) {
        this.matchResolver = Objects.requireNonNull(matchResolver);
    }

    public PredictionQueryResponse resolve(List<PredictionMatchRequest> requestedMatches,
                                           List<ProviderRawPredictionSnapshot> providerSnapshots) {

        List<MatchPredictionResult> results = requestedMatches.stream()
                .map(request -> toMatchResult(request, providerSnapshots))
                .toList();

        return PredictionQueryResponse.builder()
                .results(results)
                .build();
    }

    private MatchPredictionResult toMatchResult(PredictionMatchRequest requestedMatch,
                                                List<ProviderRawPredictionSnapshot> providerSnapshots) {
        List<ProviderPredictionResult> providers = providerSnapshots.stream()
                .map(snapshot -> toProviderResult(requestedMatch, snapshot))
                .toList();

        return MatchPredictionResult.builder()
                .clientMatchId(requestedMatch.getClientMatchId())
                .requestedHomeTeam(requestedMatch.getHomeTeam())
                .requestedAwayTeam(requestedMatch.getAwayTeam())
                .requestedStartTime(requestedMatch.getStartTime())
                .providers(providers)
                .build();
    }

    private ProviderPredictionResult toProviderResult(PredictionMatchRequest requestedMatch,
                                                      ProviderRawPredictionSnapshot snapshot) {
        if (snapshot.getErrorMessage() != null) {
            return ProviderPredictionResult.builder()
                    .provider(snapshot.getProvider())
                    .status(ProviderPredictionStatus.ERROR)
                    .message(snapshot.getErrorMessage())
                    .build();
        }

        RequestedMatchIdentity requestedIdentity = new RequestedMatchIdentity(
                requestedMatch.getHomeTeam(),
                requestedMatch.getAwayTeam(),
                requestedMatch.getStartTime()
        );

        Optional<MatchPrediction> resolved = matchResolver.resolve(
                requestedIdentity,
                snapshot.getPredictions()
        );

        if (resolved.isPresent()) {
            return ProviderPredictionResult.builder()
                    .provider(snapshot.getProvider())
                    .status(ProviderPredictionStatus.OK)
                    .prediction(resolved.get())
                    .build();
        }

        return ProviderPredictionResult.builder()
                .provider(snapshot.getProvider())
                .status(ProviderPredictionStatus.NOT_AVAILABLE)
                .message("No prediction available for requested match")
                .build();
    }
}