CREATE TABLE IF NOT EXISTS match_context (
  id BIGSERIAL PRIMARY KEY,
  game_round_id BIGINT NOT NULL REFERENCES game_round(id) ON DELETE CASCADE,
  match_number INT NOT NULL,

  -- probabilities are stored normalized in [0,1]
  market_home DOUBLE PRECISION NOT NULL,
  market_draw DOUBLE PRECISION NOT NULL,
  market_away DOUBLE PRECISION NOT NULL,

  public_home DOUBLE PRECISION NOT NULL,
  public_draw DOUBLE PRECISION NOT NULL,
  public_away DOUBLE PRECISION NOT NULL,

  home_recent_form_score INT NOT NULL,
  away_recent_form_score INT NOT NULL,

  created_at TIMESTAMP NOT NULL DEFAULT now(),

  CONSTRAINT uq_match_context_round_match UNIQUE (game_round_id, match_number)
);