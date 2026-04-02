package ar.ss.betting.persistence.repo;

import ar.ss.betting.domain.MatchStatus;
import ar.ss.betting.persistence.entity.MatchEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface MatchRepository extends JpaRepository<MatchEntity, Long> {

    List<MatchEntity> findByRoundIdOrderByMatchNumberAsc(Long roundId);

    List<MatchEntity> findByStatusAndStartDateLessThanEqual(MatchStatus status, LocalDateTime time);
}