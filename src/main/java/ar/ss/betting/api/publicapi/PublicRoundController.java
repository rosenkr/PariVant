package ar.ss.betting.api.publicapi;

import ar.ss.betting.domain.GameType;
import ar.ss.betting.persistence.entity.GameRoundEntity;
import ar.ss.betting.persistence.entity.MatchEntity;
import ar.ss.betting.persistence.repo.GameRoundRepository;
import ar.ss.betting.persistence.repo.MatchRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/public")
public class PublicRoundController {

    private static final List<GameType> DEFAULT_FALLBACK_ORDER =
            List.of(GameType.STRYKTIPSET, GameType.EUROPATIPSET, GameType.TOPPTIPSET);

    private final GameRoundRepository gameRoundRepository;
    private final MatchRepository matchRepository;

    public PublicRoundController(GameRoundRepository gameRoundRepository,
                                 MatchRepository matchRepository) {
        this.gameRoundRepository = Objects.requireNonNull(gameRoundRepository);
        this.matchRepository = Objects.requireNonNull(matchRepository);
    }

    @GetMapping("/current")
    public ResponseEntity<CurrentRoundResponse> getCurrent(@RequestParam(name = "gameType", required = false) String gameTypeParam) {

        LocalDateTime now = LocalDateTime.now();

        if (gameTypeParam != null && !gameTypeParam.isBlank()) {
            GameType requested = GameType.valueOf(gameTypeParam);
            SelectedRound selected = selectForType(requested, now);
            if (selected == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(toResponse(requested, selected));
        }

        for (GameType type : DEFAULT_FALLBACK_ORDER) {
            SelectedRound selected = selectForType(type, now);
            if (selected != null) {
                return ResponseEntity.ok(toResponse(type, selected));
            }
        }

        return ResponseEntity.notFound().build();
    }

    private SelectedRound selectForType(GameType type, LocalDateTime now) {
        List<GameRoundEntity> running = gameRoundRepository.findRunningByGameType(type, now, PageRequest.of(0, 1));
        if (!running.isEmpty()) {
            return new SelectedRound(running.get(0), RoundStatus.RUNNING);
        }

        List<GameRoundEntity> upcoming = gameRoundRepository.findNextUpcomingByGameType(type, now, PageRequest.of(0, 1));
        if (!upcoming.isEmpty()) {
            return new SelectedRound(upcoming.get(0), RoundStatus.UPCOMING);
        }

        return null;
    }

    private CurrentRoundResponse toResponse(GameType selectedGameType, SelectedRound selected) {
        GameRoundEntity round = selected.round();

        List<MatchEntity> matchEntities = matchRepository.findByGameRoundIdOrderByMatchNumberAsc(round.getId());

        List<MatchView> matches = new ArrayList<>(matchEntities.size());
        for (MatchEntity m : matchEntities) {
            matches.add(new MatchView(
                    m.getMatchNumber(),
                    m.getStartDate(),
                    m.getHomeTeamName(),
                    m.getAwayTeamName()
            ));
        }

        RoundView roundView = new RoundView(
                round.getId(),
                round.getStartDate(),
                round.getEndDate(),
                matches
        );

        return new CurrentRoundResponse(selectedGameType, selected.status(), roundView);
    }

    private record SelectedRound(GameRoundEntity round, RoundStatus status) { }

    public enum RoundStatus {
        UPCOMING,
        RUNNING
    }

    public record CurrentRoundResponse(
            GameType selectedGameType,
            RoundStatus roundStatus,
            RoundView round
    ) { }

    public record RoundView(
            long id,
            LocalDateTime startDate,
            LocalDateTime endDate,
            List<MatchView> matches
    ) { }

    public record MatchView(
            int matchNumber,
            LocalDateTime startDate,
            String homeTeamName,
            String awayTeamName
    ) { }
}