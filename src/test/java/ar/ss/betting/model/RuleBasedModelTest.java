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
            ctx.put(match.getMatchNumber(), new MatchContext(market, pub, 5, 5));
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
    void shouldOnlyProduceSinglesOrHalfGuardsOrFullGuards() {
        GameRound round = createTopptipsetRound8Matches();
        ModelInput input = createNeutralModelInputForRound(round);
        GameModel model = new RuleBasedModel();

        ModelSelectionResult result = model.generateSelection(round, input, 100);

        for (Set<Outcome> sel : result.getSelections().values()) {
            assertTrue(sel.size() == 1 || sel.size() == 2 || sel.size() == 3);
        }
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
            ctx.put(match.getMatchNumber(), new MatchContext(market, pub, 5, 5));
        }

        ctx.put(1, new MatchContext(
                new ProbabilityTriple(0.55, 0.25, 0.20),
                new ProbabilityTriple(0.80, 0.10, 0.10),
                5, 5
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
            ctx.put(match.getMatchNumber(), new MatchContext(
                    new ProbabilityTriple(0.70, 0.20, 0.10),
                    new ProbabilityTriple(0.70, 0.20, 0.10),
                    5, 5
            ));
        }

        ctx.put(7, new MatchContext(
                new ProbabilityTriple(0.34, 0.30, 0.36),
                new ProbabilityTriple(0.34, 0.30, 0.36),
                5, 5
        ));

        ModelInput input = new ModelInput(ctx);

        GameModel model = new RuleBasedModel();
        ModelSelectionResult result = model.generateSelection(round, input, 2);

        long halfGuardCount = result.getSelections().values().stream()
                .filter(s -> s.size() == 2)
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
                    new ProbabilityTriple(0.50, 0.25, 0.25),
                    5, 5
            ));
        }

        ctx.put(1, new MatchContext(
                new ProbabilityTriple(0.60, 0.10, 0.30),
                new ProbabilityTriple(0.60, 0.10, 0.30),
                5, 5
        ));

        for (int i = 2; i <= 8; i++) {
            ctx.put(i, new MatchContext(
                    new ProbabilityTriple(0.90, 0.05, 0.05),
                    new ProbabilityTriple(0.90, 0.05, 0.05),
                    5, 5
            ));
        }

        ModelInput input = new ModelInput(ctx);

        GameModel model = new RuleBasedModel();
        ModelSelectionResult result = model.generateSelection(round, input, 2);

        assertEquals(Set.of(Outcome.HOME_WIN, Outcome.AWAY_WIN), result.getSelections().get(1));
    }

    @Test
    void recentFormWeightZeroShouldNotChangeBasePickFromMarket() {
        GameRound round = createTopptipsetRound8Matches();

        Map<Integer, MatchContext> ctx = new HashMap<>();
        for (Match match : round.getMatches()) {
            ctx.put(match.getMatchNumber(), new MatchContext(
                    new ProbabilityTriple(0.50, 0.25, 0.25),
                    new ProbabilityTriple(0.50, 0.25, 0.25),
                    5, 5
            ));
        }

        ctx.put(1, new MatchContext(
                new ProbabilityTriple(0.40, 0.30, 0.30),
                new ProbabilityTriple(0.40, 0.30, 0.30),
                0, 10
        ));

        ModelInput input = new ModelInput(ctx);

        GameModel model = new RuleBasedModel(new AdjustmentWeights(0.0));
        ModelSelectionResult result = model.generateSelection(round, input, 1);

        assertEquals(Set.of(Outcome.HOME_WIN), result.getSelections().get(1));
    }

    @Test
    void recentFormWeightShouldBeAbleToFlipBasePick() {
        GameRound round = createTopptipsetRound8Matches();

        Map<Integer, MatchContext> ctx = new HashMap<>();
        for (Match match : round.getMatches()) {
            ctx.put(match.getMatchNumber(), new MatchContext(
                    new ProbabilityTriple(0.50, 0.25, 0.25),
                    new ProbabilityTriple(0.50, 0.25, 0.25),
                    5, 5
            ));
        }

        ctx.put(1, new MatchContext(
                new ProbabilityTriple(0.40, 0.30, 0.30),
                new ProbabilityTriple(0.40, 0.30, 0.30),
                0, 10
        ));

        ModelInput input = new ModelInput(ctx);

        GameModel model = new RuleBasedModel(new AdjustmentWeights(0.30));
        ModelSelectionResult result = model.generateSelection(round, input, 1);

        assertEquals(Set.of(Outcome.AWAY_WIN), result.getSelections().get(1));
    }

    @Test
    void slackBudgetShouldAllowUpgradingHalfGuardToFullGuard() {
        GameRound round = createTopptipsetRound8Matches();
        ModelInput input = createNeutralModelInputForRound(round);

        // budget=3 -> model will create 1 half guard (cost 2) and should upgrade to a full guard (cost 3)
        GameModel model = new RuleBasedModel();
        ModelSelectionResult result = model.generateSelection(round, input, 3);

        assertEquals(3, result.getTotalCostInSek());
        assertEquals(1, result.getFullGuardsCount());

        long size3Count = result.getSelections().values().stream().filter(s -> s.size() == 3).count();
        assertEquals(1, size3Count);
    }

    @Test
    void fullGuardCapShouldBeEnforcedForTopptipset() {
        GameRound round = createTopptipsetRound8Matches();
        ModelInput input = createNeutralModelInputForRound(round);

        // Large budget could in theory upgrade multiple half-guards to full-guards,
        // but Topptipset default cap is 1.
        GameModel model = new RuleBasedModel();
        ModelSelectionResult result = model.generateSelection(round, input, 1000);

        assertTrue(result.getFullGuardsCount() <= 1);

        long size3Count = result.getSelections().values().stream().filter(s -> s.size() == 3).count();
        assertTrue(size3Count <= 1);
    }
}