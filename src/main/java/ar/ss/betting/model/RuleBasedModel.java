package ar.ss.betting.model;

import ar.ss.betting.domain.GameRound;
import ar.ss.betting.domain.Match;
import ar.ss.betting.domain.Outcome;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Rule-based betting model.
 *
 * TruthEngine:
 *   - InternalProbabilityCalculator (weighted signals -> internal probabilities)
 *
 * DecisionEngine:
 *   - BaseOutcomeSelector (value logic using internal probs + public distribution + decision parameters)
 *
 * CoverageEngine:
 *   - Uncertainty-based ranking (using internal probabilities)
 *   - Half guards (single -> 2 outcomes)
 *   - Full guards (upgrade half -> 3 outcomes) ONLY when budget slack allows, capped per game type
 */
public class RuleBasedModel implements GameModel {

    private final BaseOutcomeSelector baseOutcomeSelector;
    private final InternalProbabilityCalculator probabilityCalculator;
    private final AdjustmentWeights weights;
    private final DecisionParameters decisionParameters;

    public RuleBasedModel(AdjustmentWeights weights, DecisionParameters decisionParameters) {
        this.baseOutcomeSelector = new BaseOutcomeSelector();
        this.probabilityCalculator = new InternalProbabilityCalculator();
        this.weights = Objects.requireNonNull(weights);
        this.decisionParameters = Objects.requireNonNull(decisionParameters);
    }

    public RuleBasedModel(AdjustmentWeights weights) {
        this(weights, DecisionParameters.defaults());
    }

    public RuleBasedModel() {
        this(AdjustmentWeights.none(), DecisionParameters.defaults());
    }

