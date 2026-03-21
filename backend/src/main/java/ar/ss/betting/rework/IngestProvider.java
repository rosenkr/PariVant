package ar.ss.betting.rework;

import ar.ss.betting.domain.RoundType;

public interface IngestProvider {

    String sourceName();

    boolean supports(RoundType roundType);

    IngestOutcome ingestNext(RoundType roundType);

    record IngestOutcome(
            String status,
            RoundType roundType,
            java.time.LocalDateTime roundStartDate,
            Long roundId
    ) {
        public static IngestOutcome created(RoundType roundType, java.time.LocalDateTime start, long roundId) {
            return new IngestOutcome("CREATED", roundType, start, roundId);
        }

        public static IngestOutcome duplicate(RoundType roundType, java.time.LocalDateTime start) {
            return new IngestOutcome("DUPLICATE", roundType, start, null);
        }
    }
}