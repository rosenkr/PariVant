package ar.ss.betting.persistence.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "model_run",
        indexes = {
                @Index(name = "idx_model_run_round_id", columnList = "round_id"),
                @Index(name = "idx_model_run_generated_at", columnList = "generated_at")
        }
)
public class ModelRunEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "round_id", nullable = false)
    private RoundEntity round;

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

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "selections_json", nullable = false, columnDefinition = "jsonb")
    private String selectionsJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "base_picks_json", nullable = false, columnDefinition = "jsonb")
    private String basePicksJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "internal_probabilities_json", nullable = false, columnDefinition = "jsonb")
    private String internalProbabilitiesJson;

    @Column(name = "trigger", nullable = false, length = 64)
    private String trigger;

    protected ModelRunEntity() {
    }

    public ModelRunEntity(RoundEntity round,
                          String modelName,
                          LocalDateTime generatedAt,
                          int budgetInSek,
                          int totalCostInSek,
                          int halfGuardsCount,
                          String selectionsJson,
                          String basePicksJson,
                          String internalProbabilitiesJson,
                          String trigger) {
        this.round = round;
        this.modelName = modelName;
        this.generatedAt = generatedAt;
        this.budgetInSek = budgetInSek;
        this.totalCostInSek = totalCostInSek;
        this.halfGuardsCount = halfGuardsCount;
        this.selectionsJson = selectionsJson;
        this.basePicksJson = basePicksJson;
        this.internalProbabilitiesJson = internalProbabilitiesJson;
        this.trigger = trigger;
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

    public String getBasePicksJson() {
        return basePicksJson;
    }

    public String getInternalProbabilitiesJson() {
        return internalProbabilitiesJson;
    }

    public String getTrigger() {
        return trigger;
    }
}