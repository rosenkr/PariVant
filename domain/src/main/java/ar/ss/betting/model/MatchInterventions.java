package ar.ss.betting.model;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class MatchInterventions {

    private final List<MatchTag> tags;
    private final MatchBuff buff;

    public MatchInterventions(List<MatchTag> tags, MatchBuff buff) {
        this.tags = List.copyOf(Objects.requireNonNull(tags, "tags cannot be null"));
        this.buff = buff;

        for (MatchTag tag : this.tags) {
            Objects.requireNonNull(tag, "tags cannot contain null");
        }
    }

    public static MatchInterventions empty() {
        return new MatchInterventions(List.of(), null);
    }

    public List<MatchTag> getTags() {
        return tags;
    }

    public Optional<MatchBuff> getBuff() {
        return Optional.ofNullable(buff);
    }

    public int totalBuffPoints() {
        return getBuff().map(MatchBuff::points).orElse(0);
    }

    public boolean isEmpty() {
        return tags.isEmpty() && buff == null;
    }
}