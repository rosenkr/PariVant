package ar.ss.betting.model;

import ar.ss.betting.domain.Match;
import ar.ss.betting.domain.Round;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class EnsembleModel implements GameModel {

    private final InternalProbabilityCalculator probabilityCalculator;
    private final ValueModel valueModel;

    public EnsembleModel() {
        this.probabilityCalculator = new InternalProbabilityCalculator();
        this.valueModel = new ValueModel("EnsembleModel");
    }

    @Override
    public ModelSelectionResult generateSelection(Round round,
                                                  ModelInput modelInput,
                                                  int maxBudgetInSek,
                                                  Instant generatedAt) {
        Objects.requireNonNull(round, "round cannot be null");
        Objects.requireNonNull(modelInput, "modelInput cannot be null");
        Objects.requireNonNull(generatedAt, "generatedAt cannot be null");

        if (maxBudgetInSek <= 0) {
            throw new IllegalArgumentException("Budget must be positive");
        }

        validateBuffQuota(round, modelInput);

        Map<Integer, ProbabilityTriple> internalProbabilities = new HashMap<>();
        Map<Integer, ProbabilityTriple> publicProbabilities = new HashMap<>();

        for (Match match : round.getMatches()) {
            int matchNumber = match.getMatchNumber();

            MatchContext context = modelInput.getMatchContext(matchNumber);
            MatchInterventions interventions = modelInput.getMatchInterventions(matchNumber);

            ProbabilityTriple internal = probabilityCalculator.calculateInternalProbabilities(
                    context,
                    interventions
            );
            internalProbabilities.put(matchNumber, internal);
            publicProbabilities.put(matchNumber, context.publicProbabilities());
        }

        return valueModel.generateSelection(
                round,
                new ValueModelInput(internalProbabilities, publicProbabilities),
                maxBudgetInSek,
                generatedAt
        );
    }

    private void validateBuffQuota(Round round, ModelInput modelInput) {
        int usedBuffPoints = modelInput.getMatchInterventions().values().stream()
                .mapToInt(MatchInterventions::totalBuffPoints)
                .sum();

        int roundSize = round.getMatches().size();
        int maxAllowed = roundSize * ModelConstants.BUFF_POINTS_PER_MATCH_IN_ROUND;

        if (usedBuffPoints > maxAllowed) {
            throw new IllegalArgumentException(
                    "Buff quota exceeded for round. Used " + usedBuffPoints + ", allowed " + maxAllowed
            );
        }
    }

}
