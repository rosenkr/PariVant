package ar.ss.betting.controllers.authenticated;

import ar.ss.betting.security.UserEntity;
import ar.ss.betting.services.CouponService;
import ar.ss.betting.services.dto.CouponRequest;
import ar.ss.betting.services.dto.CouponResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/coupons")
public class CouponController {

    private final CouponService couponService;

    public CouponController(CouponService couponService) {
        this.couponService = Objects.requireNonNull(couponService);
    }

    @PostMapping
    public ResponseEntity<CouponResponse> createCoupon(
            @AuthenticationPrincipal UserEntity user,
            @RequestBody CouponRequest request
    ) {
        CouponResponse created = couponService.createCoupon(user, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    public ResponseEntity<List<CouponResponse>> getCoupons(
            @AuthenticationPrincipal UserEntity user
    ) {
        return ResponseEntity.ok(couponService.getCouponsForUser(user));
    }

    @GetMapping("/{couponId}")
    public ResponseEntity<CouponResponse> getCoupon(
            @AuthenticationPrincipal UserEntity user,
            @PathVariable long couponId
    ) {
        return ResponseEntity.ok(couponService.getCouponForUser(user, couponId));
    }
}
