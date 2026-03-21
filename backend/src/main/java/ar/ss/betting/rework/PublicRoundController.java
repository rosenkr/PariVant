package ar.ss.betting.rework;

import ar.ss.betting.domain.RoundStatus;
import ar.ss.betting.domain.RoundType;
import ar.ss.betting.persistence.entity.MatchContextEntity;
import ar.ss.betting.persistence.entity.MatchEntity;
import ar.ss.betting.persistence.entity.RoundEntity;
import ar.ss.betting.persistence.repo.MatchContextRepository;
import ar.ss.betting.persistence.repo.MatchRepository;
import ar.ss.betting.persistence.repo.RoundRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/public")
public class PublicRoundController {

    private static final List<RoundType> DEFAULT_FALLBACK_ORDER =
            List.of(RoundType.STRYKTIPSET, RoundType.EUROPATIPSET, RoundType.TOPPTIPSET);

    private final RoundRepository roundRepository;
    private final MatchRepository matchRepository;
    private final MatchContextRepository matchContextRepository;

    public PublicRoundController(RoundRepository roundRepository,
                                 MatchRepository matchRepository,
                                 MatchContextRepository matchContextRepository) {
        this.roundRepository = Objects.requireNonNull(roundRepository);
        this.matchRepository = Objects.requireNonNull(matchRepository);
        this.matchContextRepository = Objects.requireNonNull(matchContextRepository);
    }

    @GetMapping("/current")
    public ResponseEntity<CurrentRoundResponse> getCurrent(
            @RequestParam(name = "roundType", required = false) String roundTypeParam
    ) {
        if (roundTypeParam != null && !roundTypeParam.isBlank()) {
            RoundType requested = RoundType.valueOf(roundTypeParam);
            SelectedRound selected = selectForType(requested);
            if (selected == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(toResponse(requested, selected));
        }

        for (RoundType type : DEFAULT_FALLBACK_ORDER) {
            SelectedRound selected = selectForType(type);
            if (selected != null) {
                return ResponseEntity.ok(toResponse(type, selected));
            }
        }

        return ResponseEntity.notFound().build();
    }

    private SelectedRound selectForType(RoundType type) {
        List<RoundEntity> running =
                roundRepository.findByRoundTypeAndStatusOrderByStartDateAsc(
                        type,
                        RoundStatus.RUNNING,
                        PageRequest.of(0, 1)
                );
        if (!running.isEmpty()) {
            return new SelectedRound(running.get(0), RoundStatus.RUNNING);
        }

        List<RoundEntity> upcoming =
                roundRepository.findByRoundTypeAndStatusOrderByStartDateAsc(
                        type,
                        RoundStatus.UPCOMING,
                        PageRequest.of(0, 1)
                );
        if (!upcoming.isEmpty()) {
            return new SelectedRound(upcoming.get(0), RoundStatus.UPCOMING);
        }

        return null;
    }

    private CurrentRoundResponse toResponse(RoundType selectedRoundType, SelectedRound selected) {
        RoundEntity round = selected.round();

        List<MatchEntity> matchEntities =
                matchRepository.findByRoundIdOrderByMatchNumberAsc(round.getId());

        List<MatchContextEntity> ctxEntities =
                matchContextRepository.findByRoundIdOrderByMatchNumberAsc(round.getId());

        Map<Integer, MatchContextEntity> ctxByMatch = new HashMap<>();
        for (MatchContextEntity c : ctxEntities) {
            ctxByMatch.put(c.getMatchNumber(), c);
        }

        List<MatchView> matches = new ArrayList<>(matchEntities.size());
        for (MatchEntity m : matchEntities) {
            MatchContextEntity c = ctxByMatch.get(m.getMatchNumber());

            TripleView market = null;
            TripleView pub = null;

            if (c != null) {
                market = new TripleView(c.getMarketHome(), c.getMarketDraw(), c.getMarketAway());
                pub = new TripleView(c.getPublicHome(), c.getPublicDraw(), c.getPublicAway());
            }

            matches.add(new MatchView(
                    m.getMatchNumber(),
                    m.getStartDate(),
                    m.getHomeTeamName(),
                    m.getAwayTeamName(),
                    market,
                    pub
            ));
        }

        RoundView roundView = new RoundView(
                round.getId(),
                round.getStartDate(),
                matches
        );

        return new CurrentRoundResponse(selectedRoundType, selected.status(), roundView);
    }

    private record SelectedRound(RoundEntity round, RoundStatus status) { }

    public record CurrentRoundResponse(
            RoundType selectedRoundType,
            RoundStatus roundStatus,
            RoundView round
    ) { }

    public record RoundView(
            long id,
            LocalDateTime startDate,
            List<MatchView> matches
    ) { }

    public record TripleView(
            double homeWin,
            double draw,
            double awayWin
    ) { }

    public record MatchView(
            int matchNumber,
            LocalDateTime startDate,
            String homeTeamName,
            String awayTeamName,
            TripleView market,
            TripleView publicPick
    ) { }
}