package ar.ss.betting.domain;

import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * A user-owned betting coupon for a specific round.
 *
 * A coupon belongs to a round identity and must contain exactly one non-empty
 * selection set for every expected match in that round type. Each selection
 * set may contain one, two or three outcomes, which naturally models singles,
 * doubles and triples.
 */
public class Coupon {

    private final long userId;
    private final long roundId;
    private final RoundType roundType;
    private final Map<Integer, Set<Outcome>> selections;
    private final CouponStatus status;
    private final Integer correctPickCount;
    private final Integer confidentPickMatchNumber;

    public Coupon(long userId, long roundId, RoundType roundType, Map<Integer, Set<Outcome>> selections) {
        this(userId, roundId, roundType, CouponStatus.UNDETERMINED, null, selections, null);
    }

    public Coupon(long userId,
                  long roundId,
                  RoundType roundType,
                  Map<Integer, Set<Outcome>> selections,
                  Integer confidentPickMatchNumber) {
        this(userId, roundId, roundType, CouponStatus.UNDETERMINED, null, selections, confidentPickMatchNumber);
    }

    public Coupon(long userId,
                  long roundId,
                  RoundType roundType,
                  CouponStatus status,
                  Integer correctPickCount,
                  Map<Integer, Set<Outcome>> selections) {
        this(userId, roundId, roundType, status, correctPickCount, selections, null);
    }

    public Coupon(long userId,
                  long roundId,
                  RoundType roundType,
                  CouponStatus status,
                  Integer correctPickCount,
                  Map<Integer, Set<Outcome>> selections,
                  Integer confidentPickMatchNumber) {
        if (userId <= 0) {
            throw new IllegalArgumentException("userId must be positive");
        }
        if (roundId <= 0) {
            throw new IllegalArgumentException("roundId must be positive");
        }

        this.userId = userId;
        this.roundId = roundId;
        this.roundType = Objects.requireNonNull(roundType, "roundType cannot be null");
        this.status = Objects.requireNonNull(status, "status cannot be null");
        this.correctPickCount = correctPickCount;
        this.confidentPickMatchNumber = confidentPickMatchNumber;
        this.selections = freezeSelections(
                Objects.requireNonNull(selections, "selections cannot be null")
        );

        validateSelections();
        validateConfidentPick();
        validateResolutionState();
    }

    public long getUserId() {
        return userId;
    }

    public long getRoundId() {
        return roundId;
    }

    public RoundType getRoundType() {
        return roundType;
    }

    public CouponStatus getStatus() {
        return status;
    }

    public Integer getCorrectPickCount() {
        return correctPickCount;
    }

    public Integer getConfidentPickMatchNumber() {
        return confidentPickMatchNumber;
    }

    public Map<Integer, Set<Outcome>> getSelections() {
        return selections;
    }

    public int getTotalCost() {
        int cost = 1;
        for (Set<Outcome> selection : selections.values()) {
            cost *= selection.size();
        }
        return cost;
    }

    public boolean isWinningCoupon() {
        return status == CouponStatus.WIN;
    }

    public Coupon resolve(Map<Integer, Outcome> actualOutcomes) {
        if (status != CouponStatus.UNDETERMINED) {
            throw new IllegalStateException("Coupon is already resolved");
        }

        Map<Integer, Outcome> frozenActualOutcomes = freezeActualOutcomes(actualOutcomes);
        validateActualOutcomes(frozenActualOutcomes);

        int hits = 0;
        for (Map.Entry<Integer, Set<Outcome>> entry : selections.entrySet()) {
            Outcome actualOutcome = frozenActualOutcomes.get(entry.getKey());
            if (entry.getValue().contains(actualOutcome)) {
                hits += 1;
            }
        }

        CouponStatus resolvedStatus = isWinningHitCount(hits) ? CouponStatus.WIN : CouponStatus.LOSE;
        return new Coupon(userId, roundId, roundType, resolvedStatus, hits, selections, confidentPickMatchNumber);
    }

