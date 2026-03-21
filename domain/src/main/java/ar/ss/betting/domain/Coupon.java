package ar.ss.betting.domain;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Represents a betting coupon (a complete ticket for a GameRound).
 */
public class Coupon {

    private final Round gameRound;
    private final LocalDateTime generatedAt;
    private final int budgetInSek;
    private final Map<Integer, Set<Outcome>> selections; // maps i:th match to a set of outcomes

    public Coupon(Round gameRound,
                  LocalDateTime generatedAt,
                  int budgetInSek,
                  Map<Integer, Set<Outcome>> selections) {

        this.gameRound = Objects.requireNonNull(gameRound);
        this.generatedAt = Objects.requireNonNull(generatedAt);

        if (budgetInSek <= 0) {
            throw new IllegalArgumentException("Budget must be positive");
        }
        this.budgetInSek = budgetInSek;

        Objects.requireNonNull(selections, "Selections cannot be null");

        Set<Integer> expectedMatchNumbers = gameRound.getMatches().stream()
                .map(Match::getMatchNumber)
                .collect(Collectors.toSet());

        if (!expectedMatchNumbers.equals(selections.keySet())) {
            throw new IllegalArgumentException("Selections must match GameRound match numbers exactly");
        }

        // Ensure no selection is empty
        for (Map.Entry<Integer, Set<Outcome>> entry : selections.entrySet()) {
            if (entry.getValue().isEmpty()) {
                throw new IllegalArgumentException("Selection for match " + entry.getKey() + " cannot be empty");
            }
        }

        this.selections = Map.copyOf(selections);
    }

    public Round getGameRound() {
        return gameRound;
    }

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }

    public int getBudgetInSek() {
        return budgetInSek;
    }

    public Map<Integer, Set<Outcome>> getSelections() {
        return Collections.unmodifiableMap(selections);
    }

    /**
     * Returns total number of betting combinations (Cartesian product size)
     */
    public int numberOfRows() {
        return selections.values().stream()
                .mapToInt(Set::size)
                .reduce(1, (a, b) -> a * b);
    }

    /**
     * Total cost = numberOfRows * 1 SEK
     */
    public int totalCost() {
        return numberOfRows();
    }
}

