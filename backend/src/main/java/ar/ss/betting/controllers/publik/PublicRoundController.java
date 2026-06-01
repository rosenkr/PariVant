package ar.ss.betting.controllers.publik;

import ar.ss.betting.domain.RoundStatus;
import ar.ss.betting.domain.RoundType;
import ar.ss.betting.persistence.entity.MatchContextEntity;
import ar.ss.betting.persistence.entity.MatchEntity;
import ar.ss.betting.persistence.entity.RoundEntity;
import ar.ss.betting.persistence.repo.MatchContextRepository;
import ar.ss.betting.persistence.repo.MatchRepository;
import ar.ss.betting.persistence.repo.RoundRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.*;

@RestController
@RequestMapping("/public")
public class PublicRoundController {

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

    // Ex #1: on site enter, get all "status = upcoming" rounds (for all types), so frontend can choose which to show
    // Ex #2: on navigating from upcoming to live, get all "status = running"
    @GetMapping("/rounds")
    public ResponseEntity<List<RoundView>> getRounds(
            @RequestParam("status") String statusParam,
            @RequestParam(value = "roundType", required = false) String roundTypeParam
    ) {
        RoundStatus status = RoundStatus.valueOf(statusParam);

        List<RoundEntity> rounds;
        if (roundTypeParam == null || roundTypeParam.isBlank()) {
            rounds = roundRepository.findByStatusOrderByStartTimeAsc(status);
        } else {
            RoundType roundType = RoundType.valueOf(roundTypeParam);
            rounds = roundRepository.findByRoundTypeAndStatusOrderByStartTimeAsc(roundType, status);
        }

        List<RoundView> response = rounds.stream()
                .map(this::toRoundView)
                .toList();

        return ResponseEntity.ok(response);
    }

    private RoundView toRoundView(RoundEntity round) {
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
            matches.add(toMatchView(m, c));
        }

        return new RoundView(
                round.getId(),
                round.getRoundType().name(),
                round.getStartTime(),
                matches
        );
    }

    private MatchView toMatchView(MatchEntity match, MatchContextEntity context) {
        TripleView market = null;
        TripleView publicPick = null;
        boolean marketFallbackUsed = false;
        String marketFallbackReason = null;

        if (context != null) {
            market = new TripleView(
                    context.getMarketHome(),
                    context.getMarketDraw(),
                    context.getMarketAway()
            );
            publicPick = new TripleView(
                    context.getPublicHome(),
                    context.getPublicDraw(),
                    context.getPublicAway()
            );
            marketFallbackUsed = context.isMarketFallbackUsed();
            marketFallbackReason = context.getMarketFallbackReason();
        }

        return new MatchView(
                match.getMatchNumber(),
                match.getStartTime(),
                match.getHomeTeamName(),
                match.getAwayTeamName(),
                market,
                publicPick,
                marketFallbackUsed,
                marketFallbackReason
        );
    }

    public record RoundView(
            long id,
            String roundType,
            Instant startTime,
            List<MatchView> matches
    ) { }

    public record TripleView(
            double homeWin,
            double draw,
            double awayWin
    ) { }

    public record MatchView(
            int matchNumber,
            Instant startTime,
            String homeTeamName,
            String awayTeamName,
            TripleView market,
            TripleView publicPick,
            boolean marketFallbackUsed,
            String marketFallbackReason
    ) { }
}