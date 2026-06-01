package ar.ss.betting.controllers.authenticated;

import ar.ss.betting.security.UserEntity;
import ar.ss.betting.services.DashboardStatsService;
import ar.ss.betting.services.dto.DashboardStatsResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

@RestController
@RequestMapping("/dashboard")
public class DashboardStatsController {

    private final DashboardStatsService dashboardStatsService;

    public DashboardStatsController(DashboardStatsService dashboardStatsService) {
        this.dashboardStatsService = Objects.requireNonNull(dashboardStatsService);
    }

    @GetMapping("/stats")
    public ResponseEntity<DashboardStatsResponse> getStats(
            @AuthenticationPrincipal UserEntity user
    ) {
        return ResponseEntity.ok(dashboardStatsService.getStats(user));
    }
}
