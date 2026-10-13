CREATE TABLE users (
    id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(254) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT ck_users_name_not_blank CHECK (length(trim(name)) > 0),
    CONSTRAINT ck_users_email_normalized CHECK (
        email = lower(trim(email)) AND length(email) > 0
    )
);

CREATE TABLE medical_images (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    original_name VARCHAR(255) NOT NULL,
    storage_key VARCHAR(500) NOT NULL,
    mime_type VARCHAR(50) NOT NULL,
    size_bytes BIGINT NOT NULL,
    width INTEGER NOT NULL,
    height INTEGER NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_medical_images_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE RESTRICT,
    CONSTRAINT uq_medical_images_storage_key UNIQUE (storage_key),
    CONSTRAINT ck_medical_images_size CHECK (
        size_bytes > 0 AND size_bytes <= 10485760
    ),
    CONSTRAINT ck_medical_images_width CHECK (
        width > 0 AND width <= 4096
    ),
    CONSTRAINT ck_medical_images_height CHECK (
        height > 0 AND height <= 4096
    ),
    CONSTRAINT ck_medical_images_mime CHECK (
        mime_type IN ('image/png', 'image/jpeg')
    )
);

CREATE TABLE analyses (
    id UUID PRIMARY KEY,
    image_id UUID NOT NULL,
    algorithm VARCHAR(50) NOT NULL,
    parameters JSONB NOT NULL,
    status VARCHAR(20) NOT NULL,
    mask_key VARCHAR(500),
    overlay_key VARCHAR(500),
    foreground_pixels BIGINT,
    foreground_ratio DOUBLE PRECISION,
    processing_time_ms BIGINT,
    error_code VARCHAR(100),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMPTZ,

    CONSTRAINT fk_analyses_image
        FOREIGN KEY (image_id) REFERENCES medical_images(id)
        ON DELETE RESTRICT,
    CONSTRAINT ck_analyses_algorithm CHECK (
        length(trim(algorithm)) > 0
    ),
    CONSTRAINT ck_analyses_parameters CHECK (
        jsonb_typeof(parameters) = 'object'
    ),
    CONSTRAINT ck_analyses_status CHECK (
        status IN ('PROCESSING', 'COMPLETED', 'FAILED')
    ),
    CONSTRAINT ck_analyses_foreground_pixels CHECK (
        foreground_pixels >= 0
    ),
    CONSTRAINT ck_analyses_foreground_ratio CHECK (
        foreground_ratio >= 0 AND foreground_ratio <= 1
    ),
    CONSTRAINT ck_analyses_processing_time CHECK (
        processing_time_ms >= 0
    ),
    CONSTRAINT ck_analyses_completion_time CHECK (
        completed_at >= created_at
    ),
    CONSTRAINT ck_analyses_result_state CHECK (
        (
            status = 'PROCESSING'
            AND completed_at IS NULL
            AND error_code IS NULL
            AND mask_key IS NULL
            AND overlay_key IS NULL
            AND foreground_pixels IS NULL
            AND foreground_ratio IS NULL
            AND processing_time_ms IS NULL
        )
        OR
        (
            status = 'COMPLETED'
            AND completed_at IS NOT NULL
            AND error_code IS NULL
            AND mask_key IS NOT NULL
            AND overlay_key IS NOT NULL
            AND foreground_pixels IS NOT NULL
            AND foreground_ratio IS NOT NULL
            AND processing_time_ms IS NOT NULL
        )
        OR
        (
            status = 'FAILED'
            AND completed_at IS NOT NULL
            AND error_code IS NOT NULL
            AND mask_key IS NULL
            AND overlay_key IS NULL
            AND foreground_pixels IS NULL
            AND foreground_ratio IS NULL
        )
    )
);

CREATE INDEX idx_medical_images_user_created
    ON medical_images (user_id, created_at DESC, id DESC);

CREATE INDEX idx_analyses_image_created
    ON analyses (image_id, created_at DESC, id DESC);

CREATE INDEX idx_analyses_status_created
    ON analyses (status, created_at DESC, id DESC);
