package ar.ss.betting.persistence.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "model_run")
public class ModelRunEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "game_round_id", nullable = false)
    private GameRoundEntity gameRound;

    @Column(name = "model_name", nullable = false, length = 64)
    private String modelName;

    @Column(name = "generated_at", nullable = false)
    private LocalDateTime generatedAt;

    @Column(name = "budget_in_sek", nullable = false)
    private int budgetInSek;

    @Column(name = "total_cost_in_sek", nullable = false)
    private int totalCostInSek;

    @Column(name = "half_guards_count", nullable = false)
    private int halfGuardsCount;

    @Column(name = "full_guards_count", nullable = false)
    private int fullGuardsCount;

    /**
     * JSONB columns stored as Strings, but bound as SQL JSON.
     * This makes Hibernate send correct parameter types to PostgreSQL.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "selections_json", nullable = false, columnDefinition = "jsonb")
    private String selectionsJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "weights_json", nullable = false, columnDefinition = "jsonb")
    private String weightsJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "decision_parameters_json", nullable = false, columnDefinition = "jsonb")
    private String decisionParametersJson;

    protected ModelRunEntity() {
        // JPA
    }

    public ModelRunEntity(GameRoundEntity gameRound,
                          String modelName,
                          LocalDateTime generatedAt,
                          int budgetInSek,
                          int totalCostInSek,
                          int halfGuardsCount,
                          int fullGuardsCount,
                          String selectionsJson,
                          String weightsJson,
                          String decisionParametersJson) {

        this.gameRound = Objects.requireNonNull(gameRound, "gameRound cannot be null");
        this.modelName = requireNonBlank(modelName, "modelName");
        this.generatedAt = Objects.requireNonNull(generatedAt, "generatedAt cannot be null");

        if (budgetInSek <= 0) throw new IllegalArgumentException("budgetInSek must be positive");
        if (totalCostInSek <= 0) throw new IllegalArgumentException("totalCostInSek must be positive");
        if (halfGuardsCount < 0) throw new IllegalArgumentException("halfGuardsCount must be >= 0");
        if (fullGuardsCount < 0) throw new IllegalArgumentException("fullGuardsCount must be >= 0");

        this.budgetInSek = budgetInSek;
        this.totalCostInSek = totalCostInSek;
        this.halfGuardsCount = halfGuardsCount;
        this.fullGuardsCount = fullGuardsCount;

        this.selectionsJson = requireNonBlank(selectionsJson, "selectionsJson");
        this.weightsJson = requireNonBlank(weightsJson, "weightsJson");
        this.decisionParametersJson = requireNonBlank(decisionParametersJson, "decisionParametersJson");
    }

    private String requireNonBlank(String s, String name) {
        Objects.requireNonNull(s, name + " cannot be null");
        if (s.isBlank()) {
            throw new IllegalArgumentException(name + " cannot be blank");
        }
        return s;
    }

    public Long getId() {
        return id;
    }

    public GameRoundEntity getGameRound() {
        return gameRound;
    }

    public String getModelName() {
        return modelName;
    }

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }

    public int getBudgetInSek() {
        return budgetInSek;
    }

    public int getTotalCostInSek() {
        return totalCostInSek;
    }

    public int getHalfGuardsCount() {
        return halfGuardsCount;
    }

    public int getFullGuardsCount() {
        return fullGuardsCount;
    }

    public String getSelectionsJson() {
        return selectionsJson;
    }

    public String getWeightsJson() {
        return weightsJson;
    }

    public String getDecisionParametersJson() {
        return decisionParametersJson;
    }
}