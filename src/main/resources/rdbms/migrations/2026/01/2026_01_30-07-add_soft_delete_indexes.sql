--liquibase formatted sql
--changeset slair:2026_01_30-07-add_soft_delete_indexes

CREATE INDEX IF NOT EXISTS idx_agents_deleted_at ON agents(deleted_at);
CREATE INDEX IF NOT EXISTS idx_tasks_deleted_at ON tasks(deleted_at);
