CREATE TABLE IF NOT EXISTS round (
    id BIGSERIAL PRIMARY KEY,
    round_type VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    start_date TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT uq_round_round_type_start_date UNIQUE (round_type, start_date)
);

CREATE TABLE IF NOT EXISTS match (
    id BIGSERIAL PRIMARY KEY,
    round_id BIGINT NOT NULL REFERENCES round(id) ON DELETE CASCADE,
    match_number INT NOT NULL,
    start_date TIMESTAMP NOT NULL,
    home_team_name VARCHAR(80) NOT NULL,
    away_team_name VARCHAR(80) NOT NULL,
    home_score INT NOT NULL DEFAULT 0,
    away_score INT NOT NULL DEFAULT 0,
    status VARCHAR(32) NOT NULL DEFAULT 'UPCOMING',
    CONSTRAINT uk_match_round_matchnumber UNIQUE (round_id, match_number)
);

CREATE TABLE IF NOT EXISTS match_context (
    id BIGSERIAL PRIMARY KEY,
    round_id BIGINT NOT NULL REFERENCES round(id) ON DELETE CASCADE,
    match_number INT NOT NULL,

    market_home DOUBLE PRECISION NOT NULL,
    market_draw DOUBLE PRECISION NOT NULL,
    market_away DOUBLE PRECISION NOT NULL,

    public_home DOUBLE PRECISION NOT NULL,
    public_draw DOUBLE PRECISION NOT NULL,
    public_away DOUBLE PRECISION NOT NULL,

    created_at TIMESTAMP NOT NULL DEFAULT now(),

    CONSTRAINT uq_match_context_round_match UNIQUE (round_id, match_number)
);

CREATE TABLE IF NOT EXISTS model_run (
    id BIGSERIAL PRIMARY KEY,
    round_id BIGINT NOT NULL REFERENCES round(id) ON DELETE CASCADE,
    model_name VARCHAR(64) NOT NULL,
    generated_at TIMESTAMP NOT NULL,
    budget_in_sek INT NOT NULL,
    total_cost_in_sek INT NOT NULL,
    half_guards_count INT NOT NULL,
    trigger VARCHAR(64) NOT NULL,
    selections_json JSONB NOT NULL,
    internal_probabilities_json JSONB NOT NULL
);

CREATE TABLE IF NOT EXISTS model_run_provider_prediction (
    id BIGSERIAL PRIMARY KEY,
    model_run_id BIGINT NOT NULL REFERENCES model_run(id) ON DELETE CASCADE,
    match_number INT NOT NULL,
    provider_name VARCHAR(64) NOT NULL,

    status VARCHAR(32) NOT NULL,
    message VARCHAR(255),

    requested_home_team_name VARCHAR(80) NOT NULL,
    requested_away_team_name VARCHAR(80) NOT NULL,

    resolved_home_team_name VARCHAR(80),
    resolved_away_team_name VARCHAR(80),

    kickoff TIMESTAMP,
    kickoff_raw VARCHAR(64),

    probability_home DOUBLE PRECISION,
    probability_draw DOUBLE PRECISION,
    probability_away DOUBLE PRECISION,

    fetched_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT now(),

    CONSTRAINT uq_model_run_match_provider
        UNIQUE (model_run_id, match_number, provider_name)
);

CREATE INDEX IF NOT EXISTS idx_mrpp_model_run_id
    ON model_run_provider_prediction(model_run_id);

CREATE INDEX IF NOT EXISTS idx_mrpp_model_run_match
    ON model_run_provider_prediction(model_run_id, match_number);

CREATE INDEX IF NOT EXISTS idx_model_run_round_id ON model_run(round_id);
CREATE INDEX IF NOT EXISTS idx_model_run_generated_at ON model_run(generated_at);
CREATE INDEX IF NOT EXISTS idx_match_round_id ON match(round_id);
CREATE UNIQUE INDEX IF NOT EXISTS uq_model_run_round_budget_trigger
    ON model_run (round_id, budget_in_sek, trigger);