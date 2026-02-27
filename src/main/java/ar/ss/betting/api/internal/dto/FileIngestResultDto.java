package ar.ss.betting.api.internal.dto;

public record FileIngestResultDto(
        String fileName,
        String status,   // INGESTED | DUPLICATE_SKIPPED | FAILED
        Long roundId,    // set for INGESTED; also for DUPLICATE if we found existing
        String message
) {}