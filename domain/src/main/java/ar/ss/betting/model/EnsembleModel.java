package ar.ss.betting.model;

import ar.ss.betting.domain.GameRound;
import ar.ss.betting.domain.Match;
import ar.ss.betting.domain.Outcome;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Ensemble betting model.
 *
 * Pipeline:
 * 1. ensemble internal probabilities from market + providers
 * 2. optional runtime overlays (tags and/or buff)
 * 3. score-based base pick selection
 * 4. uncertainty-based half-guard allocation
 */
public class EnsembleModel implements GameModel {

    private final BaseOutcomeSelector baseOutcomeSelector;
    private final InternalProbabilityCalculator probabilityCalculator;

    public EnsembleModel() {
        this.baseOutcomeSelector = new BaseOutcomeSelector();
        this.probabilityCalculator = new InternalProbabilityCalculator();
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

        validateBuffQuota(gameRound, modelInput);

        Map<Integer, ProbabilityTriple> internalProbs = new HashMap<>();
        Map<Integer, Outcome> basePicks = new HashMap<>();

        for (Match match : gameRound.getMatches()) {
            int matchNumber = match.getMatchNumber();

            MatchContext ctx = modelInput.getMatchContext(matchNumber);
            MatchInterventions interventions = modelInput.getMatchInterventions(matchNumber);

            ProbabilityTriple internal = probabilityCalculator.calculateInternalProbabilities(ctx, interventions);
            internalProbs.put(matchNumber, internal);

            Outcome base = baseOutcomeSelector.chooseBaseOutcome(
                    internal,
                    ctx.getPublicProbabilities()
            );

            basePicks.put(matchNumber, base);
        }

        Map<Integer, Set<Outcome>> selections = basePicks.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> new LinkedHashSet<>(Set.of(e.getValue()))
                ));

        int halfGuardsToUse = computeHalfGuards(gameRound.getMatches().size(), maxBudgetInSek);
        List<Integer> rankedMatches = rankByUncertainty(internalProbs);

        int appliedHalfGuards = 0;

        for (Integer matchNumber : rankedMatches) {
            if (appliedHalfGuards >= halfGuardsToUse) {
                break;
            }

            Outcome base = basePicks.get(matchNumber);
            ProbabilityTriple internal = internalProbs.get(matchNumber);

            selections.put(matchNumber, expandToHalfGuard(base, internal));
            appliedHalfGuards++;
        }

        int totalCost = computeTotalCost(selections);

        return new ModelSelectionResult(
                "EnsembleModel",
                LocalDateTime.now(),
                freezeSelections(selections),
                freezeInternalProbabilities(internalProbs),
                totalCost,
                countHalfGuards(selections),
                0
        );
    }

    private void validateBuffQuota(GameRound gameRound, ModelInput modelInput) {
        int usedBuffPoints = modelInput.getMatchInterventions().values().stream()
                .mapToInt(MatchInterventions::totalBuffPoints)
                .sum();

        int roundSize = gameRound.getMatches().size();
        int maxAllowed = roundSize * ModelConstants.BUFF_POINTS_PER_MATCH_IN_ROUND;

        if (usedBuffPoints > maxAllowed) {
            throw new IllegalArgumentException(
                    "Buff quota exceeded for round. Used " + usedBuffPoints + ", allowed " + maxAllowed
            );
        }
    }

    private Map<Integer, Set<Outcome>> freezeSelections(Map<Integer, Set<Outcome>> selections) {
        Map<Integer, Set<Outcome>> frozen = new HashMap<>();
        for (Map.Entry<Integer, Set<Outcome>> e : selections.entrySet()) {
            frozen.put(e.getKey(), Collections.unmodifiableSet(new LinkedHashSet<>(e.getValue())));
        }
        return Collections.unmodifiableMap(frozen);
    }

    private Map<Integer, ProbabilityTriple> freezeInternalProbabilities(Map<Integer, ProbabilityTriple> internalProbabilities) {
        return Collections.unmodifiableMap(new HashMap<>(internalProbabilities));
    }

    private int countHalfGuards(Map<Integer, Set<Outcome>> selections) {
        int c = 0;
        for (Set<Outcome> s : selections.values()) {
            if (s.size() == 2) {
                c++;
            }
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
            if (cmp != 0) {
                return cmp;
            }
            return Integer.compare(a, b);
        });

        return matchNumbers;
    }

    private double uncertainty(ProbabilityTriple internal) {
        double max = Math.max(
                internal.get(Outcome.HOME_WIN),
                Math.max(internal.get(Outcome.DRAW), internal.get(Outcome.AWAY_WIN))
        );
        return 1.0 - max;
    }

    private Set<Outcome> expandToHalfGuard(Outcome base, ProbabilityTriple internal) {
        Outcome bestAlt = bestAlternative(base, internal);

        LinkedHashSet<Outcome> set = new LinkedHashSet<>();
        set.add(base);
        set.add(bestAlt);

        return set;
    }

    private Outcome bestAlternative(Outcome base, ProbabilityTriple internal) {
        Outcome best = null;
        double bestP = Double.NEGATIVE_INFINITY;

        for (Outcome o : Outcome.values()) {
            if (o == base) {
                continue;
            }
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