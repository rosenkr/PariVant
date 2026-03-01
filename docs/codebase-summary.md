# V2 Backend Codebase Review

V2 is defined as the code up until UI-related code work begins, i.e includes everything under src/. This document is written during a guided walkthrough.
For each file/package:
- purpose
- main responsibilities
- key dependencies
- important assumptions/constraints
##  Entry + runtime config

### src/main/java/ar/ss/betting/BettingApplication.java
**Purpose:** Application entry point for the backend Spring Boot server.  
**Responsibilities:**
- Boots the Spring ApplicationContext (component scanning, auto-configuration, dependency injection).
- Enables scheduled/background jobs (so `@Scheduled` tasks run, e.g. model-run scheduler).

### src/main/java/ar/ss/betting/config/CorsConfig.java
**Purpose:** Configure CORS so the browser-based frontend can call the backend API.  
**Responsibilities:**
- Allows cross-origin requests from approved origins (e.g. local React dev server `http://localhost:5173`).
- Defines allowed HTTP methods and headers for API calls.
  **Why it exists:**
- Without CORS, browsers block frontend → backend calls when they are on different origins (domain/port).

### src/main/java/ar/ss/betting/config/JacksonConfig.java
**Purpose:** Configure Jackson (JSON serialization/deserialization).  
**Responsibilities:**
- Defines an `ObjectMapper` bean used across the app.
- Registers the Java time module so `LocalDateTime` and other java.time types serialize/deserialize cleanly.
  **Why it exists:**
- Prevents boot/runtime issues when Spring needs an `ObjectMapper` bean (e.g. for ingestion / JSON parsing),
  and ensures consistent JSON behavior.

### src/main/resources/application.properties
**Purpose:** Central runtime configuration for local development.  
**Responsibilities:**
- Server configuration (port).
- Database connection (PostgreSQL on localhost:5432 with username/password/db name).
- Flyway configuration (migration locations and enabling schema migrations on startup).
- CORS allowed origins (local dev now; later will include deployed frontend origin).
  **Notes:**
- These settings will likely be split by environment later (local vs production), e.g. via Spring Profiles.

## Domain (V1 core)

### src/main/java/ar/ss/betting/domain/Team.java
**Purpose:** Represents a team in a match.  
**Key design choice (current):**
- Identified by `name` only (string).
- Validates `name` is non-null and non-blank.
  **Notes:**
- This is a deliberate simplification for MVP. If we later need to distinguish teams across sports/leagues or handle aliases, the domain/persistence will need a stronger identifier strategy.

### src/main/java/ar/ss/betting/domain/Outcome.java
**Purpose:** Represents the possible 1X2 outcomes used by Svenska Spel coupon games.  
**Values:**
- HOME_WIN (1), DRAW (X), AWAY_WIN (2)  
  **Notes:**
- This models Svenska Spel-style 1X2 betting outcomes. It is not trying to represent every sport’s outcome system.

### src/main/java/ar/ss/betting/domain/Match.java
**Purpose:** Represents one match in a round.  
**Key fields/assumptions:**
- `matchNumber` identifies the match position within a round (1..N).
- `homeTeam` and `awayTeam` represent the 1 (home) vs 2 (away) semantics of 1X2.
- `startDate` is the kickoff time for the match.
  **Notes:**
- Neutral venues are not modeled in the MVP (still represented as home/away for coupon semantics).

### src/main/java/ar/ss/betting/domain/GameType.java
**Purpose:** Defines supported Svenska Spel round types and how many matches they contain.  
**Current values:**
- TOPPTIPSET (8 matches)
- STRYKTIPSET (13 matches)
- EUROPATIPSET (13 matches)

### src/main/java/ar/ss/betting/domain/GameRound.java
**Purpose:** Represents a single round (coupon round) of a given GameType at a given time.  
**Responsibilities:**
- Holds round metadata (type, start time).
- Holds the list of matches for the round.
- Validates match count matches the GameType (e.g. 8 for Topptipset).
  **Notes:**
- Additional validation may exist (or be added later), such as match number constraints and uniqueness rules.

