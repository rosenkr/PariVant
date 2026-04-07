package ar.ss.betting.persistence.repo;

import ar.ss.betting.domain.RoundStatus;
import ar.ss.betting.domain.RoundType;
import ar.ss.betting.persistence.entity.RoundEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface RoundRepository extends JpaRepository<RoundEntity, Long> {

    boolean existsById(long id);

    boolean existsByRoundTypeAndStartDate(RoundType roundType, LocalDateTime startDate);

    Optional<RoundEntity> findByRoundTypeAndStartDate(RoundType roundType, LocalDateTime startDate);
    List<RoundEntity> findByStatusOrderByStartDateAsc(RoundStatus status);
    List<RoundEntity> findByRoundTypeAndStatusOrderByStartDateAsc(RoundType roundType,
                                                                  RoundStatus status);

    List<RoundEntity> findByStatusInAndStartDateBetweenOrderByStartDateAsc(List<RoundStatus> statuses,
                                                                           LocalDateTime from,
                                                                           LocalDateTime to);

    List<RoundEntity> findByStatusAndStartDateLessThanEqual(RoundStatus status, LocalDateTime time);

    List<RoundEntity> findByStatus(RoundStatus status);
}