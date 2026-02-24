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
            // Neutral-ish defaults: market favorite HOME_WIN, public similar
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
    void shouldOnlyProduceSinglesOrHalfGuardsInStepC1() {
        GameRound round = createTopptipsetRound8Matches();
        ModelInput input = createNeutralModelInputForRound(round);
        GameModel model = new RuleBasedModel();

        ModelSelectionResult result = model.generateSelection(round, input, 100);

        for (Set<Outcome> sel : result.getSelections().values()) {
            assertTrue(sel.size() == 1 || sel.size() == 2,
                    "Selection size must be 1 or 2 in Step C.1");
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

        // Only care about match 1 for this test; others can be neutral defaults.
        Map<Integer, MatchContext> ctx = new HashMap<>();

        for (Match match : round.getMatches()) {
            ProbabilityTriple market = new ProbabilityTriple(0.50, 0.25, 0.25);
            ProbabilityTriple pub = new ProbabilityTriple(0.50, 0.25, 0.25);
            ctx.put(match.getMatchNumber(), new MatchContext(market, pub));
        }

        // For match 1:
        // market: 1=0.55, X=0.25, 2=0.20
        // public: 1=0.80, X=0.10, 2=0.10
        // value gaps:
        // v(1)= -0.25, v(X)= +0.15, v(2)= +0.10 => best is DRAW
        ctx.put(1, new MatchContext(
                new ProbabilityTriple(0.55, 0.25, 0.20),
                new ProbabilityTriple(0.80, 0.10, 0.10)
        ));

        ModelInput input = new ModelInput(ctx);

        // Use budget=1 to avoid coverage overriding the selection size, making it a pure base-pick check.
        GameModel model = new RuleBasedModel();
        ModelSelectionResult result = model.generateSelection(round, input, 1);

        assertEquals(Set.of(Outcome.DRAW), result.getSelections().get(1));
    }
}