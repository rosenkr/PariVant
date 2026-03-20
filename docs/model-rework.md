
To keep in mind, probably do want somthing lik this left but must be sure of how it works:
### src/main/java/ar/ss/betting/model/BaseOutcomeSelector.java
**Purpose:** Chooses the **base outcome** (single pick) for one match, given probabilities and policy parameters.

**Main responsibilities:**
- Define a **baseline** outcome from internal probabilities: `argMax(internal)`.
- Compute **value** per outcome: `value(o) = internal(o) - public(o)`.
- Decide whether to override the baseline with a “value” pick:
  - Take the outcome with highest value.
  - Switch only if the value improvement clears a **game-type-specific threshold** from `DecisionParameters`.
- Enforce a **probability floor** (per game type) so the base pick doesn’t become too “wild” relative to internal probabilities.

------------------------------------------------------------
New model rework:
Base model: phase 1 picks by value, phase 2 ranks matches by uncertainty/entropy, for those with most, apply picks again
When applying in this phase, do by value again. Aggregation method: weighted linear pool, default equal weights(market + providers)
$$
p = \frac{1}{n+1}\left(m + \sum_{i=1}^{n} q_i\right)
$$
probable classes to change: ModelContext, ModelInput (home score signal disappears)
might as ell change GameRound to Round and remove DecisionParameter/Adjustmenteights
ill add Tag and slider-related logic
Prolly want a static class holding all hardcoded values subject to change
probably remove hardcoded"- probability floors (per game type)
- value thresholds (per game type)
- maximum full-guard limits (soft constraints)"""
- remove full guard logic completely

------------------------------------------
Domain knowledge: By client added in runtime and can request new model run with that info on top
Has Tags the user checks in boxes.
Buffs: Slider with quota: what user believes for a match. Shifts % towards that side. Total quota X = matches in round * 5, max quota per match Y  = X/

Tags ideas:
If rainy → drawish +2%
A team often plays on a grass the other doesn’t → advantage + 2%
Key players injured/benched → disadvantage -10%
Cup game (like CL)→ more goals → clear winner probable → non-drawish +2%
Team middle of table end of season cant be relegated/enter top of table? → disadvantage +2%

-rulebasedmodel rework -> internal = market probability + provider ensemble
RuleBasedModel -> EnsembleModel
-have model akso output its internal % like market/svf (transparency)

Q: When remodeling, regarding buffs/tags; need a way to bake these in as defaults (no effect if model ran without them, but should be able to be added later when rerun model by user)
i.e user must be able to intervene the calculation for Internal probabilities, but still apply the value generation. 

Manual inputs:
Buffs = subjective direct probability override:
selected_outcome += x, draw -= x/3, opponent -= 2x/3
Tags = structured info, deterministic probability shifts


value = internal prob - public prob
penalty so model doest always pick low probability events due to typically larger % gap there:
score = (prob - public) x exp(-k * (1-prob)) for k [1,5] where low k = aggressive, likes underdog value vs higher k prefers low value over dogs