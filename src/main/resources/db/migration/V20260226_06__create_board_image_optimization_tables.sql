-- Migration: Create board image optimization tables
-- Description:
--   Stores master image optimization state, generated variants, and async jobs
--   for community image processing (thumb/detail/poster).

CREATE TABLE IF NOT EXISTS board_image_asset (
    bia_seq BIGSERIAL PRIMARY KEY,
    bf_seq BIGINT NOT NULL UNIQUE,
    master_bucket VARCHAR(200) NOT NULL,
    master_path VARCHAR(1000) NOT NULL,
    mime_type VARCHAR(200),
    width INTEGER,
    height INTEGER,
    has_alpha BOOLEAN,
    is_animated BOOLEAN,
    optimization_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    last_error VARCHAR(4000),
    reg_date TIMESTAMP NOT NULL DEFAULT NOW(),
    upd_date TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_board_image_asset_board_file
        FOREIGN KEY (bf_seq) REFERENCES board_file (bf_seq) ON DELETE CASCADE,
    CONSTRAINT ck_board_image_asset_status
        CHECK (optimization_status IN ('PENDING', 'PROCESSING', 'DONE', 'FAILED'))
);

CREATE TABLE IF NOT EXISTS board_image_variant (
    biv_seq BIGSERIAL PRIMARY KEY,
    bia_seq BIGINT NOT NULL,
    variant_role VARCHAR(20) NOT NULL,
    format VARCHAR(20) NOT NULL,
    width INTEGER,
    height INTEGER,
    file_path VARCHAR(1000) NOT NULL,
    file_size INTEGER,
    reg_date TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_board_image_variant_asset
        FOREIGN KEY (bia_seq) REFERENCES board_image_asset (bia_seq) ON DELETE CASCADE,
    CONSTRAINT uq_board_image_variant_role_format UNIQUE (bia_seq, variant_role, format),
    CONSTRAINT ck_board_image_variant_role
        CHECK (variant_role IN ('thumb', 'detail', 'poster')),
    CONSTRAINT ck_board_image_variant_format
        CHECK (format IN ('avif', 'webp', 'jpeg', 'png'))
);

CREATE TABLE IF NOT EXISTS board_image_job (
    bij_seq BIGSERIAL PRIMARY KEY,
    bia_seq BIGINT NOT NULL,
    job_type VARCHAR(20) NOT NULL DEFAULT 'OPTIMIZE',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    attempt_count INTEGER NOT NULL DEFAULT 0,
    next_run_at TIMESTAMP NOT NULL DEFAULT NOW(),
    last_error VARCHAR(4000),
    reg_date TIMESTAMP NOT NULL DEFAULT NOW(),
    upd_date TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_board_image_job_asset
        FOREIGN KEY (bia_seq) REFERENCES board_image_asset (bia_seq) ON DELETE CASCADE,
    CONSTRAINT ck_board_image_job_type
        CHECK (job_type IN ('OPTIMIZE')),
    CONSTRAINT ck_board_image_job_status
        CHECK (status IN ('PENDING', 'PROCESSING', 'DONE', 'FAILED', 'RETRY'))
);

CREATE INDEX IF NOT EXISTS idx_board_image_variant_bia_seq ON board_image_variant (bia_seq);
CREATE INDEX IF NOT EXISTS idx_board_image_job_status_next_run ON board_image_job (status, next_run_at);
CREATE INDEX IF NOT EXISTS idx_board_image_asset_status ON board_image_asset (optimization_status);
