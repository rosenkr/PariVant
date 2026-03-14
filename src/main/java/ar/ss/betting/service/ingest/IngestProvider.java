package ar.ss.betting.service.ingest;

import ar.ss.betting.domain.GameType;
import java.util.Optional;

public interface IngestProvider {

    /**
     * Human-readable identifier for logging / DB, e.g. "TIPZER" or "ODDSONLINE".
     */
    String sourceName();

    /**
     * Whether this provider can ingest the given game type.
     */
    boolean supports(GameType gameType);

    /**
     * Attempt to ingest the "next" round for this game type.
     *
     * On success returns an outcome (CREATED or DUPLICATE).
     * On failure throws an exception (orchestrator will log + fallback).
     */
    IngestOutcome ingestNext(GameType gameType);

    record IngestOutcome(
            String status, // CREATED | DUPLICATE
            GameType gameType,
            java.time.LocalDateTime roundStartDate,
            Long roundId
    ) {
        public static IngestOutcome created(GameType gameType, java.time.LocalDateTime start, long roundId) {
            return new IngestOutcome("CREATED", gameType, start, roundId);
        }

        public static IngestOutcome duplicate(GameType gameType, java.time.LocalDateTime start) {
            return new IngestOutcome("DUPLICATE", gameType, start, null);
        }
    }
}