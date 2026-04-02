package ar.ss.betting.predictionproviders.providers._11elo;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.List;

@Value
@Builder
public class _11eloResponse {
    Instant fetchedAt;
    List<_11eloMatchRow> matches;
}