### src/main/java/ar/ss/betting/domain/Coupon.java
**Purpose:** Represents a betting coupon: a selection per match (single, half-guard, or full-guard).  
**Notes:**
- Overlaps conceptually with `ModelSelectionResult` (model output).
- Keeping `Coupon` in the domain can still make sense long-term because:
    - domain coupon = a bet artifact (user-created or model-generated)
    - model result = output of a specific model run (includes metadata like parameters, timing, etc.)
- Whether both remain depends on future “My Page” features and persistence needs.

## Model engine

### src/main/java/ar/ss/betting/model/GameModel.java
**Purpose:** Strategy interface for different models.  
**Responsibilities:**
- Defines a common contract for producing a selection for a given `GameRound` under a budget, using `ModelInput` and parameter objects.

---

### src/main/java/ar/ss/betting/model/ModelSelectionResult.java
**Purpose:** The model’s output artifact.  
**Responsibilities:**
- Stores the computed selections (matchNumber → set of outcomes).
- Stores computed cost + guard counts.
- Stores metadata (generatedAt, modelName, etc.) for persistence and UI rendering.
  **Notes:**
- Conceptually overlaps with domain `Coupon`; keeping both can still make sense if:
  - `Coupon` = domain betting artifact (user-made or model-made)
  - `ModelSelectionResult` = result of a specific run + metadata + parameters snapshot

---

### src/main/java/ar/ss/betting/model/ModelInput.java
**Purpose:** Input bundle to the model for a round.  
**Responsibilities:**
- Holds `MatchContext` for each matchNumber in the round.
- Defines the minimal “data contract” required to run the model (for now: market/public/form).

---

### src/main/java/ar/ss/betting/model/MatchContext.java
**Purpose:** Per-match data used by the model.  
**Responsibilities:**
- Stores market probabilities (bookmaker-implied)
- Stores public pick distribution (Svenska folket)
- Stores simple signals (currently: home/away recent form scores)
  **Notes:**
- Expected to grow with additional signals (injuries, weather, standings, H2H, etc.).
- Likely future refactor: introduce a `Signal` abstraction instead of adding fields forever.

---

### src/main/java/ar/ss/betting/model/ProbabilityTriple.java
**Purpose:** Value object representing 1X2 probabilities.  
**Responsibilities:**
- Holds three values: home/draw/away probabilities.
- Provides helper methods (e.g. argmax / best outcome) for decision logic.

---

### src/main/java/ar/ss/betting/model/AdjustmentWeights.java
**Purpose:** Tunable weights for “truth engine” signals (how strongly signals affect internal probabilities).  
**Current scope:**
- Contains only recent form weight.
  **Notes:**
- Intended to be user-configurable in the future (e.g. UI sliders).
- Expected range: [0.0, 1.0].
- Default values are a product decision: can be 0.0 (disabled by default) or something like 0.5.

---

### src/main/java/ar/ss/betting/model/DecisionParameters.java
**Purpose:** Tunable parameters controlling decision policy (risk/value thresholds + constraints).  
**Examples:**
- probability floors (per game type)
- value thresholds (per game type)
- maximum full-guard limits (soft constraints)

---

### src/main/java/ar/ss/betting/model/InternalProbabilityCalculator.java
**Purpose:** “Truth engine”: converts market probabilities into internal probabilities by applying signals.  
**Current behavior:**
- Starts from the market triple (market as a proxy for reality).
- Applies weighted adjustments from available signals (currently: recent form).
  **Philosophy:**
- Market is the baseline because it aggregates many informed bettors.
- Signals nudge the baseline rather than replace it.
  **Future direction:**
- Add a `Signal` abstraction (each signal can compute an adjustment based on MatchContext + weights).
- Consider interactions between signals (synergy/conflicts) only after single-signal behavior is stable.

---

### src/main/java/ar/ss/betting/model/RuleBasedModel.java
**Purpose:** Main “engine” implementation of `GameModel` that produces a coupon-like selection under a max budget.

**Main responsibilities:**
- **TruthEngine (layer 1):** For each match, compute **internal probabilities** using `InternalProbabilityCalculator` + `AdjustmentWeights`.
- **DecisionEngine (layer 1):** Choose a **base pick** (single outcome) per match using `BaseOutcomeSelector`, driven by:
  - internal probabilities (model view),
  - public probabilities (Svenska folket view),
  - `DecisionParameters` (risk/value policy).
