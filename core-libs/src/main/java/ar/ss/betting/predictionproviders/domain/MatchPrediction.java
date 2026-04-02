package ar.ss.betting.predictionproviders.domain;

import ar.ss.betting.matchresolver.MatchIdentityCandidate;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.time.OffsetDateTime;

@Value
@Builder
public class MatchPrediction implements MatchIdentityCandidate {
    String provider;
    String homeTeam;
    String awayTeam;
    OffsetDateTime kickoff;
    String kickoffRaw;
    ProbabilityTriple probabilities;
    Instant fetchedAt;
}