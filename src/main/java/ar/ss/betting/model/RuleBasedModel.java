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
 * - In Step C.1 we still do NOT use full guards (3 outcomes).
 *
 * Later in Step C:
 * - Coverage ranking will use uncertainty/value from inputs.
 * - expandToHalfGuard will choose the best second outcome based on context.
 * - full guards will be introduced with soft constraints.
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

        // ---- Layer 2: allocate half guards ----
        List<Match> rankedForCoverage = rankMatchesForCoverage(gameRound, basePicks);

        int appliedHalfGuards = 0;
        for (Match match : rankedForCoverage) {
            if (appliedHalfGuards >= halfGuardsToUse) {
                break;
            }

            int matchNumber = match.getMatchNumber();
            Outcome base = basePicks.get(matchNumber);

            selections.put(matchNumber, expandToHalfGuard(base));
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
     * Step C.1 baseline ranking: deterministic ordering (by match number).
     * Step C.2/C.3: will replace with uncertainty/value based ranking.
     */
    protected List<Match> rankMatchesForCoverage(GameRound gameRound, Map<Integer, Outcome> basePicks) {
        List<Match> matches = new ArrayList<>(gameRound.getMatches());
        matches.sort(Comparator.comparingInt(Match::getMatchNumber));
        return matches;
    }

    /**
     * Step C.1 baseline: return a placeholder half-guard.
     * Step C.3: choose baseOutcome + best alternative outcome based on MatchContext.
     */
    protected Set<Outcome> expandToHalfGuard(Outcome baseOutcome) {
        // Minimal placeholder half-guard; refined later.
        return Set.of(Outcome.HOME_WIN, Outcome.DRAW);
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