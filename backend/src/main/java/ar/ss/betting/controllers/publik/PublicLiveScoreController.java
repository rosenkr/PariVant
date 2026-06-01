package ar.ss.betting.controllers.publik;

import ar.ss.betting.services.livescore.LiveScoreService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Objects;

/**
 * Public SSE endpoint for live scores.
 *
 * Client opens:
 *   GET /public/rounds/{roundId}/live
 *
 * Server streams "snapshot" events containing latest known live data.
 */
@RestController
@RequestMapping("/public/rounds")
public class PublicLiveScoreController {

    private final LiveScoreService liveScoreService;

    public PublicLiveScoreController(LiveScoreService liveScoreService) {
        this.liveScoreService = Objects.requireNonNull(liveScoreService);
    }

    @GetMapping(path = "/{roundId}/live", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@PathVariable long roundId) {
        return liveScoreService.subscribe(roundId);
    }
}