- **CoverageEngine (layer 2):** Spend budget to add coverage:
  - Compute how many **half-guards** fit under the budget (`computeHalfGuards(...)`).
  - Rank matches by **uncertainty** (derived from internal probabilities).
  - Apply half-guards to the most uncertain matches (`expandToHalfGuard(...)`).
- **Full-guard soft constraints (C4):**
  - Only “upgrade” **half-guard → full-guard** if budget slack allows.
  - Cap upgrades using `decisionParameters.maxFullGuards(gameType)`.
  - Prefer upgrading the most uncertain half-guarded matches first.

**Key dependencies:**
- `BaseOutcomeSelector` (base decision policy)
- `InternalProbabilityCalculator` (truth engine)
- `AdjustmentWeights` (signal weights)
- `DecisionParameters` (risk/value thresholds + full-guard cap)
- `ModelSelectionResult` (output artifact)

**Important assumptions/constraints:**
- Input `ModelInput` must contain a `MatchContext` for every matchNumber in the round.
- Output is always consistent with coupon pricing: **total cost = product of selection sizes** (1/2/3 per match).


### src/main/java/ar/ss/betting/model/BaseOutcomeSelector.java
**Purpose:** Chooses the **base outcome** (single pick) for one match, given probabilities and policy parameters.

**Main responsibilities:**
- Define a **baseline** outcome from internal probabilities: `argMax(internal)`.
- Compute **value** per outcome: `value(o) = internal(o) - public(o)`.
- Decide whether to override the baseline with a “value” pick:
  - Take the outcome with highest value.
  - Switch only if the value improvement clears a **game-type-specific threshold** from `DecisionParameters`.
- Enforce a **probability floor** (per game type) so the base pick doesn’t become too “wild” relative to internal probabilities.

**Key dependencies:**
- `ProbabilityTriple` (internal + public)
- `DecisionParameters` (thresholds + probability floors)
- `GameType`, `Outcome`

**Important assumptions/constraints:**
- “Value” is currently defined strictly as **internal − public** (simple and explainable, but extensible later).
- The selector is intentionally independent from coverage allocation; it only returns the **single best base pick**.

### Potential refactors / naming
- Consider renaming: `GameRound` → `Round`, `GameType` → `RoundType` (optional).
- Introduce a `Signal` model to standardize:
  - how signals are normalized
  - how they apply adjustments
  - how they are weighted and composed
- Consider making “value gap” influence continuous (bigger gap → stronger effect) rather than a hard boolean switch.

## Service layer

### `src/main/java/ar/ss/betting/service/ingest/RoundIngestService.java`
**Purpose:** Implements the ingest workflow: import upcoming rounds into the DB from external-ish sources (currently JSON files on disk; later real APIs).

**Main responsibilities:**
- Reads JSON round definitions from a configured ingest folder.
- Parses JSON into an ingest DTO/structure.
- Validates ingest payload (required fields, match count matches `GameType`, match numbers, etc.).
- Applies duplicate detection (current policy: reject/skip if a round with same `GameType` + `startDate` already exists).
- Persists round and matches via `RoundPersistenceService` (or repositories behind it).

**Key dependencies:** `ObjectMapper` (Jackson), `RoundPersistenceService` / repositories.

**Important assumptions/constraints:** Ingest is only “getting round data into the system”. It does not decide current/next round by itself; and it does not need real external APIs yet.

---

### `src/main/java/ar/ss/betting/service/JsonUtil.java`
**Purpose:** Central JSON helper used mainly for JSONB persistence and consistent serialization.

**Main responsibilities:**
- Converts Java objects → JSON string (for storing JSONB columns).
- Converts JSON string → `JsonNode` or typed objects (for returning structured JSON in API responses).

**Key dependencies:** `ObjectMapper` (Jackson).

**Important assumptions/constraints:** This is infrastructure glue, not domain logic. JSONB is intentionally used for evolving structures.

---

### `src/main/java/ar/ss/betting/service/ModelDtoMapper.java`
**Purpose:** Maps API DTOs to domain/model objects and back (boundary translation layer).

