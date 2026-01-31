--liquibase formatted sql
--changeset slair:2026_01_31-10-add_agent_name_unique_index

CREATE UNIQUE INDEX idx_agents_name_active_unique ON agents (name) WHERE deleted_at IS NULL;
