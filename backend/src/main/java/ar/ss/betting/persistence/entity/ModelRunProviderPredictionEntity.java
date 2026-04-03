package ar.ss.betting.persistence.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(
        name = "model_run_provider_prediction",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_model_run_match_provider",
                        columnNames = {"model_run_id", "match_number", "provider_name"}
                )
        }
)
public class ModelRunProviderPredictionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JoinColumn(name = "model_run_id", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private ModelRunEntity modelRun;

    @Column(name = "match_number", nullable = false)
    private int matchNumber;

    @Column(name = "provider_name", nullable = false, length = 64)
    private String providerName;

    @Column(name = "status", nullable = false, length = 32)
    private String status;

    @Column(name = "message", length = 255)
    private String message;

    @Column(name = "requested_home_team_name", nullable = false, length = 80)
    private String requestedHomeTeamName;

    @Column(name = "requested_away_team_name", nullable = false, length = 80)
    private String requestedAwayTeamName;

    @Column(name = "resolved_home_team_name", length = 80)
    private String resolvedHomeTeamName;

    @Column(name = "resolved_away_team_name", length = 80)
    private String resolvedAwayTeamName;

    @Column(name = "kickoff")
    private LocalDateTime kickoff;

    @Column(name = "kickoff_raw", length = 64)
    private String kickoffRaw;

    @Column(name = "probability_home")
    private Double probabilityHome;

    @Column(name = "probability_draw")
    private Double probabilityDraw;

    @Column(name = "probability_away")
    private Double probabilityAway;

    @Column(name = "fetched_at")
    private LocalDateTime fetchedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected ModelRunProviderPredictionEntity() {
        // JPA
    }

    public ModelRunProviderPredictionEntity(ModelRunEntity modelRun,
                                            int matchNumber,
                                            String providerName,
                                            String status,
                                            String message,
                                            String requestedHomeTeamName,
                                            String requestedAwayTeamName,
                                            String resolvedHomeTeamName,
                                            String resolvedAwayTeamName,
                                            LocalDateTime kickoff,
                                            String kickoffRaw,
                                            Double probabilityHome,
                                            Double probabilityDraw,
                                            Double probabilityAway,
                                            LocalDateTime fetchedAt,
                                            LocalDateTime createdAt) {

        this.modelRun = Objects.requireNonNull(modelRun, "modelRun cannot be null");

        if (matchNumber <= 0) {
            throw new IllegalArgumentException("matchNumber must be positive");
        }
        this.matchNumber = matchNumber;

        this.providerName = requireNonBlank(providerName, "providerName");
        this.status = requireNonBlank(status, "status");
        this.message = trimToNull(message);
        this.requestedHomeTeamName = requireNonBlank(requestedHomeTeamName, "requestedHomeTeamName");
        this.requestedAwayTeamName = requireNonBlank(requestedAwayTeamName, "requestedAwayTeamName");
        this.resolvedHomeTeamName = trimToNull(resolvedHomeTeamName);
        this.resolvedAwayTeamName = trimToNull(resolvedAwayTeamName);
        this.kickoff = kickoff;
        this.kickoffRaw = trimToNull(kickoffRaw);
        this.probabilityHome = probabilityHome;
        this.probabilityDraw = probabilityDraw;
        this.probabilityAway = probabilityAway;
        this.fetchedAt = fetchedAt;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt cannot be null");
    }

    private static String requireNonBlank(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " cannot be blank");
        }
        return value;
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public Long getId() {
        return id;
    }

    public ModelRunEntity getModelRun() {
        return modelRun;
    }

    public int getMatchNumber() {
        return matchNumber;
    }

    public String getProviderName() {
        return providerName;
    }

    public String getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public String getRequestedHomeTeamName() {
        return requestedHomeTeamName;
    }

    public String getRequestedAwayTeamName() {
        return requestedAwayTeamName;
    }

    public String getResolvedHomeTeamName() {
        return resolvedHomeTeamName;
    }

    public String getResolvedAwayTeamName() {
        return resolvedAwayTeamName;
    }

    public LocalDateTime getKickoff() {
        return kickoff;
    }

    public String getKickoffRaw() {
        return kickoffRaw;
    }

    public Double getProbabilityHome() {
        return probabilityHome;
    }

    public Double getProbabilityDraw() {
        return probabilityDraw;
    }

    public Double getProbabilityAway() {
        return probabilityAway;
    }

    public LocalDateTime getFetchedAt() {
        return fetchedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}