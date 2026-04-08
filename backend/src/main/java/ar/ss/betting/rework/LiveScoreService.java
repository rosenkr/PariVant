package ar.ss.betting.rework;

import ar.ss.betting.domain.MatchStatus;
import ar.ss.betting.domain.RoundStatus;
import ar.ss.betting.matchresolver.MatchIdentityCandidate;
import ar.ss.betting.matchresolver.MatchResolver;
import ar.ss.betting.matchresolver.RequestedMatchIdentity;
import ar.ss.betting.persistence.entity.MatchEntity;
import ar.ss.betting.persistence.entity.RoundEntity;
import ar.ss.betting.persistence.repo.MatchRepository;
import ar.ss.betting.persistence.repo.RoundRepository;
import jakarta.transaction.Transactional;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class LiveScoreService {

    private static final long EMITTER_TIMEOUT_MS = 5 * 60 * 1000L; // 5 minutes
    private static final long ROUND_FINISH_FALLBACK_MINUTES = 150;

    private final MatchRepository matchRepository;
    private final RoundRepository roundRepository;
    private final MatchResolver matchResolver;

    private final Map<Long, CopyOnWriteArrayList<SseEmitter>> emittersByRound = new ConcurrentHashMap<>();
    private volatile List<ApiFootballClient.LiveFixture> latestFixtures = List.of();

    public LiveScoreService(MatchRepository matchRepository,
                            RoundRepository roundRepository,
                            MatchResolver matchResolver) {
        this.matchRepository = Objects.requireNonNull(matchRepository);
        this.roundRepository = Objects.requireNonNull(roundRepository);
        this.matchResolver = Objects.requireNonNull(matchResolver);
    }

    public SseEmitter subscribe(long roundId) {
        SseEmitter emitter = new SseEmitter(EMITTER_TIMEOUT_MS);

        emittersByRound.computeIfAbsent(roundId, __ -> new CopyOnWriteArrayList<>()).add(emitter);

        emitter.onCompletion(() -> removeAndComplete(roundId, emitter, false));
        emitter.onTimeout(() -> removeAndComplete(roundId, emitter, true));
        emitter.onError((e) -> removeAndComplete(roundId, emitter, true));

        safeSend(roundId, emitter, buildSnapshot(roundId));

        return emitter;
    }

    @Transactional
    public void syncStatusesFromTime() {
        Instant now = Instant.now();

        List<RoundEntity> roundsToStart =
                roundRepository.findByStatusAndStartTimeLessThanEqual(RoundStatus.UPCOMING, now);
        for (RoundEntity round : roundsToStart) {
            round.setStatus(RoundStatus.RUNNING);
        }

        List<MatchEntity> matchesToStart =
                matchRepository.findByStatusAndStartTimeLessThanEqual(MatchStatus.UPCOMING, now);
        for (MatchEntity match : matchesToStart) {
            match.setStatus(MatchStatus.RUNNING);
        }

        finalizeRunningRounds(now);
    }

    public void updateFixtures(List<ApiFootballClient.LiveFixture> fixtures) {
        this.latestFixtures = List.copyOf(fixtures);
    }

    @Transactional
    public void applyLiveScoresToPersistedMatches() {
        if (latestFixtures.isEmpty()) {
            return;
        }

        List<MatchEntity> runningMatches =
                matchRepository.findByStatusAndStartTimeLessThanEqual(MatchStatus.RUNNING, Instant.now());

        List<LiveFixtureCandidate> candidates = latestFixtures.stream()
                .map(LiveFixtureCandidate::new)
                .toList();

        for (MatchEntity match : runningMatches) {
            Optional<LiveFixtureCandidate> resolved = resolveFixture(match, candidates);
            if (resolved.isEmpty()) {
                continue;
            }

            ApiFootballClient.LiveFixture fixture = resolved.get().fixture();

            if (fixture.homeGoals() != null) {
                match.setHomeScore(fixture.homeGoals());
            }
            if (fixture.awayGoals() != null) {
                match.setAwayScore(fixture.awayGoals());
            }

            String status = fixture.statusShort();
            if (status != null) {
                String upper = status.trim().toUpperCase(Locale.ROOT);
                switch (upper) {
                    case "FT", "AET", "PEN" -> match.setStatus(MatchStatus.FINISHED);
                    case "PST" -> match.setStatus(MatchStatus.POSTPONED);
                    case "CANC", "ABD", "AWD", "WO" -> match.setStatus(MatchStatus.CANCELLED);
                    default -> match.setStatus(MatchStatus.RUNNING);
                }
            }
        }

        finalizeRunningRounds(Instant.now());
    }

    public void broadcastAllSubscribedRounds() {
        for (Long roundId : emittersByRound.keySet()) {
            broadcastRound(roundId);
        }
    }

    public void broadcastRound(long roundId) {
        CopyOnWriteArrayList<SseEmitter> emitters = emittersByRound.get(roundId);
        if (emitters == null || emitters.isEmpty()) {
            return;
        }

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
        } catch (Exception ex) {
            removeAndComplete(roundId, emitter, true);
        }
    }

    private LiveRoundSnapshot buildSnapshot(long roundId) {
        List<MatchEntity> matches = matchRepository.findByRoundIdOrderByMatchNumberAsc(roundId);
        List<LiveFixtureCandidate> candidates = latestFixtures.stream()
                .map(LiveFixtureCandidate::new)
                .toList();

        List<LiveMatchUpdate> updates = new ArrayList<>(matches.size());

        for (MatchEntity match : matches) {
            Optional<LiveFixtureCandidate> resolved = resolveFixture(match, candidates);

            if (resolved.isEmpty()) {
                updates.add(new LiveMatchUpdate(
                        match.getMatchNumber(),
                        null,
                        null,
                        null,
                        match.getStatus() == MatchStatus.RUNNING ? "LIVE_DATA_UNAVAILABLE" : match.getStatus().name(),
                        null
                ));
                continue;
            }

            ApiFootballClient.LiveFixture fixture = resolved.get().fixture();

            updates.add(new LiveMatchUpdate(
                    match.getMatchNumber(),
                    fixture.fixtureId(),
                    fixture.homeGoals() != null ? fixture.homeGoals() : match.getHomeScore(),
                    fixture.awayGoals() != null ? fixture.awayGoals() : match.getAwayScore(),
                    fixture.statusShort() != null ? fixture.statusShort() : match.getStatus().name(),
                    fixture.elapsedMinutes()
            ));
        }

        return new LiveRoundSnapshot(roundId, Instant.now().toString(), updates);
    }

    private Optional<LiveFixtureCandidate> resolveFixture(MatchEntity match,
                                                          List<LiveFixtureCandidate> candidates) {
        RequestedMatchIdentity requested = new RequestedMatchIdentity(
                match.getHomeTeamName(),
                match.getAwayTeamName(),
                match.getStartTime()
        );

        return matchResolver.resolve(requested, candidates);
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
            try {
                emitter.complete();
            } catch (Exception ignore) {
            }
        }
    }

    private void finalizeRunningRounds(Instant now) {
        List<RoundEntity> runningRounds = roundRepository.findByStatus(RoundStatus.RUNNING);

        for (RoundEntity round : runningRounds) {
            List<MatchEntity> matches = matchRepository.findByRoundIdOrderByMatchNumberAsc(round.getId());

            if (matches.isEmpty()) {
                continue;
            }

            boolean anyCancelled = matches.stream()
                    .anyMatch(m -> m.getStatus() == MatchStatus.CANCELLED);
            if (anyCancelled) {
                round.setStatus(RoundStatus.UNSUPPORTED);
                continue;
            }

            boolean anyPostponed = matches.stream()
                    .anyMatch(m -> m.getStatus() == MatchStatus.POSTPONED);
            if (anyPostponed) {
                round.setStatus(RoundStatus.UNSUPPORTED);
                continue;
            }

            boolean allFinished = matches.stream()
                    .allMatch(m -> m.getStatus() == MatchStatus.FINISHED);
            if (allFinished) {
                round.setStatus(RoundStatus.ENDED);
                continue;
            }

            Instant latestKickoff = matches.stream()
                    .map(MatchEntity::getStartTime)
                    .filter(Objects::nonNull)
                    .max(Instant::compareTo)
                    .orElse(null);

            if (latestKickoff != null &&
                    !now.isBefore(latestKickoff.plusSeconds(ROUND_FINISH_FALLBACK_MINUTES * 60))) {
                round.setStatus(RoundStatus.ENDED);
            }
        }
    }

    private record LiveFixtureCandidate(ApiFootballClient.LiveFixture fixture) implements MatchIdentityCandidate {
        @Override
        public String getHomeTeam() {
            return fixture.homeTeamName();
        }

        @Override
        public String getAwayTeam() {
            return fixture.awayTeamName();
        }

        @Override
        public Instant getKickoff() {
            return null;
        }
    }

    public record LiveRoundSnapshot(
            long roundId,
            String updatedAt,
            List<LiveMatchUpdate> matches
    ) { }

    public record LiveMatchUpdate(
            int matchNumber,
            Long fixtureId,
            Integer homeGoals,
            Integer awayGoals,
            String status,
            Integer minute
    ) { }
}