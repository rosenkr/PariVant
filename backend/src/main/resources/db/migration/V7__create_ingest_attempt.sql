-- Ingestion attempt log: stores success/failure of external ingestion calls.
-- We use timestamptz so timestamps are robust across deployments/timezones.

create table if not exists ingest_attempt (
    id bigserial primary key,
    source varchar(32) not null,                 -- e.g. TIPZER, ODDSONLINE
    game_type varchar(32) not null,              -- STRYKTIPSET/EUROPATIPSET/TOPPTIPSET
    endpoint varchar(128) not null,              -- e.g. /internal/ingest/tipzer/stryktipset/next
    status varchar(16) not null,                 -- SUCCESS/FAILED
    reason text null,                            -- short message
    details text null,                           -- longer (optional), e.g. exception class + stack snippet
    created_at timestamptz not null default now()
);

create index if not exists idx_ingest_attempt_created_at
    on ingest_attempt(created_at desc);

create index if not exists idx_ingest_attempt_status
    on ingest_attempt(status);

create index if not exists idx_ingest_attempt_source_game_type
    on ingest_attempt(source, game_type);