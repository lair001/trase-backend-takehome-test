--liquibase formatted sql
--changeset slair:2026_01_30-03-create_task_supported_agents_table

CREATE TABLE task_supported_agents (
    task_id BIGINT NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
    agent_id BIGINT NOT NULL REFERENCES agents(id) ON DELETE CASCADE,
    PRIMARY KEY (task_id, agent_id)
);

CREATE INDEX idx_task_supported_agents_agent_id ON task_supported_agents(agent_id);
