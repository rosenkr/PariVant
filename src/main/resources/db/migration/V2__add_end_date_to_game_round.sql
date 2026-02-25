ALTER TABLE game_round
    ADD COLUMN end_date TIMESTAMP;

UPDATE game_round
SET end_date = start_date + INTERVAL '2 hours'
WHERE end_date IS NULL;

ALTER TABLE game_round
    ALTER COLUMN end_date SET NOT NULL;