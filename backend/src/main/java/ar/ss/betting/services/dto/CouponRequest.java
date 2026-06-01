package ar.ss.betting.services.dto;

import java.util.List;
import java.util.Map;

public record CouponRequest(
        long roundId,
        Map<Integer, List<String>> selections,
        Integer confidentPickMatchNumber
) { }
