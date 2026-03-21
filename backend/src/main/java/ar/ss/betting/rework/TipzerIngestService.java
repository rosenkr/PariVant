package ar.ss.betting.rework;

import ar.ss.betting.domain.RoundStatus;
import ar.ss.betting.domain.RoundType;
import ar.ss.betting.persistence.entity.MatchContextEntity;
import ar.ss.betting.persistence.entity.MatchEntity;
import ar.ss.betting.persistence.entity.RoundEntity;
import ar.ss.betting.persistence.repo.MatchContextRepository;
import ar.ss.betting.persistence.repo.RoundRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class TipzerIngestService {

    private final TipzerClient tipzerClient;
    private final TipzerParser tipzerParser;
    private final TipzerTopptipsetParser tipzerTopptipsetParser;
    private final RoundRepository roundRepository;
    private final MatchContextRepository matchContextRepository;

    public TipzerIngestService(
            TipzerClient tipzerClient,
            TipzerParser tipzerParser,
            TipzerTopptipsetParser tipzerTopptipsetParser,
            RoundRepository roundRepository,
            MatchContextRepository matchContextRepository
    ) {
        this.tipzerClient = Objects.requireNonNull(tipzerClient);
        this.tipzerParser = Objects.requireNonNull(tipzerParser);
        this.tipzerTopptipsetParser = Objects.requireNonNull(tipzerTopptipsetParser);
        this.roundRepository = Objects.requireNonNull(roundRepository);
        this.matchContextRepository = Objects.requireNonNull(matchContextRepository);
    }

    @Transactional
    public IngestResult ingestNextStryktipsetRound() {
        String teamsRaw = tipzerClient.getStryktipsetTeamsRaw();
        String svfRaw = tipzerClient.getStryktipsetSvenskaFolketRaw();
        String oddsRaw = tipzerClient.getStryktipsetOddsRaw();

        TipzerParser.TipzerSnapshot snapshot = tipzerParser.parse(teamsRaw, svfRaw, oddsRaw);
        return ingestFromSnapshot(RoundType.STRYKTIPSET, snapshot);
    }

    @Transactional
    public IngestResult ingestNextEuropatipsetRound() {
        String teamsRaw = tipzerClient.getEuropatipsetTeamsRaw();
        String svfRaw = tipzerClient.getEuropatipsetSvenskaFolketRaw();
        String oddsRaw = tipzerClient.getEuropatipsetOddsRaw();

        TipzerParser.TipzerSnapshot snapshot = tipzerParser.parse(teamsRaw, svfRaw, oddsRaw);
        return ingestFromSnapshot(RoundType.EUROPATIPSET, snapshot);
    }

    @Transactional
    public IngestResult ingestNextTopptipsetRound() {
        String html = tipzerClient.getTopptipsetPageRaw();
        TipzerTopptipsetParser.TopptipsetSnapshot snapshot = tipzerTopptipsetParser.parseFromPageHtml(html);

        RoundType roundType = RoundType.TOPPTIPSET;
        LocalDateTime roundStart = snapshot.roundStart().toLocalDateTime();

        boolean exists = roundRepository.existsByRoundTypeAndStartDate(roundType, roundStart);
        if (exists) {
            return IngestResult.duplicate(roundType, roundStart);
        }

        RoundEntity roundEntity = new RoundEntity(roundType, RoundStatus.UPCOMING, roundStart);

        for (TipzerTopptipsetParser.TopptipsetMatch m : snapshot.matches()) {
            MatchEntity matchEntity = new MatchEntity(
                    m.matchNumber(),
                    m.kickoff().toLocalDateTime(),
                    m.home(),
                    m.away()
            );
            roundEntity.addMatch(matchEntity);
        }

        RoundEntity savedRound = roundRepository.save(roundEntity);
        long roundId = savedRound.getId();

        List<MatchContextEntity> contexts = new ArrayList<>(snapshot.matches().size());

        for (int i = 0; i < snapshot.matches().size(); i++) {
            int matchNumber = i + 1;

            TipzerTopptipsetParser.Triple pub = snapshot.publicPick().get(i);
            TipzerTopptipsetParser.Triple market = snapshot.marketPick().get(i);

            contexts.add(new MatchContextEntity(
                    roundId,
                    matchNumber,
                    market.home(),
                    market.draw(),
                    market.away(),
                    pub.home(),
                    pub.draw(),
                    pub.away()
            ));
        }

        matchContextRepository.saveAll(contexts);

        return IngestResult.created(roundType, roundStart, roundId);
    }

    private IngestResult ingestFromSnapshot(RoundType roundType, TipzerParser.TipzerSnapshot snapshot) {
        Objects.requireNonNull(roundType, "roundType");
        Objects.requireNonNull(snapshot, "snapshot");

        LocalDateTime roundStart = snapshot.roundStart().toLocalDateTime();

        boolean exists = roundRepository.existsByRoundTypeAndStartDate(roundType, roundStart);
        if (exists) {
            return IngestResult.duplicate(roundType, roundStart);
        }

        RoundEntity roundEntity = new RoundEntity(roundType, RoundStatus.UPCOMING, roundStart);

        for (TipzerParser.TipzerMatch m : snapshot.matches()) {
            MatchEntity matchEntity = new MatchEntity(
                    m.matchNumber(),
                    m.kickoff().toLocalDateTime(),
                    m.home(),
                    m.away()
            );
            roundEntity.addMatch(matchEntity);
        }

        RoundEntity savedRound = roundRepository.save(roundEntity);
        long roundId = savedRound.getId();

        List<MatchContextEntity> contexts = new ArrayList<>(snapshot.matches().size());

        for (int i = 0; i < snapshot.matches().size(); i++) {
            int matchNumber = i + 1;

            TipzerParser.TipzerTriple svf = snapshot.svf().get(i);
            TipzerParser.TipzerTriple odds = snapshot.odds().get(i);

            double publicHome = svf.home();
            double publicDraw = svf.draw();
            double publicAway = svf.away();

            double marketHome = odds.home();
            double marketDraw = odds.draw();
            double marketAway = odds.away();

            contexts.add(new MatchContextEntity(
                    roundId,
                    matchNumber,
                    marketHome,
                    marketDraw,
                    marketAway,
                    publicHome,
                    publicDraw,
                    publicAway
            ));
        }

        matchContextRepository.saveAll(contexts);

        return IngestResult.created(roundType, roundStart, roundId);
    }

    public record IngestResult(
            String status,
            RoundType roundType,
            LocalDateTime roundStartDate,
            Long roundId
    ) {
        public static IngestResult created(RoundType roundType, LocalDateTime start, long roundId) {
            return new IngestResult("CREATED", roundType, start, roundId);
        }

        public static IngestResult duplicate(RoundType roundType, LocalDateTime start) {
            return new IngestResult("DUPLICATE", roundType, start, null);
        }
    }
}