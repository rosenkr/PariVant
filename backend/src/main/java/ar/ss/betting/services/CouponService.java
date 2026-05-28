package ar.ss.betting.services;

import ar.ss.betting.domain.Coupon;
import ar.ss.betting.domain.CouponStatus;
import ar.ss.betting.domain.Outcome;
import ar.ss.betting.domain.RoundStatus;
import ar.ss.betting.domain.RoundType;
import ar.ss.betting.persistence.entity.CouponEntity;
import ar.ss.betting.persistence.entity.RoundEntity;
import ar.ss.betting.persistence.repo.CouponRepository;
import ar.ss.betting.persistence.repo.RoundRepository;
import ar.ss.betting.security.UserEntity;
import ar.ss.betting.services.dto.CouponRequest;
import ar.ss.betting.services.dto.CouponResponse;
import ar.ss.betting.util.JsonUtil;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;

@Service
public class CouponService {

    private final CouponRepository couponRepository;
    private final RoundRepository roundRepository;
    private final Clock clock;

    public CouponService(CouponRepository couponRepository,
                         RoundRepository roundRepository,
                         Clock clock) {
        this.couponRepository = Objects.requireNonNull(couponRepository);
        this.roundRepository = Objects.requireNonNull(roundRepository);
        this.clock = Objects.requireNonNull(clock);
    }

    @Transactional
    public CouponResponse createCoupon(UserEntity user, CouponRequest request) {
        Objects.requireNonNull(request, "request cannot be null");
        Long userId = requireUserId(user);

        RoundEntity round = roundRepository.findById(request.roundId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Round not found"));

        if (round.getStatus() != RoundStatus.UPCOMING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Coupons can only be created for upcoming rounds");
        }
        if (couponRepository.existsByUser_IdAndRound_Id(userId, round.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "User already has a coupon for this round");
        }

        Coupon domainCoupon = new Coupon(
                userId,
                round.getId(),
                round.getRoundType(),
                toDomainSelections(request.selections())
        );

        Instant now = Instant.now(clock);
        CouponEntity saved = couponRepository.save(new CouponEntity(
                user,
                round,
                JsonUtil.toJson(toSelectionsJsonShape(domainCoupon.getSelections())),
                now
        ));

        return toView(saved);
    }

    @Transactional
    public List<CouponResponse> getCouponsForUser(UserEntity user) {
        Long userId = requireUserId(user);
        return couponRepository.findByUser_IdOrderByCreatedAtDesc(userId).stream()
                .map(this::toView)
                .toList();
    }

    private Long requireUserId(UserEntity user) {
        if (user == null || user.getId() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        return user.getId();
    }

    private Map<Integer, Set<Outcome>> toDomainSelections(Map<Integer, List<String>> input) {
        Objects.requireNonNull(input, "selections cannot be null");

        Map<Integer, Set<Outcome>> out = new LinkedHashMap<>();
        for (Map.Entry<Integer, List<String>> entry : input.entrySet()) {
            Integer matchNumber = Objects.requireNonNull(entry.getKey(), "selection match number cannot be null");
            List<String> rawOutcomes = Objects.requireNonNull(
                    entry.getValue(),
                    "selection outcomes cannot be null for match " + matchNumber
            );

            EnumSet<Outcome> outcomes = EnumSet.noneOf(Outcome.class);
            for (String rawOutcome : rawOutcomes) {
                if (rawOutcome == null || rawOutcome.isBlank()) {
                    throw new IllegalArgumentException("selection outcome cannot be blank for match " + matchNumber);
                }
                outcomes.add(Outcome.valueOf(rawOutcome));
            }

            out.put(matchNumber, outcomes);
        }

        return out;
    }

    private Map<String, List<String>> toSelectionsJsonShape(Map<Integer, Set<Outcome>> selections) {
        Map<String, List<String>> out = new TreeMap<>();

        for (Map.Entry<Integer, Set<Outcome>> entry : selections.entrySet()) {
            List<String> outcomes = new ArrayList<>();
            for (Outcome outcome : Outcome.values()) {
                if (entry.getValue().contains(outcome)) {
                    outcomes.add(outcome.name());
                }
            }
            out.put(String.valueOf(entry.getKey()), outcomes);
        }

        return out;
    }

    private CouponResponse toView(CouponEntity entity) {
        RoundEntity round = entity.getRound();
        return new CouponResponse(
                entity.getId(),
                round.getId(),
                round.getRoundType(),
                entity.getStatus(),
                entity.getCorrectPickCount(),
                JsonUtil.parseJsonToObject(entity.getSelectionsJson()),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
