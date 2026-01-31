--liquibase formatted sql
--changeset slair:2026_01_31-08-add_active_id_indexes

CREATE INDEX idx_agents_active_id ON agents (id) WHERE deleted_at IS NULL;
CREATE INDEX idx_tasks_active_id ON tasks (id) WHERE deleted_at IS NULL;
