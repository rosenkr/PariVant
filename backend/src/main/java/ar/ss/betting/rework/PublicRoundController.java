package ar.ss.betting.rework;

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

import java.time.LocalDateTime;
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

    @GetMapping("/rounds")
    public ResponseEntity<List<RoundView>> getRounds(
            @RequestParam("roundType") String roundTypeParam,
            @RequestParam("status") String statusParam
    ) {
        RoundType roundType = RoundType.valueOf(roundTypeParam);
        RoundStatus status = RoundStatus.valueOf(statusParam);

        List<RoundEntity> rounds =
                roundRepository.findByRoundTypeAndStatusOrderByStartDateAsc(roundType, status);

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
                round.getStartDate(),
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
                match.getStartDate(),
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
            TripleView publicPick,
            boolean marketFallbackUsed,
            String marketFallbackReason
    ) { }
}