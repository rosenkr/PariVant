package ar.ss.betting.predictionproviders.providers.clubelo;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.List;

@Value
@Builder
public class ClubEloResponse {
    Instant fetchedAt;
    List<ClubEloFixtureRow> fixtures;
}