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

    /**
     * Default parameters approximating the choices you've made so far:
     * - Topptipset slightly riskier (lower floor & threshold)
     * - Stryktipset/Europatipset safer
     * - Full guard caps: 1 for Topptipset, 2 for 13-match games
     */
    public static DecisionParameters defaults() {
        return new DecisionParameters(
                0.12, // Topptipset floor
                0.18, // 13-match games floor
                0.02, // Topptipset tau
                0.04, // 13-match games tau
                1,    // max full guards Topptipset
                2     // max full guards Stryktipset/Europatipset
        );
    }
}