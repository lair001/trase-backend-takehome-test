--liquibase formatted sql
--changeset slair:2026_01_31-09-create_task_run_idempotency_table

CREATE TABLE task_run_idempotency (
    id BIGSERIAL PRIMARY KEY,
    idempotency_key VARCHAR(128) NOT NULL,
    request_hash VARCHAR(64) NOT NULL,
    task_run_id BIGINT NOT NULL REFERENCES task_runs(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX task_run_idempotency_key_uq ON task_run_idempotency(idempotency_key);
CREATE INDEX task_run_idempotency_task_run_id_idx ON task_run_idempotency(task_run_id);
