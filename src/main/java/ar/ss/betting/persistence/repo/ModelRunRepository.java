package ar.ss.betting.persistence.repo;

import ar.ss.betting.persistence.entity.ModelRunEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ModelRunRepository extends JpaRepository<ModelRunEntity, Long> {

    List<ModelRunEntity> findByGameRoundIdOrderByGeneratedAtDesc(Long gameRoundId);
}