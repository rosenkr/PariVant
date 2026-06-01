package ar.ss.betting.model;

import ar.ss.betting.domain.Match;
import ar.ss.betting.domain.Outcome;
import ar.ss.betting.domain.Round;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class ValueModel {

    private final String modelName;
    private final BaseOutcomeSelector baseOutcomeSelector;

    public ValueModel() {
        this("ValueModel");
    }

    public ValueModel(String modelName) {
        this.modelName = requireNonBlank(modelName, "modelName");
        this.baseOutcomeSelector = new BaseOutcomeSelector();
    }

    public ModelSelectionResult generateSelection(Round round,
                                                  ValueModelInput input,
                                                  int maxBudgetInSek,
                                                  Instant generatedAt) {
        Objects.requireNonNull(round, "round cannot be null");
        Objects.requireNonNull(input, "input cannot be null");
        Objects.requireNonNull(generatedAt, "generatedAt cannot be null");

        if (maxBudgetInSek <= 0) {
            throw new IllegalArgumentException("Budget must be positive");
        }

        Map<Integer, ProbabilityTriple> internalProbabilities = new HashMap<>();
        Map<Integer, Outcome> basePicks = new HashMap<>();

        for (Match match : round.getMatches()) {
            int matchNumber = match.getMatchNumber();
            ProbabilityTriple internal = input.getInternalProbabilities(matchNumber);
            ProbabilityTriple publicProbabilities = input.getPublicProbabilities(matchNumber);

            internalProbabilities.put(matchNumber, internal);
            basePicks.put(matchNumber, baseOutcomeSelector.chooseBaseOutcome(internal, publicProbabilities));
        }

        Map<Integer, Set<Outcome>> selections = basePicks.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> {
                            LinkedHashSet<Outcome> set = new LinkedHashSet<>();
                            set.add(entry.getValue());
                            return set;
                        }
                ));

        int halfGuardsToUse = computeHalfGuards(round.getMatches().size(), maxBudgetInSek);
        List<Integer> rankedMatches = rankByUncertainty(internalProbabilities);

        int appliedHalfGuards = 0;
        for (Integer matchNumber : rankedMatches) {
            if (appliedHalfGuards >= halfGuardsToUse) {
                break;
            }

            Outcome basePick = basePicks.get(matchNumber);
            ProbabilityTriple internal = internalProbabilities.get(matchNumber);

            selections.put(matchNumber, expandToHalfGuard(basePick, internal));
            appliedHalfGuards++;
        }

        return new ModelSelectionResult(
                modelName,
                generatedAt,
                basePicks,
                freezeSelections(selections),
                internalProbabilities,
                computeTotalCost(selections),
                countHalfGuards(selections),
                0
        );
    }

    private Map<Integer, Set<Outcome>> freezeSelections(Map<Integer, Set<Outcome>> selections) {
        Map<Integer, Set<Outcome>> frozen = new HashMap<>();
        for (Map.Entry<Integer, Set<Outcome>> entry : selections.entrySet()) {
            frozen.put(entry.getKey(), Set.copyOf(entry.getValue()));
        }
        return Map.copyOf(frozen);
    }

    private int countHalfGuards(Map<Integer, Set<Outcome>> selections) {
        int count = 0;
        for (Set<Outcome> selection : selections.values()) {
            if (selection.size() == 2) {
                count++;
            }
        }
        return count;
    }

    private int computeTotalCost(Map<Integer, Set<Outcome>> selections) {
        int cost = 1;
        for (Set<Outcome> selection : selections.values()) {
            cost *= selection.size();
        }
        return cost;
    }

    private List<Integer> rankByUncertainty(Map<Integer, ProbabilityTriple> internalProbabilities) {
        List<Integer> matchNumbers = new ArrayList<>(internalProbabilities.keySet());

        matchNumbers.sort((a, b) -> {
            double uncertaintyA = uncertainty(internalProbabilities.get(a));
            double uncertaintyB = uncertainty(internalProbabilities.get(b));
            int cmp = Double.compare(uncertaintyB, uncertaintyA);
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

    private Set<Outcome> expandToHalfGuard(Outcome basePick, ProbabilityTriple internal) {
        LinkedHashSet<Outcome> set = new LinkedHashSet<>();
        set.add(basePick);
        set.add(bestAlternative(basePick, internal));

        return set;
    }

    private Outcome bestAlternative(Outcome basePick, ProbabilityTriple internal) {
        Outcome best = null;
        double bestProbability = Double.NEGATIVE_INFINITY;

        for (Outcome outcome : Outcome.values()) {
            if (outcome == basePick) {
                continue;
            }

            double probability = internal.get(outcome);
            if (probability > bestProbability) {
                bestProbability = probability;
                best = outcome;
            }
        }

        return Objects.requireNonNull(best);
    }

    private int computeHalfGuards(int numberOfMatches, int budget) {
        int halfGuards = 0;
        int cost = 1;

        while (halfGuards < numberOfMatches && cost * 2 <= budget) {
            cost *= 2;
            halfGuards++;
        }

        return halfGuards;
    }

    private String requireNonBlank(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " cannot be blank");
        }
        return value;
    }
}
