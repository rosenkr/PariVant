# API overview (MVP)

This file lists which endpoints exist, why they exist, and **the Java source file that defines them**.

---

## Public API (no authentication)

### GET /public/current
Purpose:
- Return “the round to show right now” (running if exists, else next upcoming) for a selected GameType.

Defined in:
- `src/main/java/ar/ss/betting/api/publicapi/PublicRoundController.java`

Notes:
- Accepts optional query param `gameType` (STRYKTIPSET, EUROPATIPSET, TOPPTIPSET).

---

### GET /public/rounds/{roundId}/model-runs
Purpose:
- Return persisted model runs for a round so the UI can display selections for preset budgets.

Defined in:
- `src/main/java/ar/ss/betting/api/publicapi/PublicModelRunController.java`

Notes:
- Public UI should not trigger fresh model computations; it should display stored runs.

---

## Internal API (development/admin; should be protected later)

### POST /internal/rounds
Purpose:
- Insert a new round (game type + start time + matches) into the database.

Defined in:
- `src/main/java/ar/ss/betting/api/internal/RoundController.java`

---

### POST /internal/rounds/{roundId}/model-runs
Purpose:
- Run the model for a stored round with provided match contexts and persist the result.

Defined in:
- `src/main/java/ar/ss/betting/api/internal/RoundController.java`

---

### POST /internal/ingest/rounds
Purpose:
- Trigger ingestion of round JSON files from the configured ingest folder (manual input pipeline).

Defined in:
- `src/main/java/ar/ss/betting/api/internal/IngestController.java`

---

## Health/utility

### GET /health
Purpose:
- Basic “service is alive” check.

Defined in:
- `src/main/java/ar/ss/betting/api/HealthController.java`

---

## Shared error handling

Purpose:
- Standardize API error responses (400/500 etc.).

Defined in:
- `src/main/java/ar/ss/betting/api/ApiExceptionHandler.java`