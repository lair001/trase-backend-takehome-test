--liquibase formatted sql
--changeset slair:2026_01_31-05-create_tasks_audit_table

CREATE TABLE tasks_audit (
    id BIGSERIAL PRIMARY KEY,
    task_id BIGINT NOT NULL,
    action VARCHAR(30) NOT NULL,
    actor_user_id BIGINT,
    actor_username VARCHAR(100),
    request_id VARCHAR(100),
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX tasks_audit_task_id_idx ON tasks_audit(task_id);
CREATE INDEX tasks_audit_actor_user_id_idx ON tasks_audit(actor_user_id);
