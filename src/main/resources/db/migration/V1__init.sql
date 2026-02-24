-- V1__init.sql
-- Initial schema for MVP persistence (JSONB selections).

CREATE TABLE IF NOT EXISTS game_round (
    id BIGSERIAL PRIMARY KEY,
    game_type VARCHAR(32) NOT NULL,
    start_date TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS match (
    id BIGSERIAL PRIMARY KEY,
    game_round_id BIGINT NOT NULL REFERENCES game_round(id) ON DELETE CASCADE,
    match_number INT NOT NULL,
    start_date TIMESTAMP NOT NULL,
    home_team_name VARCHAR(80) NOT NULL,
    away_team_name VARCHAR(80) NOT NULL,
    CONSTRAINT uk_match_round_matchnumber UNIQUE (game_round_id, match_number)
);

CREATE INDEX IF NOT EXISTS idx_match_round_id ON match(game_round_id);

CREATE TABLE IF NOT EXISTS model_run (
    id BIGSERIAL PRIMARY KEY,
    game_round_id BIGINT NOT NULL REFERENCES game_round(id) ON DELETE CASCADE,
    model_name VARCHAR(64) NOT NULL,
    generated_at TIMESTAMP NOT NULL,
    budget_in_sek INT NOT NULL,
    total_cost_in_sek INT NOT NULL,
    half_guards_count INT NOT NULL,
    full_guards_count INT NOT NULL,
    selections_json JSONB NOT NULL,
    weights_json JSONB NOT NULL,
    decision_parameters_json JSONB NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_model_run_round_id ON model_run(game_round_id);
CREATE INDEX IF NOT EXISTS idx_model_run_generated_at ON model_run(generated_at);
