package ar.ss.betting.persistence.repo;

import ar.ss.betting.persistence.entity.CouponEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CouponRepository extends JpaRepository<CouponEntity, Long> {

    boolean existsByUser_IdAndRound_Id(long userId, long roundId);

    List<CouponEntity> findByUser_IdOrderByCreatedAtDesc(long userId);

    Optional<CouponEntity> findByIdAndUser_Id(long id, long userId);
}
