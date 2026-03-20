package ar.ss.betting.model;

import java.util.Objects;
import java.util.Optional;

public class MatchTag {

    private final TagType type;
    private final Side affectedSide;

    private MatchTag(TagType type, Side affectedSide) {
        this.type = Objects.requireNonNull(type, "type cannot be null");
        this.affectedSide = affectedSide;

        if (type == TagType.NEUTRAL_VENUE && affectedSide != null) {
            throw new IllegalArgumentException("NEUTRAL_VENUE must not have an affected side");
        }

        if (type != TagType.NEUTRAL_VENUE && affectedSide == null) {
            throw new IllegalArgumentException(type + " requires an affected side");
        }
    }

    public static MatchTag neutralVenue() {
        return new MatchTag(TagType.NEUTRAL_VENUE, null);
    }

    public static MatchTag keyAbsence(Side affectedSide) {
        return new MatchTag(TagType.KEY_ABSENCE, affectedSide);
    }

    public static MatchTag shortRest(Side affectedSide) {
        return new MatchTag(TagType.SHORT_REST, affectedSide);
    }

    public static MatchTag incentiveLack(Side affectedSide) {
        return new MatchTag(TagType.INCENTIVE_LACK, affectedSide);
    }

    public TagType getType() {
        return type;
    }

    public Optional<Side> getAffectedSide() {
        return Optional.ofNullable(affectedSide);
    }
}