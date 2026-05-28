package ar.ss.betting.services.dto;

import ar.ss.betting.domain.CouponStatus;
import ar.ss.betting.domain.RoundType;

import java.time.Instant;

public record CouponResponse(
        long id,
        long roundId,
        RoundType roundType,
        CouponStatus status,
        Integer correctPickCount,
        int totalCost,
        Object selections,
        Instant createdAt,
        Instant updatedAt
) { }
