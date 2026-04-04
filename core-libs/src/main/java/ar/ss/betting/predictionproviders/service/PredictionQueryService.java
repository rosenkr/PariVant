package ar.ss.betting.predictionproviders.service;

import ar.ss.betting.predictionproviders.domain.MatchPrediction;
import ar.ss.betting.predictionproviders.providers.PredictionProvider;
import ar.ss.betting.predictionproviders.service.model.ProviderRawPredictionSnapshot;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PredictionQueryService {

    private final List<PredictionProvider> providers;

    public Map<String, List<MatchPrediction>> getUpcomingPredictionsDebug() {
        Map<String, List<MatchPrediction>> result = new LinkedHashMap<>();

        for (PredictionProvider provider : providers) {
            result.put(provider.providerName(), provider.fetchAllUpcomingPredictions());
        }

        return result;
    }

    public List<ProviderRawPredictionSnapshot> fetchProviderSnapshots() {
        return providers.stream()
                .map(this::fetchProviderSnapshot)
                .toList();
    }

    private ProviderRawPredictionSnapshot fetchProviderSnapshot(PredictionProvider provider) {
        try {
            return ProviderRawPredictionSnapshot.builder()
                    .provider(provider.providerName())
                    .predictions(List.copyOf(provider.fetchAllUpcomingPredictions()))
                    .errorMessage(null)
                    .build();
        } catch (Exception e) {
            return ProviderRawPredictionSnapshot.builder()
                    .provider(provider.providerName())
                    .predictions(List.of())
                    .errorMessage(e.getMessage() == null ? "Unknown provider error" : e.getMessage())
                    .build();
        }
    }
}