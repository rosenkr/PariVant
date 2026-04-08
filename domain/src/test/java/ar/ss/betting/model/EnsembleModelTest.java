package ar.ss.betting.model;

import ar.ss.betting.domain.*;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnsembleModelTest {

    private Round createTopptipsetRound8Matches() {
        return new Round(
                Instant.now(),
                RoundType.TOPPTIPSET,
                List.of(
                        new Match(1, Instant.now(), new Team("A"), new Team("B")),
                        new Match(2, Instant.now(), new Team("C"), new Team("D")),
                        new Match(3, Instant.now(), new Team("E"), new Team("F")),
                        new Match(4, Instant.now(), new Team("G"), new Team("H")),
                        new Match(5, Instant.now(), new Team("I"), new Team("J")),
                        new Match(6, Instant.now(), new Team("K"), new Team("L")),
                        new Match(7, Instant.now(), new Team("M"), new Team("N")),
                        new Match(8, Instant.now(), new Team("O"), new Team("P"))
                )
        );
    }

    private ModelInput createNeutralModelInputForRound(Round round) {
        Map<Integer, MatchContext> ctx = new HashMap<>();

        for (Match match : round.getMatches()) {
            ProbabilityTriple market = ProbabilityTriple.fromProbabilities(0.50, 0.25, 0.25);
            ProbabilityTriple pub = ProbabilityTriple.fromProbabilities(0.50, 0.25, 0.25);
            ctx.put(match.getMatchNumber(), new MatchContext(market, pub, List.of()));
        }

        return new ModelInput(ctx);
    }

    @Test
    void shouldNotExceedBudget() {
        Round round = createTopptipsetRound8Matches();
        ModelInput input = createNeutralModelInputForRound(round);
        GameModel model = new EnsembleModel();

        ModelSelectionResult result = model.generateSelection(round, input, 100);

        assertTrue(result.totalCostInSek() <= 100);
    }

    @Test
    void shouldReturnInternalProbabilitiesForEveryMatchNumber() {
        Round round = createTopptipsetRound8Matches();
        ModelInput input = createNeutralModelInputForRound(round);
        GameModel model = new EnsembleModel();

        ModelSelectionResult result = model.generateSelection(round, input, 100);

        assertEquals(8, result.internalProbabilities().size());

        for (int i = 1; i <= 8; i++) {
            ProbabilityTriple probs = result.internalProbabilities().get(i);
            double sum = probs.get(Outcome.HOME_WIN) + probs.get(Outcome.DRAW) + probs.get(Outcome.AWAY_WIN);
            assertEquals(1.0, sum, 1e-9);
        }
    }

    @Test
    void providerProbabilitiesShouldInfluenceInternalProbabilityAndBasePick() {
        Round round = createTopptipsetRound8Matches();

        Map<Integer, MatchContext> ctx = new HashMap<>();
        for (Match match : round.getMatches()) {
            ctx.put(match.getMatchNumber(), new MatchContext(
                    ProbabilityTriple.fromProbabilities(0.50, 0.25, 0.25),
                    ProbabilityTriple.fromProbabilities(0.50, 0.25, 0.25),
                    List.of()
            ));
        }

        ctx.put(1, new MatchContext(
                ProbabilityTriple.fromProbabilities(0.45, 0.30, 0.25),
                ProbabilityTriple.fromProbabilities(0.45, 0.30, 0.25),
                List.of(
                        ProbabilityTriple.fromProbabilities(0.20, 0.20, 0.60)
                )
        ));

        GameModel model = new EnsembleModel();
        ModelSelectionResult result = model.generateSelection(round, new ModelInput(ctx), 1);

        assertEquals(Set.of(Outcome.AWAY_WIN), result.selections().get(1));

        ProbabilityTriple internal = result.internalProbabilities().get(1);
        assertEquals((0.45 + 0.20) / 2.0, internal.get(Outcome.HOME_WIN), 1e-9);
        assertEquals((0.30 + 0.20) / 2.0, internal.get(Outcome.DRAW), 1e-9);
        assertEquals((0.25 + 0.60) / 2.0, internal.get(Outcome.AWAY_WIN), 1e-9);
    }

    @Test
    void tagsShouldAffectInternalProbabilitiesBeforeSelection() {
        Round round = createTopptipsetRound8Matches();

        Map<Integer, MatchContext> contexts = new HashMap<>();
        Map<Integer, MatchInterventions> interventions = new HashMap<>();

        for (Match match : round.getMatches()) {
            contexts.put(match.getMatchNumber(), new MatchContext(
                    ProbabilityTriple.fromProbabilities(0.50, 0.25, 0.25),
                    ProbabilityTriple.fromProbabilities(0.50, 0.25, 0.25),
                    List.of()
            ));
        }

        contexts.put(1, new MatchContext(
                ProbabilityTriple.fromProbabilities(0.60, 0.20, 0.20),
                ProbabilityTriple.fromProbabilities(0.60, 0.20, 0.20),
                List.of()
        ));

        interventions.put(1, new MatchInterventions(
                List.of(MatchTag.neutralVenue(), MatchTag.keyAbsence(Side.HOME)),
                null
        ));

        GameModel model = new EnsembleModel();
        ModelSelectionResult result = model.generateSelection(round, new ModelInput(contexts, interventions), 1);

        ProbabilityTriple adjusted = result.internalProbabilities().get(1);
        assertEquals(0.54, adjusted.get(Outcome.HOME_WIN), 1e-9);
        assertEquals(0.22, adjusted.get(Outcome.DRAW), 1e-9);
        assertEquals(0.24, adjusted.get(Outcome.AWAY_WIN), 1e-9);
    }

    @Test
    void buffsShouldAffectInternalProbabilitiesBeforeSelection() {
        Round round = createTopptipsetRound8Matches();

        Map<Integer, MatchContext> contexts = new HashMap<>();
        Map<Integer, MatchInterventions> interventions = new HashMap<>();

        for (Match match : round.getMatches()) {
            contexts.put(match.getMatchNumber(), new MatchContext(
                    ProbabilityTriple.fromProbabilities(0.50, 0.25, 0.25),
                    ProbabilityTriple.fromProbabilities(0.50, 0.25, 0.25),
                    List.of()
            ));
        }

        contexts.put(1, new MatchContext(
                ProbabilityTriple.fromProbabilities(0.40, 0.31, 0.29),
                ProbabilityTriple.fromProbabilities(0.40, 0.31, 0.29),
                List.of()
        ));

        interventions.put(1, new MatchInterventions(
                List.of(),
                new MatchBuff(Outcome.AWAY_WIN, 3)
        ));

        GameModel model = new EnsembleModel();
        ModelSelectionResult result = model.generateSelection(round, new ModelInput(contexts, interventions), 1);

        ProbabilityTriple adjusted = result.internalProbabilities().get(1);
        assertEquals(0.385, adjusted.get(Outcome.HOME_WIN), 1e-9);
        assertEquals(0.295, adjusted.get(Outcome.DRAW), 1e-9);
        assertEquals(0.32, adjusted.get(Outcome.AWAY_WIN), 1e-9);

        assertEquals(Set.of(Outcome.AWAY_WIN), result.selections().get(1));
    }

    @Test
    void shouldRejectRoundInputThatExceedsBuffQuota() {
        Round round = createTopptipsetRound8Matches();

        Map<Integer, MatchContext> contexts = new HashMap<>();
        Map<Integer, MatchInterventions> interventions = new HashMap<>();

        for (Match match : round.getMatches()) {
            contexts.put(match.getMatchNumber(), new MatchContext(
                    ProbabilityTriple.fromProbabilities(0.50, 0.25, 0.25),
                    ProbabilityTriple.fromProbabilities(0.50, 0.25, 0.25),
                    List.of()
            ));
        }

        interventions.put(1, new MatchInterventions(List.of(), new MatchBuff(Outcome.HOME_WIN, 5)));
        interventions.put(2, new MatchInterventions(List.of(), new MatchBuff(Outcome.HOME_WIN, 5)));
        interventions.put(3, new MatchInterventions(List.of(), new MatchBuff(Outcome.HOME_WIN, 5)));
        interventions.put(4, new MatchInterventions(List.of(), new MatchBuff(Outcome.HOME_WIN, 5)));
        interventions.put(5, new MatchInterventions(List.of(), new MatchBuff(Outcome.HOME_WIN, 5)));
        interventions.put(6, new MatchInterventions(List.of(), new MatchBuff(Outcome.HOME_WIN, 5)));
        interventions.put(7, new MatchInterventions(List.of(), new MatchBuff(Outcome.HOME_WIN, 5)));
        interventions.put(8, new MatchInterventions(List.of(), new MatchBuff(Outcome.HOME_WIN, 5)));

        // quota for 8-match round = 8 * 5 = 40, so 8x5 is still valid.
        interventions.put(9, new MatchInterventions(List.of(), new MatchBuff(Outcome.HOME_WIN, 5)));

        GameModel model = new EnsembleModel();

        assertThrows(
                IllegalArgumentException.class,
                () -> model.generateSelection(round, new ModelInput(contexts, interventions), 1)
        );
    }
}