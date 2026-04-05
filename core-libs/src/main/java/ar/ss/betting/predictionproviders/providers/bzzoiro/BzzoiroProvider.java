package ar.ss.betting.predictionproviders.providers.bzzoiro;

import ar.ss.betting.predictionproviders.domain.MatchPrediction;
import ar.ss.betting.predictionproviders.providers.PredictionProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class BzzoiroProvider implements PredictionProvider {

    private final BzzoiroClient client;
    private final BzzoiroMapper mapper;

    @Override
    public String providerName() {
        return "bzzoiro";
    }

    @Override
    public List<MatchPrediction> fetchAllUpcomingPredictions() {
        BzzoiroResponse response = client.fetchUpcomingPredictions();
        return mapper.toMatchPredictions(response);
    }
}