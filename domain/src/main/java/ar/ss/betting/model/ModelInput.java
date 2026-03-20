package ar.ss.betting.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Round-level model input.
 *
 * Holds:
 * - base match contexts
 * - optional per-match interventions (tags and/or buff)
 *
 * Base runs can use the single-argument constructor, which defaults to no interventions.
 */
public class ModelInput {

    private final Map<Integer, MatchContext> matchContexts;
    private final Map<Integer, MatchInterventions> matchInterventions;

    public ModelInput(Map<Integer, MatchContext> matchContexts) {
        this(matchContexts, Map.of());
    }

    public ModelInput(Map<Integer, MatchContext> matchContexts,
                      Map<Integer, MatchInterventions> matchInterventions) {

        Objects.requireNonNull(matchContexts, "matchContexts cannot be null");
        Objects.requireNonNull(matchInterventions, "matchInterventions cannot be null");

        this.matchContexts = Map.copyOf(matchContexts);

        Map<Integer, MatchInterventions> copiedInterventions = new HashMap<>();
        for (Map.Entry<Integer, MatchInterventions> entry : matchInterventions.entrySet()) {
            Objects.requireNonNull(entry.getKey(), "matchInterventions cannot contain null keys");
            copiedInterventions.put(
                    entry.getKey(),
                    Objects.requireNonNull(entry.getValue(), "matchInterventions cannot contain null values")
            );
        }
        this.matchInterventions = Map.copyOf(copiedInterventions);
    }

    public MatchContext getMatchContext(int matchNumber) {
        MatchContext ctx = matchContexts.get(matchNumber);
        if (ctx == null) {
            throw new IllegalArgumentException("Missing MatchContext for match number " + matchNumber);
        }
        return ctx;
    }

    public MatchInterventions getMatchInterventions(int matchNumber) {
        return matchInterventions.getOrDefault(matchNumber, MatchInterventions.empty());
    }

    public Map<Integer, MatchContext> getMatchContexts() {
        return matchContexts;
    }

    public Map<Integer, MatchInterventions> getMatchInterventions() {
        return matchInterventions;
    }
}