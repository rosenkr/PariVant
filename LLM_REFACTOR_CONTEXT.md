# LLM Refactor Context: domain/model extraction + model redesign

## Purpose of this document
This file is the operating brief for a new LLM context.

The goal is **not** to refactor immediately. The goal is to:
1. understand the current `domain` + `model` slice,
2. plan a safe extraction into a top-level module,
3. verify the extracted module compiles and works when hooked up,
4. then plan and execute the model redesign in controlled steps,
5. update/create JUnit tests as part of the work.

Work in small, reversible steps. Preserve behavior unless a change is explicitly requested in this document.

---

## Current project structure
Main app package today:
- `src/main/java/ar/ss/betting/api`
- `src/main/java/ar/ss/betting/config`
- `src/main/java/ar/ss/betting/domain`
- `src/main/java/ar/ss/betting/model`
- `src/main/java/ar/ss/betting/persistence`
- `src/main/java/ar/ss/betting/service`

Frontend also exists in a sibling top-level folder:
- `frontend/`

The first refactor target is only the Java code currently under:
- `src/main/java/ar/ss/betting/domain`
- `src/main/java/ar/ss/betting/model`

---

## Refactor mission

### Step 1: Extraction / modularization
Move `domain` + `model` into a top-level module such as `domain-model`.

Target outcome for step 1:
- the extracted code builds as its own Maven module,
- the main backend can depend on that module,
- the application still compiles after wiring it back in,
- existing behavior is preserved as much as possible,
- tests are updated or recreated to protect the extraction.

This step is about **structure and boundaries**, not yet about changing model logic.

### Step 2: Model redesign
After step 1 is stable, redesign the model part.

Target outcome for step 2:
- keep the useful parts of the current design,
- remove the obsolete signal-based logic,
- introduce an ensemble-based internal probability model,
- keep the two-phase selection flow,
- support future user runtime adjustments (`tags` and `buffs`),
- remove old concepts that no longer belong.

---

## High-level domain description
The code models a betting round made up of football matches with 3 possible outcomes:
- `HOME_WIN`
- `DRAW`
- `AWAY_WIN`

A model produces a set of selected outcomes per match under a budget constraint.

Current cost model:
- cost is based on number of rows,
- row count is the Cartesian product of the number of selected outcomes per match,
- total cost currently equals number of rows.

Current selection flow is roughly:
1. compute internal probabilities,
2. choose a base outcome per match,
3. rank matches by uncertainty,
4. add coverage (guards) according to budget.

---

## Current code inventory

### Domain classes
- `GameType`
  - enum of round types (`STRYKTIPSET`, `EUROPATIPSET`, `TOPPTIPSET`)
  - currently defines number of matches for each type
- `Match`
  - one match with match number, start date, home team, away team
- `GameRound`
  - one round with start date, game type, and list of matches
  - validates match count, numbering, and uniqueness of teams in a round
- `Coupon`
  - complete betting coupon for a round
  - holds selections and computes `numberOfRows()` / `totalCost()`
- `Team`
  - value object for team identity by normalized name
- `Outcome`
  - enum for 1X2 outcomes

### Model classes
- `GameModel`
  - strategy interface
- `RuleBasedModel`
  - current main implementation
  - computes internal probabilities, chooses base picks, ranks uncertainty, adds guards under budget
- `InternalProbabilityCalculator`
  - current internal probability engine
  - starts from market probabilities and adjusts with recent form
- `BaseOutcomeSelector`
  - chooses base outcome from internal vs public probabilities
- `ProbabilityTriple`
  - normalized probability value object for 3 outcomes
- `ModelInput`
  - round-level model input keyed by match number
- `MatchContext`
  - per-match market/public probabilities + recent form scores
- `ModelSelectionResult`
  - output from model layer
- `DecisionParameters`
  - currently holds probability floors, value thresholds, and max full-guard caps
- `AdjustmentWeights`
  - currently holds recent-form weight

---

## Current model behavior that should be understood before changing anything

### Base outcome selection
This behavior likely remains conceptually relevant, even if the details change.

#### `BaseOutcomeSelector`
**Purpose:** chooses the base outcome (single pick) for one match.

Current responsibilities:
- baseline outcome = `argMax(internal)`
- value per outcome = `internal(outcome) - public(outcome)`
- candidate outcomes must pass a probability floor from `DecisionParameters`
- choose the highest-value candidate
- switch from baseline only if value improvement exceeds threshold

This is the part that probably survives conceptually, but not necessarily with the same supporting classes.

### Current internal probability engine
Current implementation uses:
- market probabilities as baseline,
- recent form score as an adjustment,
- renormalization after adjustment.

This part is expected to change significantly.

