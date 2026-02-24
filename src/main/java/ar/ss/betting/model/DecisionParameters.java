package ar.ss.betting.model;

import ar.ss.betting.domain.GameType;

import java.util.Objects;

/**
 * User-configurable parameters controlling "decision layer" behavior:
 * - probability floors
 * - value thresholds
 * - full guard caps
 *
 * This avoids hardcoded constants and allows clients to define what
 * "low/medium/high risk" means to them programmatically.
 *
 * RiskProfile can later be added as presets mapping to DecisionParameters.
 */
public class DecisionParameters {

    // Default constants (explicit names, easy to tweak later)
    public static final double DEFAULT_PROBABILITY_FLOOR_TOPPTIPSET = 0.12;
    public static final double DEFAULT_PROBABILITY_FLOOR_STRYKTIPSET = 0.18;

    public static final double DEFAULT_VALUE_THRESHOLD_TOPPTIPSET = 0.02;
    public static final double DEFAULT_VALUE_THRESHOLD_STRYKTIPSET = 0.04;

    public static final int DEFAULT_MAX_FULL_GUARDS_TOPPTIPSET = 1;
    public static final int DEFAULT_MAX_FULL_GUARDS_STRYKTIPSET = 2;

    // Probability floor for considering an outcome as a value-candidate
    private final double probabilityFloorTopptipset;
    private final double probabilityFloorStryktipset;

    // Value threshold tau: require this much improvement over baseline value to switch
    private final double valueThresholdTopptipset;
    private final double valueThresholdStryktipset;

    // Soft caps for full guards (model constraint)
    private final int maxFullGuardsTopptipset;
    private final int maxFullGuardsStryktipset;

    public DecisionParameters(double probabilityFloorTopptipset,
                              double probabilityFloorStryktipset,
                              double valueThresholdTopptipset,
                              double valueThresholdStryktipset,
                              int maxFullGuardsTopptipset,
                              int maxFullGuardsStryktipset) {

        validateProb(probabilityFloorTopptipset, "probabilityFloorTopptipset");
        validateProb(probabilityFloorStryktipset, "probabilityFloorStryktipset");
        validateProb(valueThresholdTopptipset, "valueThresholdTopptipset");
        validateProb(valueThresholdStryktipset, "valueThresholdStryktipset");

        if (maxFullGuardsTopptipset < 0) {
            throw new IllegalArgumentException("maxFullGuardsTopptipset must be >= 0");
        }
        if (maxFullGuardsStryktipset < 0) {
            throw new IllegalArgumentException("maxFullGuardsStryktipset must be >= 0");
        }

        this.probabilityFloorTopptipset = probabilityFloorTopptipset;
        this.probabilityFloorStryktipset = probabilityFloorStryktipset;
        this.valueThresholdTopptipset = valueThresholdTopptipset;
        this.valueThresholdStryktipset = valueThresholdStryktipset;
        this.maxFullGuardsTopptipset = maxFullGuardsTopptipset;
        this.maxFullGuardsStryktipset = maxFullGuardsStryktipset;
    }

    private void validateProb(double value, String name) {
        if (value < 0.0 || value > 1.0) {
            throw new IllegalArgumentException(name + " must be between 0.0 and 1.0");
        }
    }

    // --- Resolution methods (keep these, used by the model) ---

    public double probabilityFloor(GameType gameType) {
        Objects.requireNonNull(gameType);
        return (gameType == GameType.TOPPTIPSET) ? probabilityFloorTopptipset : probabilityFloorStryktipset;
    }

    public double valueThreshold(GameType gameType) {
        Objects.requireNonNull(gameType);
        return (gameType == GameType.TOPPTIPSET) ? valueThresholdTopptipset : valueThresholdStryktipset;
    }

    public int maxFullGuards(GameType gameType) {
        Objects.requireNonNull(gameType);
        return (gameType == GameType.TOPPTIPSET) ? maxFullGuardsTopptipset : maxFullGuardsStryktipset;
    }

    // --- Getters (needed for persistence snapshot & API transparency) ---

    public double getProbabilityFloorTopptipset() {
        return probabilityFloorTopptipset;
    }

    public double getProbabilityFloorStryktipset() {
        return probabilityFloorStryktipset;
    }

    public double getValueThresholdTopptipset() {
        return valueThresholdTopptipset;
    }

    public double getValueThresholdStryktipset() {
        return valueThresholdStryktipset;
    }

    public int getMaxFullGuardsTopptipset() {
        return maxFullGuardsTopptipset;
    }

    public int getMaxFullGuardsStryktipset() {
        return maxFullGuardsStryktipset;
    }

    /**
     * Default parameters approximating the choices you've made so far:
     * - Topptipset slightly riskier (lower floor & threshold)
     * - Stryktipset/Europatipset safer
     * - Full guard caps: 1 for Topptipset, 2 for 13-match games
     */
    public static DecisionParameters defaults() {
        return new DecisionParameters(
                DEFAULT_PROBABILITY_FLOOR_TOPPTIPSET,
                DEFAULT_PROBABILITY_FLOOR_STRYKTIPSET,
                DEFAULT_VALUE_THRESHOLD_TOPPTIPSET,
                DEFAULT_VALUE_THRESHOLD_STRYKTIPSET,
                DEFAULT_MAX_FULL_GUARDS_TOPPTIPSET,
                DEFAULT_MAX_FULL_GUARDS_STRYKTIPSET
        );
    }
}