    @Override
    public ModelSelectionResult generateSelection(GameRound gameRound,
                                                  ModelInput modelInput,
                                                  int maxBudgetInSek) {

        Objects.requireNonNull(gameRound, "gameRound cannot be null");
        Objects.requireNonNull(modelInput, "modelInput cannot be null");

        if (maxBudgetInSek <= 0) {
            throw new IllegalArgumentException("Budget must be positive");
        }

        Map<Integer, ProbabilityTriple> internalProbs = new HashMap<>();
        Map<Integer, Outcome> basePicks = new HashMap<>();

        // ---- Layer 1: compute internal probabilities + base picks ----
        for (Match match : gameRound.getMatches()) {

            int matchNumber = match.getMatchNumber();
            MatchContext ctx = modelInput.getMatchContext(matchNumber);

            ProbabilityTriple internal =
                    probabilityCalculator.calculateInternalProbabilities(ctx, weights);

            internalProbs.put(matchNumber, internal);

            Outcome base =
                    baseOutcomeSelector.chooseBaseOutcome(
                            gameRound.getGameType(),
                            internal,
                            ctx.getPublicProbabilities(),
                            decisionParameters
                    );

            basePicks.put(matchNumber, base);
        }

        Map<Integer, Set<Outcome>> selections = basePicks.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> new LinkedHashSet<>(Set.of(e.getValue()))
                ));

        // ---- Layer 2: allocate half guards ----
        int halfGuardsToUse = computeHalfGuards(gameRound.getMatches().size(), maxBudgetInSek);
        List<Integer> rankedMatches = rankByUncertainty(internalProbs);

        int appliedHalfGuards = 0;
        List<Integer> halfGuardedMatches = new ArrayList<>();

        for (Integer matchNumber : rankedMatches) {

            if (appliedHalfGuards >= halfGuardsToUse) break;

            Outcome base = basePicks.get(matchNumber);
            ProbabilityTriple internal = internalProbs.get(matchNumber);

            selections.put(matchNumber, expandToHalfGuard(base, internal));
            halfGuardedMatches.add(matchNumber);
            appliedHalfGuards++;
        }

        // ---- C4: upgrade half -> full guards using slack, capped ----
        int maxFullGuards = decisionParameters.maxFullGuards(gameRound.getGameType());
        int fullGuardsApplied = 0;

        // Rank the half-guarded matches by uncertainty (most uncertain first)
        halfGuardedMatches.sort((a, b) -> {
            double ua = uncertainty(internalProbs.get(a));
            double ub = uncertainty(internalProbs.get(b));
            int cmp = Double.compare(ub, ua);
            if (cmp != 0) return cmp;
            return Integer.compare(a, b);
        });

        boolean upgraded;
        do {
            upgraded = false;

            if (fullGuardsApplied >= maxFullGuards) break;

            int currentCost = computeTotalCost(selections);

            for (Integer matchNumber : halfGuardedMatches) {

                if (fullGuardsApplied >= maxFullGuards) break;

                Set<Outcome> currentSel = selections.get(matchNumber);

                // Only upgrade half-guards (size 2) to full-guards (size 3)
                if (currentSel.size() != 2) {
                    continue;
                }

                ProbabilityTriple internal = internalProbs.get(matchNumber);
                Set<Outcome> full = expandToFullGuard(currentSel, internal);

                // compute new cost if we apply this upgrade
                selections.put(matchNumber, full);
                int newCost = computeTotalCost(selections);

                if (newCost <= maxBudgetInSek) {
                    fullGuardsApplied++;
                    upgraded = true;
                    break; // recompute slack fresh
                } else {
                    // revert
                    selections.put(matchNumber, currentSel);
                }
            }

        } while (upgraded);

        int totalCost = computeTotalCost(selections);

        return new ModelSelectionResult(
                "RuleBasedModel",
                LocalDateTime.now(),
                freezeSelections(selections),
                totalCost,
                countHalfGuards(selections),
                countFullGuards(selections)
        );
    }

    private Map<Integer, Set<Outcome>> freezeSelections(Map<Integer, Set<Outcome>> selections) {
        Map<Integer, Set<Outcome>> frozen = new HashMap<>();
        for (Map.Entry<Integer, Set<Outcome>> e : selections.entrySet()) {
            frozen.put(e.getKey(), Collections.unmodifiableSet(new LinkedHashSet<>(e.getValue())));
        }
        return Collections.unmodifiableMap(frozen);
    }

    private int countHalfGuards(Map<Integer, Set<Outcome>> selections) {
        int c = 0;
        for (Set<Outcome> s : selections.values()) {
            if (s.size() == 2) c++;
        }
        return c;
    }

    private int countFullGuards(Map<Integer, Set<Outcome>> selections) {
        int c = 0;
        for (Set<Outcome> s : selections.values()) {
            if (s.size() == 3) c++;
        }
        return c;
    }

    private int computeTotalCost(Map<Integer, Set<Outcome>> selections) {
        int cost = 1;
        for (Set<Outcome> s : selections.values()) {
            cost *= s.size();
        }
        return cost;
    }

    private List<Integer> rankByUncertainty(Map<Integer, ProbabilityTriple> internalProbs) {
        List<Integer> matchNumbers = new ArrayList<>(internalProbs.keySet());

        matchNumbers.sort((a, b) -> {
            double ua = uncertainty(internalProbs.get(a));
            double ub = uncertainty(internalProbs.get(b));
            int cmp = Double.compare(ub, ua);
            if (cmp != 0) return cmp;
            return Integer.compare(a, b);
        });

        return matchNumbers;
    }

    private double uncertainty(ProbabilityTriple internal) {
        double max = Math.max(internal.get(Outcome.HOME_WIN),
                Math.max(internal.get(Outcome.DRAW), internal.get(Outcome.AWAY_WIN)));
        return 1.0 - max;
    }

    private Set<Outcome> expandToHalfGuard(Outcome base, ProbabilityTriple internal) {
        Outcome bestAlt = bestAlternative(base, internal);

        LinkedHashSet<Outcome> set = new LinkedHashSet<>();
        set.add(base);
        set.add(bestAlt);

        return set;
    }

    private Set<Outcome> expandToFullGuard(Set<Outcome> currentHalfGuard, ProbabilityTriple internal) {
        // currentHalfGuard has size 2; add the missing outcome
        LinkedHashSet<Outcome> set = new LinkedHashSet<>(currentHalfGuard);
        for (Outcome o : Outcome.values()) {
            if (!set.contains(o)) {
                set.add(o);
                break;
            }
        }
        return set;
    }

    private Outcome bestAlternative(Outcome base, ProbabilityTriple internal) {
        Outcome best = null;
        double bestP = Double.NEGATIVE_INFINITY;

        for (Outcome o : Outcome.values()) {
            if (o == base) continue;
            double p = internal.get(o);
            if (p > bestP) {
                bestP = p;
                best = o;
            }
        }
        return Objects.requireNonNull(best);
    }

    private int computeHalfGuards(int numberOfMatches, int budget) {
        // Keep Step B behavior: choose largest power of 2 <= budget (as half-guards count)
        int half = 0;
        int cost = 1;

        while (half < numberOfMatches && cost * 2 <= budget) {
            cost *= 2;
            half++;
        }

        return half;
    }
}