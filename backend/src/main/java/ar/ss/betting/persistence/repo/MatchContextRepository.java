package ar.ss.betting.persistence.repo;

import ar.ss.betting.persistence.entity.MatchContextEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MatchContextRepository extends JpaRepository<MatchContextEntity, Long> {

    // For a round, get all its match contexts for each match
    List<MatchContextEntity> findByRoundIdOrderByMatchNumberAsc(long roundId);

    boolean existsByRoundId(long roundId);
}