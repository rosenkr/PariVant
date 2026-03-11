package ar.ss.betting.persistence.repo;

import ar.ss.betting.persistence.entity.MatchContextEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MatchContextRepository extends JpaRepository<MatchContextEntity, Long> {
    List<MatchContextEntity> findByGameRoundIdOrderByMatchNumberAsc(long gameRoundId);
    boolean existsByGameRoundId(long gameRoundId);
}