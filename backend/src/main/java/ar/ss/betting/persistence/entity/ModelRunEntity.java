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

    @JoinColumn(name = "round_id", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private RoundEntity round;

    @Column(name = "model_name", nullable = false, length = 128)
    private String modelName;

    @Column(name = "generated_at", nullable = false)
    private LocalDateTime generatedAt;

    @Column(name = "budget_in_sek", nullable = false)
    private int budgetInSek;

    @Column(name = "total_cost_in_sek", nullable = false)
    private int totalCostInSek;

    @Column(name = "half_guards_count", nullable = false)
    private int halfGuardsCount;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "selections_json", nullable = false, columnDefinition = "jsonb")
    private String selectionsJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "internal_probabilities_json", nullable = false, columnDefinition = "jsonb")
    private String internalProbabilitiesJson;

    @Column(name = "trigger", nullable = false, length = 64)
    private String trigger;

    protected ModelRunEntity() {
        // JPA
    }

    public ModelRunEntity(RoundEntity round,
                          String modelName,
                          LocalDateTime generatedAt,
                          int budgetInSek,
                          int totalCostInSek,
                          int halfGuardsCount,
                          String selectionsJson,
                          String internalProbabilitiesJson,
                          String trigger) {

        this.round = Objects.requireNonNull(round, "round cannot be null");
        this.modelName = requireNonBlank(modelName, "modelName");
        this.generatedAt = Objects.requireNonNull(generatedAt, "generatedAt cannot be null");
        this.budgetInSek = requirePositive(budgetInSek, "budgetInSek");
        this.totalCostInSek = requirePositive(totalCostInSek, "totalCostInSek");

        if (halfGuardsCount < 0) {
            throw new IllegalArgumentException("halfGuardsCount cannot be negative");
        }

        this.halfGuardsCount = halfGuardsCount;
        this.selectionsJson = requireNonBlank(selectionsJson, "selectionsJson");
        this.internalProbabilitiesJson = requireNonBlank(internalProbabilitiesJson, "internalProbabilitiesJson");
        this.trigger = requireNonBlank(trigger, "trigger");
    }

    private static int requirePositive(int value, String field) {
        if (value <= 0) {
            throw new IllegalArgumentException(field + " must be positive");
        }
        return value;
    }

    private static String requireNonBlank(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " cannot be blank");
        }
        return value;
    }

    public Long getId() {
        return id;
    }

    public RoundEntity getRound() {
        return round;
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

    public String getSelectionsJson() {
        return selectionsJson;
    }

    public String getInternalProbabilitiesJson() {
        return internalProbabilitiesJson;
    }

    public String getTrigger() {
        return trigger;
    }
}