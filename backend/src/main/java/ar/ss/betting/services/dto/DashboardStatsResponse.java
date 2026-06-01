package ar.ss.betting.services.dto;

public record DashboardStatsResponse(
        StatBlock couponStats,
        StatBlock confidentPickStats
) {
    public record StatBlock(
            int wins,
            int losses,
            int undetermined
    ) { }
}