### Current coverage logic
Current model then:
- ranks matches by uncertainty,
- applies half-guards,
- may upgrade some half-guards to full guards if slack allows,
- caps full guards by game type.

Full guard logic is expected to be removed in the redesign.

---

## Confirmed redesign direction for step 2

### Rename / conceptual cleanup
Likely intended changes:
- `RuleBasedModel` -> `EnsembleModel`
- `GameRound` -> `Round`
- remove `DecisionParameters`
- remove `AdjustmentWeights`
- remove full-guard logic completely

These should be treated as proposed refactor targets, not blindly applied without checking dependencies.

### New internal probability idea
The model should no longer be based on self-made weighted signals like recent form.

Instead:
- internal probability should be based on **market probability + provider ensemble**,
- default aggregation should be a **weighted linear pool**,
- default weights should be equal.

Formula:

```math
p = \frac{1}{n+1}\left(m + \sum_{i=1}^{n} q_i\right)
```

Where:
- `m` = market probability triple
- `q_i` = provider probability triple(s)
- `p` = aggregated internal probability triple

The model should also expose internal probabilities in output for transparency.

### New two-phase selection flow
Planned model flow:
1. **Phase 1:** pick outcomes by value
2. **Phase 2:** rank matches by uncertainty / entropy
3. for the most uncertain matches, apply picks again by value

Important:
- ordering by uncertainty should remain conceptually the same,
- phase 2 should still use value-based selection,
- exact mechanics should be designed carefully before coding.

### Recent form disappears
The following should be removed from the decision engine redesign:
- home recent form score
- away recent form score
- logic depending on recent form adjustments

This strongly affects:
- `MatchContext`
- `ModelInput`
- `InternalProbabilityCalculator` or its replacement
- any related tests

---

## New manual intervention requirements
The user must be able to rerun the model with runtime user input layered on top of the normal internal probability calculation.

There are two planned mechanisms.

### 1. Buffs
Subjective direct probability override.

Example rule:
- selected outcome `+= x`
- `DRAW -= x/3`
- opponent `-= 2x/3`

Interpretation:
- the user believes more in one side for a given match,
- this shifts internal probabilities before selection logic is applied,
- the value-generation logic should still run after this adjustment.

### 2. Tags
Structured deterministic probability shifts based on domain knowledge.

Examples:
- rainy -> drawish `+2%`
- one team often plays on grass and the other does not -> advantage `+2%`
- key players injured/benched -> disadvantage `-10%`
- cup game / Champions League type match -> less drawish, clearer winner more probable
- end-of-season motivation asymmetry -> directional advantage

Important requirement:
- tags and buffs should be possible to add later when the user reruns the model,
- default model run without tags/buffs should have no effect from them,
- the design should make these additions composable rather than hardcoded into every path.

---

## New scoring idea for value selection
Current value definition remains:

```text
value = internalProbability - publicProbability
```

But there is also a proposed penalty so the model does not over-favor low-probability outcomes simply because their gap is large.

Proposed scoring function:

```text
score = (prob - public) * exp(-k * (1 - prob))
```

Where:
- `prob` = internal probability for the outcome
- `public` = public pick probability
- `k` in `[1, 5]`
- lower `k` = more aggressive / likes underdog value more
- higher `k` = more conservative / less attracted to low-probability outcomes

This is a design candidate and should be evaluated before being made final.

---

## Things that should probably be removed
These are expected removals in the redesign unless dependency analysis shows a temporary migration need:
- full-guard logic completely
- hardcoded probability floors by game type
- hardcoded value thresholds by game type
- hardcoded max full-guard limits
- recent-form-based truth adjustment

There may still be a need for a **static constants/config class** for hardcoded values that remain temporarily changeable.

---

## Step 1 acceptance criteria
The LLM should help achieve all of the following before starting redesign work.

### Structural acceptance
- create a top-level module for `domain` + `model`, e.g. `domain-model`
- move or extract the relevant Java classes into that module
- keep package naming coherent and intentional
- minimize coupling back to `service`, `api`, `persistence`
- ensure no accidental Spring dependencies leak into the extracted module

### Build acceptance
- `domain-model` compiles on its own
- root/main project compiles after depending on `domain-model`
- imports and package references are updated cleanly
- JUnit tests compile and run

### Behavioral acceptance
- extraction alone should not intentionally alter selection logic
- if any behavior must temporarily change due to extraction, call it out explicitly
- preserve public interfaces as much as practical during step 1

### Test acceptance
At minimum, tests should cover:
- `ProbabilityTriple` normalization / validation
- `BaseOutcomeSelector` current decision behavior
- `GameRound` validation rules
- `Coupon` row-count / total-cost logic
- `RuleBasedModel` or current model smoke behavior under budget

