package ar.ss.betting.api.internal.dto;

import java.time.Instant;
import java.util.List;

public record IngestReportDto(
        Instant timestamp,
        int ingestedCount,
        int duplicateCount,
        int failedCount,
        List<FileIngestResultDto> results
) {}
