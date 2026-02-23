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
 * - Choose exactly one outcome for each match. Total cost initially = 1 row.
 *
 * Layer 2 (Coverage allocation):
 * - Spend budget by expanding some matches from single -> half guard (2 outcomes).
 * - In Step B baseline, we do NOT use full guards (3 outcomes).
 *
 * The key design: we separate "choose base outcome" from "where to allocate coverage".
 * In Step C, we will plug in real scoring using odds/public distribution/etc.
 */
public class RuleBasedModel implements GameModel {

    @Override
    public ModelSelectionResult generateSelection(GameRound gameRound, int maxBudgetInSek) {

        if (maxBudgetInSek <= 0) {
            throw new IllegalArgumentException("Budget must be positive");
        }

        // ---- Layer 1: base picks (all singles) ----
        Map<Integer, Outcome> basePicks = new HashMap<>();
        for (Match match : gameRound.getMatches()) {
            basePicks.put(match.getMatchNumber(), chooseSingleOutcome(match));
        }

        // Selections start as singles
        Map<Integer, Set<Outcome>> selections = basePicks.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> Set.of(e.getValue())
                ));

        // Initial cost is 1 row
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
     * Layer 1 decision: choose exactly one outcome for a match.
     *
     * Step B baseline: placeholder always picks HOME_WIN.
     * Step C: will use odds/public distribution/etc.
     */
    protected Outcome chooseSingleOutcome(Match match) {
        return Outcome.HOME_WIN;
    }

    /**
     * Rank matches by how beneficial it is to add coverage (half-guard).
     *
     * Step B baseline: deterministic ordering (by match number).
     * Step C: replace with real uncertainty/value scoring.
     */
    protected List<Match> rankMatchesForCoverage(GameRound gameRound, Map<Integer, Outcome> basePicks) {
        List<Match> matches = new ArrayList<>(gameRound.getMatches());
        matches.sort(Comparator.comparingInt(Match::getMatchNumber));
        return matches;
    }

    /**
     * Layer 2 action: expand a single outcome to a half-guard.
     *
     * Step B baseline: {HOME_WIN, DRAW} regardless of base pick (placeholder).
     * Step C: choose the "best second outcome" for that match (e.g., add AWAY_WIN if needed).
     */
    protected Set<Outcome> expandToHalfGuard(Outcome baseOutcome) {
        // Minimal placeholder half-guard.
        // NOTE: In Step C we'll improve this to include baseOutcome + best alternate outcome.
        return Set.of(Outcome.HOME_WIN, Outcome.DRAW);
    }

    /**
     * For Step B baseline, we use only half-guards to reach the largest power-of-two <= budget.
     */
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