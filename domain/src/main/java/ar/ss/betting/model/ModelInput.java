package ar.ss.betting.model;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * Model-side input for a whole round.
 * Keyed by matchNumber.
 */
public class ModelInput {

    private final Map<Integer, MatchContext> matchContexts;

    public ModelInput(Map<Integer, MatchContext> matchContexts) {
        this.matchContexts = Map.copyOf(Objects.requireNonNull(matchContexts, "matchContexts cannot be null"));
    }

    public MatchContext getMatchContext(int matchNumber) {
        MatchContext ctx = matchContexts.get(matchNumber);
        if (ctx == null) {
            throw new IllegalArgumentException("Missing MatchContext for matchNumber=" + matchNumber);
        }
        return ctx;
    }

    public Map<Integer, MatchContext> getMatchContexts() {
        return Collections.unmodifiableMap(matchContexts);
    }
}