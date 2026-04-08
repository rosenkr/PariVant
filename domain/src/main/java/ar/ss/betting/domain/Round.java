package ar.ss.betting.domain;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * A betting round consisting of a fixed number of matches determined by RoundType.
 */
public class Round {

    private final Instant startTime;
    private final RoundType roundType;
    private final RoundStatus status;
    private final List<Match> matches;

    public Round(Instant startTime, RoundType roundType, List<Match> matches) {
        this(startTime, roundType, RoundStatus.UPCOMING, matches);
    }

    public Round(Instant startTime,
                 RoundType roundType,
                 RoundStatus status,
                 List<Match> matches) {
        this.startTime = Objects.requireNonNull(startTime, "startTime cannot be null");
        this.roundType = Objects.requireNonNull(roundType, "roundType cannot be null");
        this.status = Objects.requireNonNull(status, "status cannot be null");
        this.matches = List.copyOf(Objects.requireNonNull(matches, "matches cannot be null"));

        validateMatchCount();
        validateUniqueMatchNumbers();
        validateConsecutiveMatchNumbers();
        validateTeamsAppearAtMostOnce();
    }

    public Instant getStartTime() {
        return startTime;
    }

    public RoundType getRoundType() {
        return roundType;
    }

    public RoundStatus getStatus() {
        return status;
    }

    public List<Match> getMatches() {
        return matches;
    }

    private void validateMatchCount() {
        int expected = roundType.getNumberOfMatches();
        if (matches.size() != expected) {
            throw new IllegalArgumentException(
                    "Expected " + expected + " matches for " + roundType + " but got " + matches.size()
            );
        }
    }

    private void validateUniqueMatchNumbers() {
        Set<Integer> seen = new HashSet<>();
        for (Match match : matches) {
            int matchNumber = match.getMatchNumber();
            if (!seen.add(matchNumber)) {
                throw new IllegalArgumentException("Duplicate match number: " + matchNumber);
            }
        }
    }

    private void validateConsecutiveMatchNumbers() {
        List<Integer> numbers = matches.stream()
                .map(Match::getMatchNumber)
                .sorted()
                .toList();

        for (int i = 0; i < numbers.size(); i++) {
            int expected = i + 1;
            int actual = numbers.get(i);
            if (actual != expected) {
                throw new IllegalArgumentException(
                        "Match numbers must be consecutive starting at 1. Missing/invalid at " + expected
                );
            }
        }
    }

    private void validateTeamsAppearAtMostOnce() {
        Set<Team> seen = new HashSet<>();

        for (Match match : matches) {
            Team home = match.getHomeTeam();
            Team away = match.getAwayTeam();

            if (!seen.add(home)) {
                throw new IllegalArgumentException("Team appears more than once in round: " + home.getName());
            }
            if (!seen.add(away)) {
                throw new IllegalArgumentException("Team appears more than once in round: " + away.getName());
            }
        }
    }
}