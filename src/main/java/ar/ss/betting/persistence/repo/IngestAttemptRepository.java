package ar.ss.betting.persistence.repo;

import ar.ss.betting.persistence.entity.IngestAttemptEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface IngestAttemptRepository extends CrudRepository<IngestAttemptEntity, Long> {

    @Query("select a from IngestAttemptEntity a order by a.createdAt desc")
    List<IngestAttemptEntity> findLatest(Pageable pageable);

    @Query("select a from IngestAttemptEntity a where a.status = :status order by a.createdAt desc")
    List<IngestAttemptEntity> findLatestByStatus(String status, Pageable pageable);
}