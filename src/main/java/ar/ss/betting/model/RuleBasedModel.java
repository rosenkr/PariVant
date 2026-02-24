package ar.ss.betting.model;

import ar.ss.betting.domain.GameRound;
import ar.ss.betting.domain.Match;
import ar.ss.betting.domain.Outcome;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Rule-based model with two layers:
 *
 * Layer 1 (Base pick):
 * - Choose exactly one outcome for each match using BaseOutcomeSelector.
 *
 * Layer 2 (Coverage allocation):
 * - Spend budget by expanding some matches from single -> half guard (2 outcomes).
 *
 * Step C.2 changes:
 * - Coverage ranking uses uncertainty derived from internal probabilities.
 * - expandToHalfGuard chooses the best alternative outcome based on internal probabilities.
 *
 * For now: internal probabilities == market probabilities.
 * Later: internal probabilities will be adjusted using form, injuries, weather, etc. with user weights.
 */
public class RuleBasedModel implements GameModel {

    private final BaseOutcomeSelector baseOutcomeSelector;

    public RuleBasedModel() {
        this(new BaseOutcomeSelector());
    }

    public RuleBasedModel(BaseOutcomeSelector baseOutcomeSelector) {
        this.baseOutcomeSelector = Objects.requireNonNull(baseOutcomeSelector);
    }

    @Override
    public ModelSelectionResult generateSelection(GameRound gameRound, ModelInput modelInput, int maxBudgetInSek) {

        Objects.requireNonNull(gameRound, "gameRound cannot be null");
        Objects.requireNonNull(modelInput, "modelInput cannot be null");

        if (maxBudgetInSek <= 0) {
            throw new IllegalArgumentException("Budget must be positive");
        }

        // ---- Layer 1: base picks (all singles) ----
        Map<Integer, Outcome> basePicks = new HashMap<>();
        for (Match match : gameRound.getMatches()) {
            MatchContext ctx = modelInput.getMatchContext(match.getMatchNumber());
            Outcome base = baseOutcomeSelector.chooseBaseOutcome(gameRound.getGameType(), ctx);
            basePicks.put(match.getMatchNumber(), base);
        }

        // Selections start as singles
        Map<Integer, Set<Outcome>> selections = basePicks.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> Set.of(e.getValue())
                ));

        int halfGuardsToUse = computeHalfGuards(gameRound.getMatches().size(), maxBudgetInSek);

        // ---- Layer 2: allocate half guards based on uncertainty ----
        List<Match> rankedForCoverage = rankMatchesForCoverage(gameRound, modelInput);

        int appliedHalfGuards = 0;
        for (Match match : rankedForCoverage) {
            if (appliedHalfGuards >= halfGuardsToUse) {
                break;
            }

            int matchNumber = match.getMatchNumber();
            Outcome base = basePicks.get(matchNumber);
            MatchContext ctx = modelInput.getMatchContext(matchNumber);

            selections.put(matchNumber, expandToHalfGuard(base, ctx));
            appliedHalfGuards++;
        }

        int totalCost = 1 << appliedHalfGuards; // 2^halfGuards

        return new ModelSelectionResult(
                "RuleBasedModel",
                LocalDateTime.now(),
                selections,
                totalCost,
                appliedHalfGuards,
                0
        );
    }

    /**
     * Coverage allocation ranking:
     * - Higher uncertainty means higher priority to add coverage.
     *
     * uncertainty = 1 - max_o p_i(o)
     *
     * For now p_i == market probabilities.
     * Later p_i will be adjusted probabilities from many factors with weights.
     */
    protected List<Match> rankMatchesForCoverage(GameRound gameRound, ModelInput modelInput) {
        List<Match> matches = new ArrayList<>(gameRound.getMatches());

        matches.sort((a, b) -> {
            double ua = uncertainty(a.getMatchNumber(), modelInput);
            double ub = uncertainty(b.getMatchNumber(), modelInput);
            int cmp = Double.compare(ub, ua); // descending uncertainty
            if (cmp != 0) return cmp;
            return Integer.compare(a.getMatchNumber(), b.getMatchNumber()); // deterministic tie-break
        });

        return matches;
    }

    protected double uncertainty(int matchNumber, ModelInput modelInput) {
        MatchContext ctx = modelInput.getMatchContext(matchNumber);
        ProbabilityTriple internal = getInternalProbabilities(ctx);

        double max = Math.max(internal.get(Outcome.HOME_WIN),
                Math.max(internal.get(Outcome.DRAW), internal.get(Outcome.AWAY_WIN)));

        return 1.0 - max;
    }

    /**
     * Step C.2: Choose the second outcome for a half-guard based on internal probabilities.
     *
     * For now: internal == market.
     * Later: internal will include adjustments and may prefer a different "second best".
     */
    protected Set<Outcome> expandToHalfGuard(Outcome baseOutcome, MatchContext ctx) {
        ProbabilityTriple internal = getInternalProbabilities(ctx);

        Outcome bestAlt = bestAlternativeOutcome(baseOutcome, internal);

        // Use LinkedHashSet to keep deterministic iteration order (base first).
        LinkedHashSet<Outcome> set = new LinkedHashSet<>();
        set.add(baseOutcome);
        set.add(bestAlt);
        return Collections.unmodifiableSet(set);
    }

    protected Outcome bestAlternativeOutcome(Outcome baseOutcome, ProbabilityTriple internal) {
        Outcome best = null;
        double bestP = Double.NEGATIVE_INFINITY;

        for (Outcome o : List.of(Outcome.HOME_WIN, Outcome.DRAW, Outcome.AWAY_WIN)) {
            if (o == baseOutcome) continue;
            double p = internal.get(o);
            if (p > bestP) {
                bestP = p;
                best = o;
            }
        }

        // Should never be null because there are 3 outcomes.
        return Objects.requireNonNull(best);
    }

    /**
     * Internal probability source.
     *
     * Step C.2 baseline: return market probabilities.
     * Future: return adjusted probabilities (market + form/injuries/weather/etc using weights).
     */
    protected ProbabilityTriple getInternalProbabilities(MatchContext ctx) {
        return ctx.getMarketProbabilities();
    }

    private int computeHalfGuards(int numberOfMatches, int budget) {
        int half = 0;
        int cost = 1;

        while (half < numberOfMatches && cost * 2 <= budget) {
            cost *= 2;
            half++;
        }

        return half;
    }
}