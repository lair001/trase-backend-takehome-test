--liquibase formatted sql
--changeset slair:2026_01_31-07-create_revoked_tokens_table

CREATE TABLE revoked_tokens (
    id BIGSERIAL PRIMARY KEY,
    jti VARCHAR(200) NOT NULL UNIQUE,
    revoked_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX revoked_tokens_expires_at_idx ON revoked_tokens(expires_at);