**Main responsibilities:**
- Request DTO → `GameRound`, `ModelInput`, `MatchContext`, `AdjustmentWeights`, `DecisionParameters`.
- `ModelSelectionResult` → response DTO.
- Performs boundary validation (null/blank/format) to return clean 400 errors early.

**Key dependencies:** domain package (`GameRound`, `Match`, `Team`, `Outcome`), model package (`ModelInput`, `MatchContext`, `ProbabilityTriple`, etc.).

**Important assumptions/constraints:** Keeps controllers thin; prevents “DTO knowledge” leaking into domain/model.

---

### `src/main/java/ar/ss/betting/service/ModelRunScheduler.java`
**Purpose:** Background job that creates scheduled model runs for rounds at preset budgets and triggers.

**Main responsibilities:**
- Periodically scans DB for upcoming rounds.
- Ensures `OPENED` runs exist when a round becomes available.
- Ensures `T_MINUS_15` runs exist when `now >= roundStart - 15min` and still before start.
- Idempotency: never creates duplicates for (`roundId`, `trigger`, `budget`).
- Runs model for budgets `{32, 64, 128, 256}` and persists results.

**Key dependencies:** `RoundPersistenceService` / repositories, `ModelService` (or direct model runner).

**Important assumptions/constraints:** No runs after round start (betting closed). Scheduler is “snapshot generation”, not live updating.

---

### `src/main/java/ar/ss/betting/service/ModelService.java`
**Purpose:** Executes a `GameModel` with given round + model input + parameters to produce a `ModelSelectionResult`.

**Main responsibilities:**
- Owns the `GameModel` dependency (`RuleBasedModel` today; others later).
- Runs the model and returns the computed result.

**Important assumptions/constraints:** Keeps execution “pure”; persistence is handled elsewhere.

---

### `src/main/java/ar/ss/betting/service/RoundApiService.java`
**Purpose:** Application/use-case orchestration for API workflows involving rounds and model runs.

**Main responsibilities:**
- Creates rounds (request mapping + domain validation + persistence).
- Runs model for a round and persists it (`ModelService` + `RoundPersistenceService`).
- Implements “public use-cases” like selecting current/next round for a `GameType` (depending on where logic is placed).

**Important assumptions/constraints:** Orchestrates; does not contain heavy model logic; delegates to model and persistence services.

---

### `src/main/java/ar/ss/betting/service/RoundPersistenceService.java`
**Purpose:** Persistence-focused service wrapping repositories; saves/loads rounds and model-runs and encapsulates JSONB storage details.

**Main responsibilities:**
- Persist `GameRoundEntity` + `MatchEntity`.
- Persist `ModelRunEntity` including JSONB columns (selections, weights, decision parameters) and `trigger`.
- Query model runs for a round, “exists” checks for scheduler idempotency, lookup current/next round.

**Important assumptions/constraints:** Centralizes DB logic so controllers/services don’t scatter repository calls; should be the only layer (besides entities/repos) that “knows” table shapes.

## API layer (controllers, DTOs, exception handling)

### src/main/java/ar/ss/betting/api/ApiExceptionHandler.java
**Purpose:** Centralized exception-to-HTTP response mapping.

**Main responsibilities:**
- Converts common failures into stable HTTP responses for clients.
- Returns a consistent `ApiError` JSON shape for:
  - `IllegalArgumentException` → **400**
  - `HttpMessageNotReadableException` (bad JSON) → **400**
  - `DataIntegrityViolationException` → **400**
  - all other uncaught exceptions → **500** (with a minimal error message)

**Key dependencies:** Spring `@RestControllerAdvice`, `ResponseEntity`, exception classes.

**Important assumptions/constraints:**
- The handler is intentionally conservative (does not leak stack traces / internal details).
- Some errors that are “really 404” can currently present as generic errors depending on where thrown.

---

### src/main/java/ar/ss/betting/api/HealthController.java
**Purpose:** Lightweight liveness endpoint.

**Main responsibilities:**
- Exposes `GET /health` used to verify the server is up.

**Key dependencies:** Spring MVC annotations.

**Important assumptions/constraints:**
- This is a “process is running” check, not a deep dependency check.

---

### src/main/java/ar/ss/betting/api/ModelController.java
**Purpose:** “Model-only” endpoint used for running the engine without persistence (useful in early development / debugging).

