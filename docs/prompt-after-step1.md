Step 1 is complete.

Current status
- The project is now split into a multi-module Maven structure:
   - root `betting-model` = parent/aggregator pom
   - `backend` = Spring Boot application module
   - `domain` = extracted domain/model module
- The extracted module currently contains:
   - `ar.ss.betting.domain`
   - `ar.ss.betting.model`
- Package names were preserved during extraction. This step only changed module structure, not design.
- `mvn clean test` passes.
- The backend application starts successfully from the `backend` module.

Temporary decisions / known limitations
- This was a structural extraction only. No intentional redesign of domain/model logic has been done yet.
- `DecisionParameters` and `AdjustmentWeights` still exist for now, but they are candidates for removal in step 2.
- Existing naming such as `RuleBasedModel`, `GameRound`, and related inputs/contexts has not yet been cleaned up.
- The RuleBasedModel is effectively to be turned into `EnsembleModel`
- The current model behavior is still based on the pre-refactor implementation.

Domain context
- The code models a round of football matches with 3-outcome matches (Stryktipset, Europatipset, Topptipset).
- The current implementation returns a model selection constrained by a budget, where cost is based on row count.
- The current selection logic is layered in two steps:
   - value-based
   - coverage-based
- Inputs currently include market/public pick probabilities and recent home form score.
- Recent home form score should disappear in the redesign.
- Ordering by uncertainty should remain.
- Value should continue to mean:
   - `value = internal probability - public probability`
- internal probability is a simple average over all available sources of truth, hence the model is an EnsembleModel.
- Internal probability formula, for M = market probability triple {1,x,2}, P_i = provider i probability triple, n = number of providers: (M + sum(P_i for i = 1..n)) / (n+1)

Step 2 goal
We now want to redesign the extracted domain/model module in small safe steps.

Important intended changes
1. The model should move away from self-made weighted signals and toward an ensemble-based internal probability model.
2. The model should output both:
   - the final selection result
   - the full internal probabilities for each match
3. Recent home form score should disappear.
4. `DecisionParameters` and `AdjustmentWeights` should disappear initially, though similar ideas may later reappear through Tags/Buffs or a small static configuration holder.
5. `GameRound` may become `Round`.
6. `RuleBasedModel` may become `EnsembleModel`.
7. Full guard logic should be removed completely.
8. Hardcoded probability floors, value thresholds, and full-guard limits should be removed or replaced by a simpler explicit config approach.

Selection logic change to evaluate carefully
In `BaseOutcomeSelector`, selection should no longer be:
- argmax(internal) -> value comparison -> threshold override

Instead, evaluate a value-aware score like:

score = (internal - public) * exp(-k * (1 - internal))

where:
- `internal` is the model’s internal probability for that outcome
- `public` is the public pick probability
- `k` is a tunable aggressiveness parameter in roughly [1, 5]
- lower `k` = more aggressive toward underdog value
- higher `k` = more conservative

Tentative desired behavior:
- compute internal probabilities first
- compute score per outcome
- `basePick = argmax(score)`

But do not assume this is final without analyzing ripple effects.

New model direction
- Internal probabilities should be based on aggregation/ensemble rather than recent form heuristics.
- Aggregation method should be a weighted linear pool, with equal weights as the default:

  p = 1/(n+1) * (m + sum(q_i))

  where:
   - `m` = market probability triple
   - `q_i` = provider/model probability triples

- Value remains based on internal probability minus public probability.
- Phase 1 picks by value.
- Phase 2 ranks matches by uncertainty/entropy and reapplies picks by value to the most uncertain matches.
- When picks are applied again in phase 2, they should still be based on value.

Manual intervention requirements
The user must be able to rerun the model with runtime client inputs layered on top of the base model.

Two forms of manual inputs are planned:
1. Buffs
   - user-provided subjective direct probability override
   - example:
     selected_outcome += x
     draw -= x/3
     opponent -= 2x/3

2. Tags
   - structured deterministic probability shifts
   - examples:
      - rainy -> drawish +2%
      - grass familiarity advantage -> +2%
      - key players injured/benched -> -10%
      - cup game -> less drawish / clearer winner +2%
      - low-stakes end-of-season situation -> disadvantage shift

Important constraint:
- Tags/Buffs should be able to be absent with zero effect by default
- but must be possible to apply later when the user reruns the model
- they should affect internal probabilities before value generation, not replace the full pipeline

Transparency requirement
- The redesigned model should expose its internal probability output for each match, similar to how market/public percentages are exposed now.

Your task for step 2
1. Analyze the current extracted `domain` module as it exists after step 1.
2. Propose the smallest safe redesign toward the new ensemble-based model.
3. Ensure the redesign will support returning both:
   - final selection result
   - full internal probabilities for each match
4. Identify which classes should be renamed, removed, or changed first.
5. Propose the JUnit test plan before any coding.
6. Do not refactor everything at once.
7. Preserve behavior where not explicitly changed.
8. Call out assumptions instead of inventing missing details.

Please start by:
- summarizing the current design from the extracted module
- identifying the smallest safe migration order
- listing the classes most likely to change first
- proposing the tests to add/update before implementation
- not writing code yet

If you notice inconsistencies, you may briefly point them out, but focus on the task as requested.