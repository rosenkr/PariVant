package ar.ss.betting.persistence.repo;

import ar.ss.betting.domain.RoundStatus;
import ar.ss.betting.domain.RoundType;
import ar.ss.betting.persistence.entity.RoundEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface RoundRepository extends JpaRepository<RoundEntity, Long> {

    boolean existsById(long id);

    boolean existsByRoundTypeAndStartTime(RoundType roundType, Instant startTime);

    Optional<RoundEntity> findByRoundTypeAndStartTime(RoundType roundType, Instant startTime);

    List<RoundEntity> findByStatusOrderByStartTimeAsc(RoundStatus status);

    List<RoundEntity> findByRoundTypeAndStatusOrderByStartTimeAsc(RoundType roundType,
                                                                  RoundStatus status);

    List<RoundEntity> findByStatusInAndStartTimeBetweenOrderByStartTimeAsc(List<RoundStatus> statuses,
                                                                           Instant from,
                                                                           Instant to);

    List<RoundEntity> findByStatusAndStartTimeLessThanEqual(RoundStatus status, Instant time);

    List<RoundEntity> findByStatus(RoundStatus status);
}