**Main responsibilities:**
- Accepts a model selection request DTO.
- Delegates to `ModelService` to run the model.
- Returns a response DTO suitable for clients.

**Key dependencies:** `ModelService`, `ModelDtoMapper`.

**Important assumptions/constraints:**
- This controller is mainly a dev tool; the persisted workflow is handled via `/internal/...` + `/public/...` endpoints.

---

### src/main/java/ar/ss/betting/api/dto/ModelSelectionRequestDto.java
**Purpose:** Request contract for running the model (or creating a model run).

**Main responsibilities:**
- Defines the incoming JSON shape for:
  - round definition (gameType, roundStartDate, matches)
  - budget
  - per-match contexts (market/public/form)
  - tunables (`weights`, `decisionParameters`)

**Key dependencies:** None besides Java records.

**Important assumptions/constraints:**
- `ProbabilityTripleDto` fields are treated as *probability-like numbers* at the API boundary.
  - If an upstream API provides decimal odds, those should be converted before reaching this DTO (e.g., by ingest).

---

### src/main/java/ar/ss/betting/api/dto/ModelSelectionResponseDto.java
**Purpose:** Response contract for returning a model selection.

**Main responsibilities:**
- Provides a stable JSON response shape for a selection:
  - model metadata (name, generatedAt)
  - cost/guard counts
  - `selections` as matchNumber → list of outcome strings

**Key dependencies:** None besides Java records.

**Important assumptions/constraints:**
- Outcomes are serialized as enum names (e.g., `HOME_WIN`, `DRAW`, `AWAY_WIN`).

---

### src/main/java/ar/ss/betting/api/internal/IngestController.java
**Purpose:** Internal-only endpoint to trigger ingestion (import rounds into the DB).

**Main responsibilities:**
- Exposes endpoints under `/internal/ingest/...` (exact paths depend on implementation) that:
  - start an ingest run
  - return an ingest report/result

**Key dependencies:** `RoundIngestService`.

**Important assumptions/constraints:**
- Intended for admin/manual tooling (not public).
- Assumes an ingest folder exists and is accessible to the backend process (local filesystem in dev).

---

### src/main/java/ar/ss/betting/api/internal/RoundController.java
**Purpose:** Internal-only endpoints to manage rounds and to create persisted model runs.

**Main responsibilities:**
- `POST /internal/rounds` to create a `GameRound` + matches in the DB.
- `POST /internal/rounds/{roundId}/model-runs` to run the model for a round and persist the result.

**Key dependencies:** `RoundApiService`, DTOs from `ModelSelectionRequestDto`.

**Important assumptions/constraints:**
- This is for ingestion/admin workflows; the public site reads via `/public/...`.

---

### src/main/java/ar/ss/betting/api/internal/dto/FileIngestResultDto.java
**Purpose:** Response DTO describing the result of ingesting a single file.

**Main responsibilities:**
- Communicates per-file status such as:
  - processed / skipped / error
  - a message and/or created round id (depending on implementation)

**Key dependencies:** None besides Java records.

---

### src/main/java/ar/ss/betting/api/internal/dto/IngestReportDto.java
**Purpose:** Response DTO aggregating an ingest run.

**Main responsibilities:**
- Provides a list of `FileIngestResultDto` results.
- Provides summary counts (if implemented) like successes/failures/skips.

**Key dependencies:** `FileIngestResultDto`.

---

### src/main/java/ar/ss/betting/api/publicapi/PublicRoundController.java
**Purpose:** Public read endpoints for the website.

**Main responsibilities:**
- Exposes public read-only API under `/public/...`.
- Implements “current round selection” logic via `GET /public/current`:
  - optionally accepts `gameType`
  - returns either a RUNNING round of that type or the NEXT upcoming round
  - also returns computed `roundStatus`

**Key dependencies:** `RoundApiService` (or a read-focused service method).

**Important assumptions/constraints:**
- “Running” is computed from `start_date` and `end_date` (end-date currently derived as +2h).
- If no round is found for a type, controller/service may fall back to another type (if implemented).

---

### src/main/java/ar/ss/betting/api/publicapi/PublicModelRunController.java
**Purpose:** Public read endpoints for model runs.

