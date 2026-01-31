--liquibase formatted sql
--changeset slair:2026_01_31-06-create_task_runs_audit_table

CREATE TABLE task_runs_audit (
    id BIGSERIAL PRIMARY KEY,
    task_run_id BIGINT NOT NULL,
    action VARCHAR(30) NOT NULL,
    status VARCHAR(30),
    actor_user_id BIGINT,
    actor_username VARCHAR(100),
    request_id VARCHAR(100),
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX task_runs_audit_task_run_id_idx ON task_runs_audit(task_run_id);
CREATE INDEX task_runs_audit_actor_user_id_idx ON task_runs_audit(actor_user_id);
