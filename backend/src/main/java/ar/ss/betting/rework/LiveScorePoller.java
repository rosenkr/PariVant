package ar.ss.betting.rework;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.util.List;
import java.util.Objects;

/**
 * Polls API-Football live fixtures and updates LiveScoreService cache.
 *
 * Schedule:
 * - every 10 minutes
 * - only between 12:00 and 00:00 (local server time)
 *
 * NOTE: We poll regardless of SSE subscriber count (per your current preference),
 * but still limit to the time window to stay within quota.
 */
@Service
public class LiveScorePoller {

    private static final Logger log = LoggerFactory.getLogger(LiveScorePoller.class);

    private static final LocalTime WINDOW_START = LocalTime.of(12, 0);

    @SuppressWarnings("unused")
    private static final LocalTime WINDOW_END = LocalTime.MIDNIGHT;

    private final ApiFootballClient apiFootballClient;
    private final LiveScoreService liveScoreService;

    public LiveScorePoller(ApiFootballClient apiFootballClient,
                           LiveScoreService liveScoreService) {
        this.apiFootballClient = Objects.requireNonNull(apiFootballClient);
        this.liveScoreService = Objects.requireNonNull(liveScoreService);
    }

    @Scheduled(fixedDelay = 600_000) // 10 minutes
    public void tick() {
        LocalTime now = LocalTime.now();
        if (!withinWindow(now)) {
            return;
        }

        try {
            liveScoreService.syncStatusesFromTime();

            List<ApiFootballClient.LiveFixture> fixtures = apiFootballClient.getLiveFixtures();
            liveScoreService.updateFixtures(fixtures);
            liveScoreService.applyLiveScoresToPersistedMatches();
            liveScoreService.broadcastAllSubscribedRounds();

            log.info("[live] polled {} fixtures", fixtures.size());
        } catch (Exception e) {
            log.warn("[live] poll failed: {}", e.getMessage());
        }
    }

    private boolean withinWindow(LocalTime now) {
        return !now.isBefore(WINDOW_START);
    }
}