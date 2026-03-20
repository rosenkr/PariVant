package ar.ss.betting.persistence.repo;

import ar.ss.betting.persistence.entity.MatchEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MatchRepository extends JpaRepository<MatchEntity, Long> {

    List<MatchEntity> findByGameRoundIdOrderByMatchNumberAsc(Long gameRoundId);
}
