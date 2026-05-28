CREATE TABLE IF NOT EXISTS coupon (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    round_id BIGINT NOT NULL REFERENCES round(id) ON DELETE CASCADE,

    status VARCHAR(32) NOT NULL DEFAULT 'UNDETERMINED',
    selections_json JSONB NOT NULL,
    correct_pick_count INT,

    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT uq_coupon_user_round UNIQUE (user_id, round_id),
    CONSTRAINT ck_coupon_status CHECK (status IN ('UNDETERMINED', 'WIN', 'LOSE')),
    CONSTRAINT ck_coupon_resolution_state CHECK (
        (status = 'UNDETERMINED' AND correct_pick_count IS NULL)
        OR
        (status IN ('WIN', 'LOSE') AND correct_pick_count IS NOT NULL AND correct_pick_count >= 0)
    )
);

CREATE INDEX IF NOT EXISTS idx_coupon_user_id ON coupon(user_id);
CREATE INDEX IF NOT EXISTS idx_coupon_round_id ON coupon(round_id);
