package ar.ss.betting.rework;

import ar.ss.betting.persistence.entity.MatchEntity;
import ar.ss.betting.persistence.repo.MatchRepository;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Stores latest live fixtures in memory and broadcasts round-specific snapshots over SSE.
 *
 * Matching v0:
 * - normalize = trim + lowercase
 * - match Tipzer match (home/away) against API-Football fixture (home/away) exactly.
 */
@Service
public class LiveScoreService {

    // IMPORTANT: avoid "graceful shutdown aborted" by not keeping SSE requests open forever.
    // Client will automatically reconnect; we also push a snapshot on connect.
    private static final long EMITTER_TIMEOUT_MS = 5 * 60 * 1000L; // 5 minutes

    private final MatchRepository matchRepository;

    // emitters per roundId
    private final Map<Long, CopyOnWriteArrayList<SseEmitter>> emittersByRound = new ConcurrentHashMap<>();

    // latest fixtures keyed by "home||away" after normalize
    private volatile Map<String, ApiFootballClient.LiveFixture> latestFixtureMap = Map.of();

    public LiveScoreService(MatchRepository matchRepository) {
        this.matchRepository = Objects.requireNonNull(matchRepository);
    }

    public SseEmitter subscribe(long roundId) {
        // finite timeout => connection won't block shutdown forever
        SseEmitter emitter = new SseEmitter(EMITTER_TIMEOUT_MS);

        emittersByRound.computeIfAbsent(roundId, __ -> new CopyOnWriteArrayList<>()).add(emitter);

        emitter.onCompletion(() -> removeAndComplete(roundId, emitter, false));
        emitter.onTimeout(() -> removeAndComplete(roundId, emitter, true));
        emitter.onError((e) -> removeAndComplete(roundId, emitter, true));

        // Send an initial snapshot immediately (best effort)
        safeSend(roundId, emitter, buildSnapshot(roundId));

        return emitter;
    }

    public void updateFixtures(List<ApiFootballClient.LiveFixture> fixtures) {
        Map<String, ApiFootballClient.LiveFixture> map = new HashMap<>();
        for (ApiFootballClient.LiveFixture f : fixtures) {
            String key = key(f.homeTeamName(), f.awayTeamName());
            map.put(key, f);
        }
        latestFixtureMap = Map.copyOf(map);
    }

    /**
     * Broadcast latest snapshot to all rounds that currently have SSE subscribers.
     */
    public void broadcastAllSubscribedRounds() {
        for (Long roundId : emittersByRound.keySet()) {
            broadcastRound(roundId);
        }
    }

    /**
     * Broadcast snapshot for one round.
     */
    public void broadcastRound(long roundId) {
        CopyOnWriteArrayList<SseEmitter> emitters = emittersByRound.get(roundId);
        if (emitters == null || emitters.isEmpty()) return;

        LiveRoundSnapshot snapshot = buildSnapshot(roundId);

        for (SseEmitter emitter : emitters) {
            safeSend(roundId, emitter, snapshot);
        }
    }

    private void safeSend(long roundId, SseEmitter emitter, LiveRoundSnapshot snapshot) {
        try {
            emitter.send(SseEmitter.event()
                    .name("snapshot")
                    .data(snapshot, MediaType.APPLICATION_JSON));
        } catch (IOException | IllegalStateException ex) {
            // Client disconnected or emitter already completed
            removeAndComplete(roundId, emitter, true);
        } catch (Exception ex) {
            // Any other send failure: treat as dead connection
            removeAndComplete(roundId, emitter, true);
        }
    }

    private LiveRoundSnapshot buildSnapshot(long roundId) {
        List<MatchEntity> matches = matchRepository.findByRoundIdOrderByMatchNumberAsc(roundId);

        List<LiveMatchUpdate> updates = new ArrayList<>(matches.size());
        Map<String, ApiFootballClient.LiveFixture> map = latestFixtureMap;

        for (MatchEntity m : matches) {
            String home = m.getHomeTeamName();
            String away = m.getAwayTeamName();

            ApiFootballClient.LiveFixture fixture = map.get(key(home, away));

            if (fixture == null) {
                updates.add(new LiveMatchUpdate(
                        m.getMatchNumber(),
                        null,
                        null,
                        null,
                        null,
                        null
                ));
            } else {
                updates.add(new LiveMatchUpdate(
                        m.getMatchNumber(),
                        fixture.fixtureId(),
                        fixture.homeGoals(),
                        fixture.awayGoals(),
                        fixture.statusShort(),
                        fixture.elapsedMinutes()
                ));
            }
        }

        return new LiveRoundSnapshot(roundId, Instant.now().toString(), updates);
    }

    private void removeAndComplete(long roundId, SseEmitter emitter, boolean callComplete) {
        CopyOnWriteArrayList<SseEmitter> list = emittersByRound.get(roundId);
        if (list != null) {
            list.remove(emitter);
            if (list.isEmpty()) {
                emittersByRound.remove(roundId);
            }
        }
        if (callComplete) {
            try { emitter.complete(); } catch (Exception ignore) {}
        }
    }

    private static String key(String home, String away) {
        return normalize(home) + "||" + normalize(away);
    }

    private static String normalize(String s) {
        if (s == null) return "";
        return s.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * One snapshot message sent to clients.
     */
    public record LiveRoundSnapshot(
            long roundId,
            String updatedAt,
            List<LiveMatchUpdate> matches
    ) { }

    /**
     * Per match number: score/status/minute if found; else all null (no match found).
     */
    public record LiveMatchUpdate(
            int matchNumber,
            Long fixtureId,
            Integer homeGoals,
            Integer awayGoals,
            String status,
            Integer minute
    ) { }
}