package ar.ss.betting.domain;

import java.util.Locale;
import java.util.Objects;

/**
 * Represents a team participating in a Match.
 *
 * Identity policy (for now):
 * - Teams are considered equal by normalized name (case-insensitive, trimmed).
 *
 * Future extension:
 * - If you later need to distinguish AIK football vs AIK hockey, add a TeamId and/or sport/competition.
 */
public class Team {

    private final String name;          // Display name (trimmed)
    private final String normalizedKey; // Identity key (lowercased, trimmed)

    public Team(String name) {
        Objects.requireNonNull(name, "Team name cannot be null");

        String trimmed = name.trim();
        if (trimmed.isBlank()) {
            throw new IllegalArgumentException("Team name cannot be blank");
        }

        this.name = trimmed;
        this.normalizedKey = trimmed.toLowerCase(Locale.ROOT);
    }

    public String getName() {
        return name;
    }

    /**
     * Case-insensitive identity key.
     * Useful for debugging/logging and later persistence mapping.
     */
    public String getNormalizedKey() {
        return normalizedKey;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Team team)) return false;
        return normalizedKey.equals(team.normalizedKey);
    }

    @Override
    public int hashCode() {
        return normalizedKey.hashCode();
    }

    @Override
    public String toString() {
        return "Team{name='" + name + "'}";
    }
}
