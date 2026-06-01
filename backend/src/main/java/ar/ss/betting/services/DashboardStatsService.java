package ar.ss.betting.services;

import ar.ss.betting.domain.CouponStatus;
import ar.ss.betting.persistence.entity.CouponEntity;
import ar.ss.betting.persistence.repo.CouponRepository;
import ar.ss.betting.security.UserEntity;
import ar.ss.betting.services.dto.DashboardStatsResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Objects;

@Service
public class DashboardStatsService {

    private final CouponRepository couponRepository;

    public DashboardStatsService(CouponRepository couponRepository) {
        this.couponRepository = Objects.requireNonNull(couponRepository);
    }

    public DashboardStatsResponse getStats(UserEntity user) {
        Long userId = requireUserId(user);
        List<CouponEntity> coupons = couponRepository.findByUser_IdOrderByCreatedAtDesc(userId);

        int couponWins = 0;
        int couponLosses = 0;
        int couponUndetermined = 0;
        int confidentPickUndetermined = 0;

        for (CouponEntity coupon : coupons) {
            if (coupon.getStatus() == CouponStatus.WIN) {
                couponWins++;
            } else if (coupon.getStatus() == CouponStatus.LOSE) {
                couponLosses++;
            } else {
                couponUndetermined++;
            }

            if (coupon.getConfidentPickMatchNumber() != null) {
                confidentPickUndetermined++;
            }
        }

        return new DashboardStatsResponse(
                new DashboardStatsResponse.StatBlock(couponWins, couponLosses, couponUndetermined),
                new DashboardStatsResponse.StatBlock(0, 0, confidentPickUndetermined)
        );
    }

    private Long requireUserId(UserEntity user) {
        if (user == null || user.getId() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        return user.getId();
    }
}
