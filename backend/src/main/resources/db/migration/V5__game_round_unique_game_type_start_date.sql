-- Prevent accidental duplicate rounds for the same game type and start time.
ALTER TABLE game_round
    ADD CONSTRAINT uq_game_round_game_type_start_date UNIQUE (game_type, start_date);