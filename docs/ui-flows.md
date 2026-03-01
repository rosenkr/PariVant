# UI MVP Flows (Public)

This document defines the minimal public UI flows we will implement first.
Notes:
- The public UI is read-only: it does NOT trigger new model runs.
- Budget selection on the public UI is via presets only (32/64/128/256).
- “Current round” selection happens server-side and is exposed through `/public/current`.

## Flow 1: Landing page shows the current round + default model selections

Goal:
- When a user enters the site, they immediately see a meaningful round (for a selected game type) and the model’s default selections for that round.

Steps:
1. UI loads `/public/current` (optionally with `?gameType=...`).
2. Backend decides which round is “current” for that game type:
    - If a round is RUNNING → return that
    - Else if a round is UPCOMING → return the next upcoming
    - Else → return the most recent FINISHED
3. UI renders:
    - Selected game type (e.g. Stryktipset default)
    - Round status: UPCOMING / RUNNING / FINISHED
    - Match list (matchNumber, teams, kickoff time)
4. UI fetches `/public/rounds/{roundId}/model-runs` and picks the default preset (64 SEK).
5. UI highlights the model’s selections per match (1 / X / 2, including half/full guards).

Acceptance:
- User sees a round and a model selection without clicking anything.

## Flow 2: User switches game type (Stryktipset / Europatipset / Topptipset)

Goal:
- User can switch game type via dropdown and see the relevant “current” round for that type.

Steps:
1. User selects a game type in the UI.
2. UI calls `/public/current?gameType=...`.
3. UI renders the returned round and status.
4. UI fetches `/public/rounds/{roundId}/model-runs` and defaults to 64 SEK preset again.

Acceptance:
- Switching game type updates the round + selections accordingly.

## Flow 3: User switches model-run preset (32 / 64 / 128 / 256)

Goal:
- User can change which precomputed model run is displayed.

Steps:
1. UI has a preset dropdown: 32 / 64 / 128 / 256.
2. UI already has `/public/rounds/{roundId}/model-runs` loaded.
3. When the user changes preset, UI selects the matching model run (same `budgetInSek`) and re-renders selections.

Rules:
- Public UI does not request new model runs.
- If the selected budget preset is missing, UI shows a “not available yet” state (and can default back to 64).

Acceptance:
- Switching presets changes which guards are highlighted.

## Flow 4: Round status display and messaging

Goal:
- Show status clearly and behave slightly differently depending on it.

Rules:
- UPCOMING:
    - betting open (in real world), but our site is still read-only
    - show “Starts at …”
- RUNNING:
    - show “Round is running”
    - selections shown as “snapshot” (still useful for transparency/history)
- FINISHED:
    - show “Round finished”
    - (future) can show results / performance (not MVP)

Acceptance:
- Status is visible and understandable with minimal UI text.