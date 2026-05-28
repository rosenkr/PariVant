package ar.ss.betting.persistence.repo;

import ar.ss.betting.persistence.entity.CouponEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CouponRepository extends JpaRepository<CouponEntity, Long> {

    boolean existsByUserIdAndRoundId(long userId, long roundId);

    List<CouponEntity> findByUserIdOrderByCreatedAtDesc(long userId);

    Optional<CouponEntity> findByIdAndUserId(long id, long userId);
}