**Main responsibilities:**
- Serves stored model runs, e.g. `GET /public/rounds/{roundId}/model-runs`.

**Key dependencies:** `RoundApiService` / persistence services.

**Important assumptions/constraints:**
- Intended output is JSON objects, not raw JSONB strings (uses `JsonNode` mapping).

---

## Persistence layer (entities + repositories)

### src/main/java/ar/ss/betting/persistence/entity/GameRoundEntity.java
**Purpose:** JPA entity mapping for the `game_round` table.

**Main responsibilities:**
- Persists round metadata:
  - `gameType`, `startDate`, `endDate`, `createdAt`
- Provides relationship mapping to matches.

**Key dependencies:** JPA (`jakarta.persistence.*`).

**Important assumptions/constraints:**
- Unique constraint exists on (`game_type`, `start_date`) at the DB level (see Flyway V5).
- `endDate` is currently a derived field (start + 2 hours).

---

### src/main/java/ar/ss/betting/persistence/entity/MatchEntity.java
**Purpose:** JPA entity mapping for the `match` table.

**Main responsibilities:**
- Stores matchNumber, startDate, and team names.
- Associates each match with its owning `GameRoundEntity`.

**Key dependencies:** JPA annotations.

**Important assumptions/constraints:**
- Match team names are stored as strings (no separate Team table yet).

---

### src/main/java/ar/ss/betting/persistence/entity/ModelRunEntity.java
**Purpose:** JPA entity mapping for the `model_run` table.

**Main responsibilities:**
- Stores the result of one model execution for one round:
  - modelName, generatedAt
  - budgetInSek, totalCostInSek
  - half/full guard counts
  - trigger (OPENED vs T_MINUS_15)
  - JSONB payloads:
    - selections
    - weights
    - decision parameters

**Key dependencies:** JPA, JSONB mapped as `String` columns in the entity.

**Important assumptions/constraints:**
- JSON is stored denormalized as JSONB for flexibility.
- Trigger is an enum at the domain/service level and persisted as a string/enum column.

---

### src/main/java/ar/ss/betting/persistence/repo/GameRoundRepository.java
**Purpose:** Spring Data JPA repository for rounds.

**Main responsibilities:**
- Basic CRUD via `JpaRepository`.
- Query helpers used by “current/next/running round” logic (e.g., ordering by startDate / filtering by gameType).

**Key dependencies:** Spring Data JPA.

---

### src/main/java/ar/ss/betting/persistence/repo/MatchRepository.java
**Purpose:** Spring Data JPA repository for matches.

**Main responsibilities:**
- Basic CRUD.
- Optional round-scoped lookups (depending on usage).

**Key dependencies:** Spring Data JPA.

---

### src/main/java/ar/ss/betting/persistence/repo/ModelRunRepository.java
**Purpose:** Spring Data JPA repository for model runs.

**Main responsibilities:**
- Query model runs for a given round (typically ordered newest-first).
- Existence checks for idempotency (roundId + budget + trigger).

**Key dependencies:** Spring Data JPA.

---

## Flyway migrations

> Migration files live under: `src/main/resources/db/migration/`

### V1__init.sql
**Purpose:** Creates initial schema for MVP.

**Creates:**
- `game_round`
- `match`
- `model_run`
- plus supporting indexes/constraints needed at the time.

---

### V2__add_end_date_to_game_round.sql
**Purpose:** Adds `end_date` to `game_round`.

**Why:**
- Enables “running vs upcoming vs finished” classification on the server.

---

### V3__game_round_created_at_default.sql
**Purpose:** Adds defaulting behavior for `created_at` (or adjusts it).

**Why:**
- Ensures `created_at` is populated consistently without relying on application code.

---

### V4__add_model_run_trigger.sql
**Purpose:** Adds the `trigger` column for `model_run`.

**Why:**
- Distinguishes “OPENED” runs from “T_MINUS_15” runs (or other future run types).

---

### V5__game_round_unique_game_type_start_date.sql
**Purpose:** Adds a uniqueness constraint for rounds.

**Constraint:**
- Enforces uniqueness on (`game_type`, `start_date`).

**Why:**
- Prevents ingest/admin duplication from creating multiple DB rows for what is conceptually the same round.