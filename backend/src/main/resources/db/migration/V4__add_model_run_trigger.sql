ALTER TABLE model_run
    ADD COLUMN trigger VARCHAR(32);

UPDATE model_run
SET trigger = 'MANUAL'
WHERE trigger IS NULL;

ALTER TABLE model_run
    ALTER COLUMN trigger SET NOT NULL;

CREATE UNIQUE INDEX uq_model_run_round_budget_trigger
    ON model_run (game_round_id, budget_in_sek, trigger);