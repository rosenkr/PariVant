package ar.ss.betting.predictionproviders.providers.clubelo;

import ar.ss.betting.predictionproviders.domain.MatchPrediction;
import ar.ss.betting.predictionproviders.providers.PredictionProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ClubEloProvider implements PredictionProvider {

    private final ClubEloClient client;
    private final ClubEloMapper mapper;

    @Override
    public String providerName() {
        return "clubelo";
    }

    @Override
    public List<MatchPrediction> fetchAllUpcomingPredictions() {
        ClubEloResponse response = client.fetchFixtures();
        return mapper.toMatchPredictions(response);
    }
}