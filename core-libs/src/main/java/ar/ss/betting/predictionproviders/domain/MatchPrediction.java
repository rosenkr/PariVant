package ar.ss.betting.predictionproviders.domain;

import ar.ss.betting.matchresolver.MatchIdentityCandidate;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@Builder
public class MatchPrediction implements MatchIdentityCandidate {
    String provider;
    String homeTeam;
    String awayTeam;
    Instant kickoff;
    String kickoffRaw;
    ProviderProbabilityTriple probabilities;
    Instant fetchedAt;
}