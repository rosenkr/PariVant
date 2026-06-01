package ar.ss.betting.services.dto;

public record ModelRunResponse(
        long id,
        int budgetInSek,
        String trigger,
        ModelResultResponse result
) { }
