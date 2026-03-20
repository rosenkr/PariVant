package ar.ss.betting.domain;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Represents one betting round (e.g., one Stryktipset round).
 */
public class GameRound {

    private final LocalDateTime startDate;
    private final GameType gameType;
    private final List<Match> matches;

    public GameRound(LocalDateTime startDate,
                     GameType gameType,
                     List<Match> matches) {

        this.startDate = Objects.requireNonNull(startDate, "Start date cannot be null");
        this.gameType = Objects.requireNonNull(gameType, "GameType cannot be null");
        this.matches = List.copyOf(Objects.requireNonNull(matches, "Matches cannot be null"));

        validate();
    }

    private void validate() {
        validateMatchCount();
        validateMatchNumbers();
        validateUniqueTeams();
    }

    private void validateMatchCount() {
        if (matches.size() != gameType.getNumberOfMatches()) {
            throw new IllegalArgumentException(
                    "Expected " + gameType.getNumberOfMatches() +
                            " matches for " + gameType +
                            " but got " + matches.size()
            );
        }
    }

    private void validateMatchNumbers() {

        Set<Integer> numbers = matches.stream()
                .map(Match::getMatchNumber)
                .collect(Collectors.toSet());

        // duplicate numbers
        if (numbers.size() != matches.size()) {
            throw new IllegalArgumentException("Match numbers must be unique");
        }

        // consecutive from 1..N
        for (int i = 1; i <= matches.size(); i++) {
            if (!numbers.contains(i)) {
                throw new IllegalArgumentException(
                        "Match numbers must be consecutive from 1 to " + matches.size()
                );
            }
        }
    }

    private void validateUniqueTeams() {

        Set<Team> seenTeams = new HashSet<>();

        for (Match match : matches) {

            if (!seenTeams.add(match.getHomeTeam())) {
                throw new IllegalArgumentException("A team appears more than once in the round");
            }

            if (!seenTeams.add(match.getAwayTeam())) {
                throw new IllegalArgumentException("A team appears more than once in the round");
            }
        }
    }

    public LocalDateTime getStartDate() {
        return startDate;
    }

    public GameType getGameType() {
        return gameType;
    }

    public List<Match> getMatches() {
        return Collections.unmodifiableList(matches);
    }
}
