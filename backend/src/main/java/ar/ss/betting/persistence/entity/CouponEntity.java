package ar.ss.betting.persistence.entity;

import ar.ss.betting.domain.CouponStatus;
import ar.ss.betting.security.UserEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(
        name = "coupon",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_coupon_user_round",
                        columnNames = {"user_id", "round_id"}
                )
        },
        indexes = {
                @Index(name = "idx_coupon_user_id", columnList = "user_id"),
                @Index(name = "idx_coupon_round_id", columnList = "round_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CouponEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "round_id", nullable = false)
    private RoundEntity round;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private CouponStatus status;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "selections_json", nullable = false, columnDefinition = "jsonb")
    private String selectionsJson;

    @Column(name = "correct_pick_count")
    private Integer correctPickCount;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public CouponEntity(UserEntity user,
                        RoundEntity round,
                        String selectionsJson,
                        Instant createdAt) {
        this(
                user,
                round,
                CouponStatus.UNDETERMINED,
                selectionsJson,
                null,
                createdAt,
                createdAt
        );
    }

    public CouponEntity(UserEntity user,
                        RoundEntity round,
                        CouponStatus status,
                        String selectionsJson,
                        Integer correctPickCount,
                        Instant createdAt,
                        Instant updatedAt) {
        this.user = Objects.requireNonNull(user, "user cannot be null");
        this.round = Objects.requireNonNull(round, "round cannot be null");
        this.status = Objects.requireNonNull(status, "status cannot be null");
        this.selectionsJson = requireNonBlank(selectionsJson, "selectionsJson");
        this.correctPickCount = correctPickCount;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt cannot be null");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt cannot be null");

        validateResolutionState();
    }

    public void resolve(CouponStatus status, int correctPickCount, Instant updatedAt) {
        if (status == CouponStatus.UNDETERMINED) {
            throw new IllegalArgumentException("Resolved coupon status must be WIN or LOSE");
        }
        if (correctPickCount < 0) {
            throw new IllegalArgumentException("correctPickCount cannot be negative");
        }

        this.status = status;
        this.correctPickCount = correctPickCount;
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt cannot be null");
    }

    private void validateResolutionState() {
        if (status == CouponStatus.UNDETERMINED) {
            if (correctPickCount != null) {
                throw new IllegalArgumentException("Undetermined coupon cannot have correctPickCount");
            }
            return;
        }

        if (correctPickCount == null) {
            throw new IllegalArgumentException("Resolved coupon must have correctPickCount");
        }
        if (correctPickCount < 0) {
            throw new IllegalArgumentException("correctPickCount cannot be negative");
        }
    }

    private static String requireNonBlank(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " cannot be blank");
        }
        return value;
    }
}
