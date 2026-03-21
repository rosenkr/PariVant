package ar.ss.betting.persistence.repo;

import ar.ss.betting.persistence.entity.ModelRunEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ModelRunRepository extends JpaRepository<ModelRunEntity, Long> {

    // Return all model runs for a round
    List<ModelRunEntity> findByRoundIdOrderByGeneratedAtDesc(long roundId);

    boolean existsByRoundIdAndBudgetInSekAndTrigger(long roundId, int budgetInSek, String trigger);
}