---

## Step 2 acceptance criteria
The LLM should help plan and then implement the redesign in small increments.

### Redesign acceptance
- internal probabilities come from ensemble aggregation instead of recent form
- model supports provider probabilities plus market probability
- model output can expose internal probabilities for transparency
- tags and buffs can be applied when rerunning the model
- selection still uses value-based logic
- uncertainty ranking remains part of the flow
- full guards are removed

### Migration acceptance
- rename or reshape types only when the dependency impact is understood
- do not force a big-bang rewrite if an adapter or transitional type is safer
- update tests alongside design changes
- keep the system runnable after each major step

---

## Suggested migration plan

### Phase A: extract without redesign
1. identify the exact Java files in the current `domain` and `model` packages,
2. create a separate Maven module,
3. move/extract classes,
4. wire main app to depend on it,
5. repair imports and package refs,
6. add or repair tests,
7. verify compile + test green.

### Phase B: stabilize boundaries
1. inspect whether naming and package boundaries are good enough,
2. identify dependencies from service/api into model details,
3. decide which types are pure domain and which are engine/model concerns,
4. only then start redesign.

### Phase C: redesign model internals
1. replace recent-form-driven internal probability calculation,
2. introduce ensemble input types,
3. redesign `ModelInput` / `MatchContext`,
4. decide where tags/buffs live,
5. redesign scoring/value selection,
6. remove full-guard logic,
7. rename classes if still appropriate,
8. update tests.

---

## Suggested file handoff to the LLM
There are about 15 Java files in the current `domain` + `model` folders. That is a manageable amount.

Recommended handoff package for a new LLM context:

### Always include
1. this file,
2. the 15 Java files from `domain` + `model`,
3. relevant existing JUnit tests for those files if they already exist,
4. the current `pom.xml` files or module-related Maven files.

### Optional but useful
- one short tree of the current package/module structure,
- any compile errors encountered during extraction,
- any service/api classes directly depending on these types.

### Good prompt for the next LLM
Use something like:

```text
I am extracting my current domain + model code into a separate top-level Maven module.
Read the attached refactor context and the attached Java files.

Task order:
1. summarize the current design,
2. identify coupling and risks for extraction,
3. propose the exact module structure and move plan,
4. list all imports/usages likely to break,
5. propose JUnit tests needed to protect step 1,
6. do not redesign the model yet.

Constraints:
- preserve behavior in step 1,
- prefer minimal reversible changes,
- do not invent missing files or dependencies without labeling assumptions.
```

### Best way to hand over the 15 Java files
Do **not** manually summarize every class into prose first.

Instead, give the LLM:
- this brief,
- the actual files,
- optionally one index of file paths.

For 15 files, neat options are:
1. zip only the `domain` + `model` folders,
2. or paste a concatenated code bundle with file headers,
3. or attach the repo and point the LLM only at those folders.

Best practical choice:
- zip the exact `domain` + `model` slice,
- include this markdown file,
- include tests + pom files.

That gives full context without dumping the whole project.

---

## Proposed dependency boundary after extraction
The extracted module should ideally contain only pure Java/domain/model logic.

Good candidates to keep inside extracted module:
- round/match/team/outcome abstractions
- coupon/result abstractions
- probability types
- model input and output types
- selection engines and ranking logic

Things to avoid pulling into the extracted module unless necessary:
- Spring annotations / framework wiring
- persistence/JPA concerns
- controller/API transport concerns
- UI/frontend-specific shapes

---

## Known likely hotspots
The following files are likely to change first during redesign:
- `ModelInput`
- `MatchContext`
- `InternalProbabilityCalculator` (or replacement)
- `RuleBasedModel` (or replacement)
- `ModelSelectionResult`
- possibly `GameRound` if renamed to `Round`
- possibly `DecisionParameters` and `AdjustmentWeights` due to removal

The following files are likely to remain fairly stable conceptually:
- `Outcome`
- `Team`
- much of `Match`
- much of row/cost logic in `Coupon`
- much of `ProbabilityTriple`

---

## Important constraints for the LLM
- do not start with a full rewrite,
- do not change backend and UI contracts before the module extraction is stable,
- explain ripple effects before renaming central types,
- propose small steps with checkpoint builds/tests,
- treat Git commit boundaries as rollback points,
- update or recreate JUnit tests along the way.

---

## First concrete task for the next LLM
For the next session, focus only on **step 1**.

Specifically:
- inspect the attached `domain` and `model` Java files,
- propose the exact Maven module extraction plan,
- identify likely broken imports/usages,
- suggest the minimal test suite needed,
- then help perform the extraction in small steps.

Do **not** begin the ensemble-model redesign until step 1 is complete and compiling.