    private Map<Integer, Set<Outcome>> freezeSelections(Map<Integer, Set<Outcome>> input) {
        Map<Integer, Set<Outcome>> frozen = new LinkedHashMap<>();

        for (Map.Entry<Integer, Set<Outcome>> entry : input.entrySet()) {
            Integer matchNumber = Objects.requireNonNull(entry.getKey(), "matchNumber cannot be null");
            Set<Outcome> outcomes = Objects.requireNonNull(entry.getValue(),
                    "selection set cannot be null for match " + matchNumber);

            if (outcomes.isEmpty()) {
                throw new IllegalArgumentException("selection set cannot be empty for match " + matchNumber);
            }

            EnumSet<Outcome> copy = EnumSet.noneOf(Outcome.class);
            for (Outcome outcome : outcomes) {
                copy.add(Objects.requireNonNull(outcome,
                        "selection contains null outcome for match " + matchNumber));
            }

            frozen.put(matchNumber, Collections.unmodifiableSet(copy));
        }

        return Collections.unmodifiableMap(frozen);
    }

    private Map<Integer, Outcome> freezeActualOutcomes(Map<Integer, Outcome> input) {
        Objects.requireNonNull(input, "actualOutcomes cannot be null");

        Map<Integer, Outcome> frozen = new LinkedHashMap<>();
        for (Map.Entry<Integer, Outcome> entry : input.entrySet()) {
            Integer matchNumber = Objects.requireNonNull(entry.getKey(), "matchNumber cannot be null");
            Outcome outcome = Objects.requireNonNull(entry.getValue(),
                    "actual outcome cannot be null for match " + matchNumber);
            frozen.put(matchNumber, outcome);
        }
        return Collections.unmodifiableMap(frozen);
    }

    private void validateSelections() {
        int expectedMatchCount = roundType.getNumberOfMatches();
        if (selections.size() != expectedMatchCount) {
            throw new IllegalArgumentException(
                    "Expected selections for " + expectedMatchCount
                            + " matches but got " + selections.size()
            );
        }

        for (Integer matchNumber : selections.keySet()) {
            if (!isValidMatchNumber(matchNumber)) {
                throw new IllegalArgumentException("Selection refers to unknown match number: " + matchNumber);
            }
        }

        for (int matchNumber = 1; matchNumber <= expectedMatchCount; matchNumber++) {
            if (!selections.containsKey(matchNumber)) {
                throw new IllegalArgumentException("Missing selection for match number: " + matchNumber);
            }
        }
    }

    private void validateResolutionState() {
        int maxCorrectPicks = roundType.getNumberOfMatches();

        if (status == CouponStatus.UNDETERMINED) {
            if (correctPickCount != null) {
                throw new IllegalArgumentException("Undetermined coupon cannot have correctPickCount");
            }
            return;
        }

        if (correctPickCount == null) {
            throw new IllegalArgumentException("Resolved coupon must have correctPickCount");
        }
        if (correctPickCount < 0 || correctPickCount > maxCorrectPicks) {
            throw new IllegalArgumentException(
                    "correctPickCount must be between 0 and " + maxCorrectPicks
            );
        }
    }

    private void validateConfidentPick() {
        if (confidentPickMatchNumber == null) {
            return;
        }
        if (!isValidMatchNumber(confidentPickMatchNumber)) {
            throw new IllegalArgumentException("confidentPickMatchNumber refers to unknown match number: "
                    + confidentPickMatchNumber);
        }
        if (!selections.containsKey(confidentPickMatchNumber) || selections.get(confidentPickMatchNumber).isEmpty()) {
            throw new IllegalArgumentException("confidentPickMatchNumber must refer to a selected match");
        }
    }

    private void validateActualOutcomes(Map<Integer, Outcome> actualOutcomes) {
        int expectedMatchCount = roundType.getNumberOfMatches();
        if (actualOutcomes.size() != expectedMatchCount) {
            throw new IllegalArgumentException(
                    "Expected actual outcomes for " + expectedMatchCount
                            + " matches but got " + actualOutcomes.size()
            );
        }

        for (Integer matchNumber : actualOutcomes.keySet()) {
            if (!isValidMatchNumber(matchNumber)) {
                throw new IllegalArgumentException("Actual outcomes contain unknown match number: " + matchNumber);
            }
        }

        for (int matchNumber = 1; matchNumber <= expectedMatchCount; matchNumber++) {
            if (!actualOutcomes.containsKey(matchNumber)) {
                throw new IllegalArgumentException("Missing actual outcome for match number: " + matchNumber);
            }
        }
    }

    private boolean isValidMatchNumber(int matchNumber) {
        return matchNumber >= 1 && matchNumber <= roundType.getNumberOfMatches();
    }

    private boolean isWinningHitCount(int hits) {
        return switch (roundType) {
            case TOPPTIPSET -> hits == roundType.getNumberOfMatches();
            case STRYKTIPSET, EUROPATIPSET -> hits >= 10;
        };
    }
}
