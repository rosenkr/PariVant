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
 *   - BaseOutcomeSelector (value logic using internal probs + public distribution)
 *
 * CoverageEngine:
 *   - Uncertainty-based ranking (using internal probabilities)
 *   - Expansion using internal probabilities
 */
public class RuleBasedModel implements GameModel {

    private final BaseOutcomeSelector baseOutcomeSelector;
    private final InternalProbabilityCalculator probabilityCalculator;
    private final AdjustmentWeights weights;

    public RuleBasedModel(AdjustmentWeights weights) {
        this.baseOutcomeSelector = new BaseOutcomeSelector();
        this.probabilityCalculator = new InternalProbabilityCalculator();
        this.weights = Objects.requireNonNull(weights);
    }

    public RuleBasedModel() {
        this(AdjustmentWeights.none());
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
                            ctx.getPublicProbabilities()
                    );

            basePicks.put(matchNumber, base);
        }

        Map<Integer, Set<Outcome>> selections = basePicks.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> Set.of(e.getValue())
                ));

        int halfGuardsToUse = computeHalfGuards(gameRound.getMatches().size(), maxBudgetInSek);

        // ---- Layer 2: coverage allocation ----
        List<Integer> rankedMatches = rankByUncertainty(internalProbs);

        int appliedHalfGuards = 0;
        for (Integer matchNumber : rankedMatches) {

            if (appliedHalfGuards >= halfGuardsToUse) break;

            Outcome base = basePicks.get(matchNumber);
            ProbabilityTriple internal = internalProbs.get(matchNumber);

            selections.put(matchNumber, expandToHalfGuard(base, internal));
            appliedHalfGuards++;
        }

        int totalCost = 1 << appliedHalfGuards;

        return new ModelSelectionResult(
                "RuleBasedModel",
                LocalDateTime.now(),
                selections,
                totalCost,
                appliedHalfGuards,
                0
        );
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

    private Set<Outcome> expandToHalfGuard(Outcome base,
                                           ProbabilityTriple internal) {

        Outcome bestAlt = bestAlternative(base, internal);

        LinkedHashSet<Outcome> set = new LinkedHashSet<>();
        set.add(base);
        set.add(bestAlt);

        return Collections.unmodifiableSet(set);
    }

    private Outcome bestAlternative(Outcome base,
                                    ProbabilityTriple internal) {

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
        int half = 0;
        int cost = 1;

        while (half < numberOfMatches && cost * 2 <= budget) {
            cost *= 2;
            half++;
        }

        return half;
    }
}