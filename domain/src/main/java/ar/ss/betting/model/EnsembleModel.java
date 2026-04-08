package ar.ss.betting.model;

import ar.ss.betting.domain.Match;
import ar.ss.betting.domain.Outcome;
import ar.ss.betting.domain.Round;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

public class EnsembleModel implements GameModel {

    private final BaseOutcomeSelector baseOutcomeSelector;
    private final InternalProbabilityCalculator probabilityCalculator;

    public EnsembleModel() {
        this.baseOutcomeSelector = new BaseOutcomeSelector();
        this.probabilityCalculator = new InternalProbabilityCalculator();
    }

    @Override
    public ModelSelectionResult generateSelection(Round round,
                                                  ModelInput modelInput,
                                                  int maxBudgetInSek,
                                                  Instant generatedAt) {
        Objects.requireNonNull(round, "round cannot be null");
        Objects.requireNonNull(modelInput, "modelInput cannot be null");
        Objects.requireNonNull(generatedAt, "generatedAt cannot be null");

        if (maxBudgetInSek <= 0) {
            throw new IllegalArgumentException("Budget must be positive");
        }

        validateBuffQuota(round, modelInput);

        Map<Integer, ProbabilityTriple> internalProbabilities = new HashMap<>();
        Map<Integer, Outcome> basePicks = new HashMap<>();

        for (Match match : round.getMatches()) {
            int matchNumber = match.getMatchNumber();

            MatchContext context = modelInput.getMatchContext(matchNumber);
            MatchInterventions interventions = modelInput.getMatchInterventions(matchNumber);

            ProbabilityTriple internal = probabilityCalculator.calculateInternalProbabilities(
                    context,
                    interventions
            );
            internalProbabilities.put(matchNumber, internal);

            Outcome basePick = baseOutcomeSelector.chooseBaseOutcome(
                    internal,
                    context.publicProbabilities()
            );
            basePicks.put(matchNumber, basePick);
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

        int totalCost = computeTotalCost(selections);

        return new ModelSelectionResult(
                "EnsembleModel",
                generatedAt,
                freezeBasePicks(basePicks),
                freezeSelections(selections),
                freezeInternalProbabilities(internalProbabilities),
                totalCost,
                countHalfGuards(selections),
                0
        );
    }

    private void validateBuffQuota(Round round, ModelInput modelInput) {
        int usedBuffPoints = modelInput.getMatchInterventions().values().stream()
                .mapToInt(MatchInterventions::totalBuffPoints)
                .sum();

        int roundSize = round.getMatches().size();
        int maxAllowed = roundSize * ModelConstants.BUFF_POINTS_PER_MATCH_IN_ROUND;

        if (usedBuffPoints > maxAllowed) {
            throw new IllegalArgumentException(
                    "Buff quota exceeded for round. Used " + usedBuffPoints + ", allowed " + maxAllowed
            );
        }
    }

    private Map<Integer, Outcome> freezeBasePicks(Map<Integer, Outcome> basePicks) {
        return Collections.unmodifiableMap(new HashMap<>(basePicks));
    }

    private Map<Integer, Set<Outcome>> freezeSelections(Map<Integer, Set<Outcome>> selections) {
        Map<Integer, Set<Outcome>> frozen = new HashMap<>();
        for (Map.Entry<Integer, Set<Outcome>> entry : selections.entrySet()) {
            frozen.put(
                    entry.getKey(),
                    Collections.unmodifiableSet(new LinkedHashSet<>(entry.getValue()))
            );
        }
        return Collections.unmodifiableMap(frozen);
    }

    private Map<Integer, ProbabilityTriple> freezeInternalProbabilities(
            Map<Integer, ProbabilityTriple> internalProbabilities
    ) {
        return Collections.unmodifiableMap(new HashMap<>(internalProbabilities));
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
        Outcome bestAlternative = bestAlternative(basePick, internal);

        LinkedHashSet<Outcome> set = new LinkedHashSet<>();
        set.add(basePick);
        set.add(bestAlternative);

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
}