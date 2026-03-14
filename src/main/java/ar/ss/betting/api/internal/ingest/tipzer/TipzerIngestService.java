package ar.ss.betting.api.internal.ingest.tipzer;

import ar.ss.betting.domain.GameType;
import ar.ss.betting.persistence.entity.GameRoundEntity;
import ar.ss.betting.persistence.entity.MatchContextEntity;
import ar.ss.betting.persistence.entity.MatchEntity;
import ar.ss.betting.persistence.repo.GameRoundRepository;
import ar.ss.betting.persistence.repo.MatchContextRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class TipzerIngestService {

    private static final int DEFAULT_ROUND_DURATION_HOURS = 2;

    private final TipzerClient tipzerClient;
    private final TipzerParser tipzerParser;

    private final GameRoundRepository gameRoundRepository;
    private final MatchContextRepository matchContextRepository;

    public TipzerIngestService(
            TipzerClient tipzerClient,
            TipzerParser tipzerParser,
            GameRoundRepository gameRoundRepository,
            MatchContextRepository matchContextRepository
    ) {
        this.tipzerClient = Objects.requireNonNull(tipzerClient);
        this.tipzerParser = Objects.requireNonNull(tipzerParser);
        this.gameRoundRepository = Objects.requireNonNull(gameRoundRepository);
        this.matchContextRepository = Objects.requireNonNull(matchContextRepository);
    }

    @Transactional
    public IngestResult ingestNextStryktipsetRound() {
        String teamsRaw = tipzerClient.getStryktipsetTeamsRaw();
        String svfRaw = tipzerClient.getStryktipsetSvenskaFolketRaw();
        String oddsRaw = tipzerClient.getStryktipsetOddsRaw();

        TipzerParser.TipzerSnapshot snapshot = tipzerParser.parse(teamsRaw, svfRaw, oddsRaw);
        return ingestFromSnapshot(GameType.STRYKTIPSET, snapshot);
    }

    @Transactional
    public IngestResult ingestNextEuropatipsetRound() {
        String teamsRaw = tipzerClient.getEuropatipsetTeamsRaw();
        String svfRaw = tipzerClient.getEuropatipsetSvenskaFolketRaw();
        String oddsRaw = tipzerClient.getEuropatipsetOddsRaw();

        TipzerParser.TipzerSnapshot snapshot = tipzerParser.parse(teamsRaw, svfRaw, oddsRaw);
        return ingestFromSnapshot(GameType.EUROPATIPSET, snapshot);
    }

    private IngestResult ingestFromSnapshot(GameType gameType, TipzerParser.TipzerSnapshot snapshot) {
        Objects.requireNonNull(gameType, "gameType");
        Objects.requireNonNull(snapshot, "snapshot");

        // Tipzer gives OffsetDateTime; our DB/entities use LocalDateTime
        LocalDateTime roundStart = snapshot.roundStart().toLocalDateTime();
        LocalDateTime roundEnd = roundStart.plusHours(DEFAULT_ROUND_DURATION_HOURS);

        boolean exists = gameRoundRepository.existsByGameTypeAndStartDate(gameType, roundStart);
        if (exists) {
            return IngestResult.duplicate(gameType, roundStart);
        }

        // Persist round + matches (matches are cascaded from GameRoundEntity)
        GameRoundEntity roundEntity = new GameRoundEntity(gameType, roundStart, roundEnd);

        for (TipzerParser.TipzerMatch m : snapshot.matches()) {
            MatchEntity matchEntity = new MatchEntity(
                    m.matchNumber(),
                    m.kickoff().toLocalDateTime(),
                    m.home(),
                    m.away()
            );
            roundEntity.addMatch(matchEntity);
        }

        GameRoundEntity savedRound = gameRoundRepository.save(roundEntity);
        long roundId = savedRound.getId();

        // Persist match contexts (one per match number)
        List<MatchContextEntity> contexts = new ArrayList<>(snapshot.matches().size());

        for (int i = 0; i < snapshot.matches().size(); i++) {
            int matchNumber = i + 1;

            TipzerParser.TipzerTriple svf = snapshot.svf().get(i);
            TipzerParser.TipzerTriple odds = snapshot.odds().get(i);

            // TipzerParser already normalized % strings to probabilities 0..1
            double publicHome = svf.home();
            double publicDraw = svf.draw();
            double publicAway = svf.away();

            double marketHome = odds.home();
            double marketDraw = odds.draw();
            double marketAway = odds.away();

            // No recent-form provider yet => neutral defaults
            int homeForm = 5;
            int awayForm = 5;

            contexts.add(new MatchContextEntity(
                    roundId,
                    matchNumber,
                    marketHome,
                    marketDraw,
                    marketAway,
                    publicHome,
                    publicDraw,
                    publicAway,
                    homeForm,
                    awayForm
            ));
        }

        matchContextRepository.saveAll(contexts);

        return IngestResult.created(gameType, roundStart, roundId);
    }

    public record IngestResult(
            String status,
            GameType gameType,
            LocalDateTime roundStartDate,
            Long roundId
    ) {
        public static IngestResult created(GameType gameType, LocalDateTime start, long roundId) {
            return new IngestResult("CREATED", gameType, start, roundId);
        }

        public static IngestResult duplicate(GameType gameType, LocalDateTime start) {
            return new IngestResult("DUPLICATE", gameType, start, null);
        }
    }
}