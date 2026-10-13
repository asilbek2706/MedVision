CREATE TABLE storage_deletion_jobs (
    id UUID PRIMARY KEY,
    storage_key VARCHAR(500) NOT NULL UNIQUE,
    attempts BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    next_retry_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT ck_storage_deletion_key CHECK (
        storage_key ~ '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\.(png|jpg)$'
    ),
    CONSTRAINT ck_storage_deletion_attempts CHECK (attempts >= 0)
);

CREATE INDEX idx_storage_deletion_retry
    ON storage_deletion_jobs (next_retry_at, created_at, id);