--liquibase formatted sql
--changeset slair:2026_01_30-04-create_task_runs_table

CREATE TABLE task_runs (
    id BIGSERIAL PRIMARY KEY,
    task_id BIGINT NOT NULL REFERENCES tasks(id) ON DELETE RESTRICT,
    agent_id BIGINT NOT NULL REFERENCES agents(id) ON DELETE RESTRICT,
    status VARCHAR(32) NOT NULL,
    started_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    completed_at TIMESTAMPTZ
);

CREATE INDEX idx_task_runs_status ON task_runs(status);
