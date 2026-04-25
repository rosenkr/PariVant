CREATE TABLE IF NOT EXISTS app_user (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    p_hash VARCHAR(255),
    role VARCHAR(20) NOT NULL,
    google_subject VARCHAR(255),
    CONSTRAINT uq_email UNIQUE (email),
    CONSTRAINT uq_sub UNIQUE (google_subject)
);