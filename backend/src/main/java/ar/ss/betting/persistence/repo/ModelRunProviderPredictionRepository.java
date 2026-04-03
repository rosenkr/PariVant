package ar.ss.betting.persistence.repo;

import ar.ss.betting.persistence.entity.ModelRunProviderPredictionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ModelRunProviderPredictionRepository extends JpaRepository<ModelRunProviderPredictionEntity, Long> {

    List<ModelRunProviderPredictionEntity> findByModelRunIdOrderByMatchNumberAscProviderNameAsc(long modelRunId);
}