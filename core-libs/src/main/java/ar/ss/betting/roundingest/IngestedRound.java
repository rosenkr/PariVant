package ar.ss.betting.roundingest;

import ar.ss.betting.domain.RoundType;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;

public record IngestedRound(
        RoundType roundType,
        OffsetDateTime roundStart,
        List<IngestedMatch> matches
) {
    public IngestedRound {
        Objects.requireNonNull(roundType, "roundType");
        Objects.requireNonNull(roundStart, "roundStart");
        Objects.requireNonNull(matches, "matches");

        if (matches.isEmpty()) {
            throw new IllegalArgumentException("matches cannot be empty");
        }

        matches = List.copyOf(matches);
    }
}