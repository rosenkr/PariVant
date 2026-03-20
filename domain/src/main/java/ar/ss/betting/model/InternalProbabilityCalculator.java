package ar.ss.betting.model;

import ar.ss.betting.domain.Outcome;

import java.util.List;
import java.util.Objects;

/**
 * Internal probability engine.
 *
 * Step-2 behavior:
 * 1. Compute base internal probabilities as equal-weight ensemble:
 *      p = (market + sum(providers)) / (n + 1)
 * 2. Apply optional deterministic tag shifts
 * 3. Apply optional user buff shift
 * 4. Clamp to positive components and renormalize
 */
public class InternalProbabilityCalculator {

    public ProbabilityTriple calculateInternalProbabilities(MatchContext ctx) {
        return calculateInternalProbabilities(ctx, MatchInterventions.empty());
    }

    public ProbabilityTriple calculateInternalProbabilities(MatchContext ctx,
                                                            MatchInterventions interventions) {
        Objects.requireNonNull(ctx, "ctx cannot be null");
        Objects.requireNonNull(interventions, "interventions cannot be null");

        ProbabilityTriple base = aggregateBaseProbabilities(ctx);
        ProbabilityShift totalShift = resolveTotalShift(interventions);

        return applyShift(base, totalShift);
    }

    private ProbabilityTriple aggregateBaseProbabilities(MatchContext ctx) {
        ProbabilityTriple market = ctx.getMarketProbabilities();
        List<ProbabilityTriple> providers = ctx.getProviderProbabilities();

        double home = market.get(Outcome.HOME_WIN);
        double draw = market.get(Outcome.DRAW);
        double away = market.get(Outcome.AWAY_WIN);

        for (ProbabilityTriple provider : providers) {
            home += provider.get(Outcome.HOME_WIN);
            draw += provider.get(Outcome.DRAW);
            away += provider.get(Outcome.AWAY_WIN);
        }

        double divisor = providers.size() + 1.0;

        return ProbabilityTriple.fromProbabilities(
                home / divisor,
                draw / divisor,
                away / divisor
        );
    }

    private ProbabilityShift resolveTotalShift(MatchInterventions interventions) {
        ProbabilityShift total = ProbabilityShift.none();

        for (MatchTag tag : interventions.getTags()) {
            total = total.plus(resolveTagShift(tag));
        }

        if (interventions.getBuff().isPresent()) {
            MatchBuff buff = interventions.getBuff().get();
            total = total.plus(ModelConstants.buffShift(buff.getTargetOutcome(), buff.getPoints()));
        }

        return total;
    }

    private ProbabilityShift resolveTagShift(MatchTag tag) {
        return switch (tag.getType()) {
            case NEUTRAL_VENUE -> ModelConstants.neutralVenueShift();
            case KEY_ABSENCE -> ModelConstants.keyAbsenceShift(tag.getAffectedSide().orElseThrow());
            case SHORT_REST -> ModelConstants.shortRestShift(tag.getAffectedSide().orElseThrow());
            case INCENTIVE_LACK -> ModelConstants.incentiveLackShift(tag.getAffectedSide().orElseThrow());
        };
    }

    private ProbabilityTriple applyShift(ProbabilityTriple base, ProbabilityShift shift) {
        double home = base.get(Outcome.HOME_WIN) + shift.getHomeDelta();
        double draw = base.get(Outcome.DRAW) + shift.getDrawDelta();
        double away = base.get(Outcome.AWAY_WIN) + shift.getAwayDelta();

        home = Math.max(home, ModelConstants.MIN_PROBABILITY_COMPONENT);
        draw = Math.max(draw, ModelConstants.MIN_PROBABILITY_COMPONENT);
        away = Math.max(away, ModelConstants.MIN_PROBABILITY_COMPONENT);

        double sum = home + draw + away;

        return ProbabilityTriple.fromProbabilities(
                home / sum,
                draw / sum,
                away / sum
        );
    }
}