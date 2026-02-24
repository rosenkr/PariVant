package ar.ss.betting.model;

import ar.ss.betting.domain.*;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class RuleBasedModelTest {

    private GameRound createTopptipsetRound8Matches() {
        return new GameRound(
                LocalDateTime.now(),
                GameType.TOPPTIPSET,
                List.of(
                        new Match(1, LocalDateTime.now(), new Team("A"), new Team("B")),
                        new Match(2, LocalDateTime.now(), new Team("C"), new Team("D")),
                        new Match(3, LocalDateTime.now(), new Team("E"), new Team("F")),
                        new Match(4, LocalDateTime.now(), new Team("G"), new Team("H")),
                        new Match(5, LocalDateTime.now(), new Team("I"), new Team("J")),
                        new Match(6, LocalDateTime.now(), new Team("K"), new Team("L")),
                        new Match(7, LocalDateTime.now(), new Team("M"), new Team("N")),
                        new Match(8, LocalDateTime.now(), new Team("O"), new Team("P"))
                )
        );
    }

    private ModelInput createNeutralModelInputForRound(GameRound round) {
        Map<Integer, MatchContext> ctx = new HashMap<>();

        for (Match match : round.getMatches()) {
            ProbabilityTriple market = new ProbabilityTriple(0.50, 0.25, 0.25);
            ProbabilityTriple pub = new ProbabilityTriple(0.50, 0.25, 0.25);
            ctx.put(match.getMatchNumber(), new MatchContext(market, pub));
        }

        return new ModelInput(ctx);
    }

    @Test
    void shouldNotExceedBudget() {
        GameRound round = createTopptipsetRound8Matches();
        ModelInput input = createNeutralModelInputForRound(round);
        GameModel model = new RuleBasedModel();

        ModelSelectionResult result = model.generateSelection(round, input, 100);

        assertTrue(result.getTotalCostInSek() <= 100);
    }

    @Test
    void budget100ShouldReachAtLeast64() {
        GameRound round = createTopptipsetRound8Matches();
        ModelInput input = createNeutralModelInputForRound(round);
        GameModel model = new RuleBasedModel();

        ModelSelectionResult result = model.generateSelection(round, input, 100);

        assertTrue(result.getTotalCostInSek() >= 64);
    }

    @Test
    void budget2ShouldCost2AndReportOneHalfGuard() {
        GameRound round = createTopptipsetRound8Matches();
        ModelInput input = createNeutralModelInputForRound(round);
        GameModel model = new RuleBasedModel();

        ModelSelectionResult result = model.generateSelection(round, input, 2);

        assertEquals(2, result.getTotalCostInSek());
        assertEquals(1, result.getHalfGuardsCount());
    }

    @Test
    void budget1ShouldHaveOnlySingles() {
        GameRound round = createTopptipsetRound8Matches();
        ModelInput input = createNeutralModelInputForRound(round);
        GameModel model = new RuleBasedModel();

        ModelSelectionResult result = model.generateSelection(round, input, 1);

        assertEquals(1, result.getTotalCostInSek());

        for (Set<Outcome> sel : result.getSelections().values()) {
            assertEquals(1, sel.size());
        }
    }

    @Test
    void shouldReturnASelectionForEveryMatchNumber() {
        GameRound round = createTopptipsetRound8Matches();
        ModelInput input = createNeutralModelInputForRound(round);
        GameModel model = new RuleBasedModel();

        ModelSelectionResult result = model.generateSelection(round, input, 100);

        assertEquals(8, result.getSelections().size());

        for (int i = 1; i <= 8; i++) {
            assertTrue(result.getSelections().containsKey(i), "Missing selection for match " + i);
            assertFalse(result.getSelections().get(i).isEmpty(), "Empty selection for match " + i);
        }
    }

    @Test
    void halfGuardCountShouldMatchSelections() {
        GameRound round = createTopptipsetRound8Matches();
        ModelInput input = createNeutralModelInputForRound(round);
        GameModel model = new RuleBasedModel();

        ModelSelectionResult result = model.generateSelection(round, input, 100);

        long derivedHalfGuards = result.getSelections().values().stream()
                .filter(s -> s.size() == 2)
                .count();

        assertEquals(derivedHalfGuards, result.getHalfGuardsCount());
    }

    @Test
    void shouldOnlyProduceSinglesOrHalfGuardsInStepC2() {
        GameRound round = createTopptipsetRound8Matches();
        ModelInput input = createNeutralModelInputForRound(round);
        GameModel model = new RuleBasedModel();

        ModelSelectionResult result = model.generateSelection(round, input, 100);

        for (Set<Outcome> sel : result.getSelections().values()) {
            assertTrue(sel.size() == 1 || sel.size() == 2,
                    "Selection size must be 1 or 2 in Step C.2");
        }

        assertEquals(0, result.getFullGuardsCount());
    }

    @Test
    void totalCostShouldEqualProductOfSelectionSizes() {
        GameRound round = createTopptipsetRound8Matches();
        ModelInput input = createNeutralModelInputForRound(round);
        GameModel model = new RuleBasedModel();

        ModelSelectionResult result = model.generateSelection(round, input, 100);

        int derivedCost = result.getSelections().values().stream()
                .mapToInt(Set::size)
                .reduce(1, (a, b) -> a * b);

        assertEquals(derivedCost, result.getTotalCostInSek());
    }

    @Test
    void basePickShouldChooseOutcomeWithBestValueGap() {
        GameRound round = createTopptipsetRound8Matches();

        Map<Integer, MatchContext> ctx = new HashMap<>();
        for (Match match : round.getMatches()) {
            ProbabilityTriple market = new ProbabilityTriple(0.50, 0.25, 0.25);
            ProbabilityTriple pub = new ProbabilityTriple(0.50, 0.25, 0.25);
            ctx.put(match.getMatchNumber(), new MatchContext(market, pub));
        }

        ctx.put(1, new MatchContext(
                new ProbabilityTriple(0.55, 0.25, 0.20),
                new ProbabilityTriple(0.80, 0.10, 0.10)
        ));

        ModelInput input = new ModelInput(ctx);

        GameModel model = new RuleBasedModel();
        ModelSelectionResult result = model.generateSelection(round, input, 1);

        assertEquals(Set.of(Outcome.DRAW), result.getSelections().get(1));
    }

    @Test
    void coverageShouldPrioritizeMostUncertainMatch() {
        GameRound round = createTopptipsetRound8Matches();

        Map<Integer, MatchContext> ctx = new HashMap<>();
        for (Match match : round.getMatches()) {
            // Default: fairly certain (max=0.70 -> uncertainty 0.30)
            ctx.put(match.getMatchNumber(), new MatchContext(
                    new ProbabilityTriple(0.70, 0.20, 0.10),
                    new ProbabilityTriple(0.70, 0.20, 0.10)
            ));
        }

        // Make match 7 highly uncertain (max=0.36 -> uncertainty 0.64)
        ctx.put(7, new MatchContext(
                new ProbabilityTriple(0.34, 0.30, 0.36),
                new ProbabilityTriple(0.34, 0.30, 0.36)
        ));

        ModelInput input = new ModelInput(ctx);

        // budget=2 => exactly one half guard
        GameModel model = new RuleBasedModel();
        ModelSelectionResult result = model.generateSelection(round, input, 2);

        // Only match 7 should have size 2 (half-guard), because it's most uncertain.
        long halfGuardCount = result.getSelections().entrySet().stream()
                .filter(e -> e.getValue().size() == 2)
                .count();
        assertEquals(1, halfGuardCount);

        assertEquals(2, result.getSelections().get(7).size());
    }

    @Test
    void halfGuardShouldAddBestAlternativeAccordingToInternalProbabilities() {
        GameRound round = createTopptipsetRound8Matches();

        Map<Integer, MatchContext> ctx = new HashMap<>();
        for (Match match : round.getMatches()) {
            ctx.put(match.getMatchNumber(), new MatchContext(
                    new ProbabilityTriple(0.50, 0.25, 0.25),
                    new ProbabilityTriple(0.50, 0.25, 0.25)
            ));
        }

        // For match 1: market says HOME strongest, AWAY second, DRAW weakest
        // base pick in this test is enforced by budget=1? No, we want coverage so budget=2.
        // We'll set public = market so baseOutcome becomes HOME (baseline).
        ctx.put(1, new MatchContext(
                new ProbabilityTriple(0.60, 0.10, 0.30),
                new ProbabilityTriple(0.60, 0.10, 0.30)
        ));

        // Make match 1 the most uncertain among all by making others very certain.
        for (int i = 2; i <= 8; i++) {
            ctx.put(i, new MatchContext(
                    new ProbabilityTriple(0.90, 0.05, 0.05),
                    new ProbabilityTriple(0.90, 0.05, 0.05)
            ));
        }

        ModelInput input = new ModelInput(ctx);

        // budget=2 => one half guard applied to the most uncertain match (match 1)
        GameModel model = new RuleBasedModel();
        ModelSelectionResult result = model.generateSelection(round, input, 2);

        // Base for match 1 should be HOME (public==market, baseline is HOME).
        // Best alternative should be AWAY (0.30) rather than DRAW (0.10).
        assertEquals(Set.of(Outcome.HOME_WIN, Outcome.AWAY_WIN), result.getSelections().get(1));
    }
}