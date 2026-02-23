package ar.ss.betting.model;

import ar.ss.betting.domain.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Basic rule-based implementation of GameModel.
 */
// TODO: Improve model
public class RuleBasedModel implements GameModel {

    @Override
    public Coupon generateCoupon(GameRound gameRound, int budgetInSek) {

        Map<Integer, Set<Outcome>> selections = new HashMap<>();

        // Placeholder logic:
        // Always pick HOME_WIN only.
        for (Match match : gameRound.getMatches()) {
            selections.put(
                    match.getMatchNumber(),
                    Set.of(Outcome.HOME_WIN)
            );
        }

        return new Coupon(
                gameRound,
                LocalDateTime.now(),
                budgetInSek,
                selections
        );
    }
}
