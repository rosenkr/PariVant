package ar.ss.betting.predictionproviders.providers._11elo;

import ar.ss.betting.predictionproviders.domain.MatchPrediction;
import ar.ss.betting.predictionproviders.providers.PredictionProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class _11eloProvider implements PredictionProvider {

    private final _11eloClient client;
    private final _11eloMapper mapper;

    @Override
    public String providerName() {
        return "11elo";
    }

    @Override
    public List<MatchPrediction> fetchAllUpcomingPredictions() {
        _11eloResponse response = client.fetchUpcomingMatches();
        return mapper.toMatchPredictions(response);
    }
}