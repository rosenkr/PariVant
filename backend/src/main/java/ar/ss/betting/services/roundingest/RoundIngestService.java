package ar.ss.betting.services.roundingest;

import ar.ss.betting.domain.RoundStatus;
import ar.ss.betting.domain.RoundType;
import ar.ss.betting.persistence.entity.MatchContextEntity;
import ar.ss.betting.persistence.entity.MatchEntity;
import ar.ss.betting.persistence.entity.RoundEntity;
import ar.ss.betting.persistence.repo.MatchContextRepository;
import ar.ss.betting.persistence.repo.RoundRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Objects;

@Service
public class RoundIngestService {

    private final RoundRepository roundRepository;
    private final MatchContextRepository matchContextRepository;

    public RoundIngestService(RoundRepository roundRepository,
                              MatchContextRepository matchContextRepository) {
        this.roundRepository = Objects.requireNonNull(roundRepository);
        this.matchContextRepository = Objects.requireNonNull(matchContextRepository);
    }

    @Transactional
    public PersistResult persistIfNew(ar.ss.betting.roundingest.IngestedRound ingestedRound) {
        Objects.requireNonNull(ingestedRound, "ingestedRound");

        Instant roundStartTime = ingestedRound.roundStart();

        boolean exists = roundRepository.existsByRoundTypeAndStartTime(
                ingestedRound.roundType(),
                roundStartTime
        );

        if (exists) {
            return PersistResult.duplicate(ingestedRound.roundType(), roundStartTime);
        }

        RoundEntity roundEntity = new RoundEntity(
                ingestedRound.roundType(),
                RoundStatus.UPCOMING,
                roundStartTime
        );

        for (ar.ss.betting.roundingest.IngestedMatch m : ingestedRound.matches()) {
            MatchEntity matchEntity = new MatchEntity(
                    m.matchNumber(),
                    m.kickoff(),
                    m.homeTeamName(),
                    m.awayTeamName()
            );
            roundEntity.addMatch(matchEntity);
        }

        RoundEntity savedRound = roundRepository.save(roundEntity);

        for (ar.ss.betting.roundingest.IngestedMatch m : ingestedRound.matches()) {
            MatchContextEntity ctx = new MatchContextEntity(
                    savedRound.getId(),
                    m.matchNumber(),
                    m.market().homeWin(),
                    m.market().draw(),
                    m.market().awayWin(),
                    m.publicPick().homeWin(),
                    m.publicPick().draw(),
                    m.publicPick().awayWin(),
                    m.marketFallbackUsed(),
                    m.marketFallbackReason()
            );
            matchContextRepository.save(ctx);
        }

        return PersistResult.created(
                ingestedRound.roundType(),
                roundStartTime,
                savedRound.getId()
        );
    }

    public record PersistResult(
            String status,
            RoundType roundType,
            Instant roundStartTime,
            Long roundId
    ) {
        public static PersistResult created(RoundType roundType, Instant startTime, long roundId) {
            return new PersistResult("CREATED", roundType, startTime, roundId);
        }

        public static PersistResult duplicate(RoundType roundType, Instant startTime) {
            return new PersistResult("DUPLICATE", roundType, startTime, null);
        }
    }
}