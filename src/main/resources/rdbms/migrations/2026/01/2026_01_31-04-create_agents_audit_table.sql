--liquibase formatted sql
--changeset slair:2026_01_31-04-create_agents_audit_table

CREATE TABLE agents_audit (
    id BIGSERIAL PRIMARY KEY,
    agent_id BIGINT NOT NULL,
    action VARCHAR(30) NOT NULL,
    actor_user_id BIGINT,
    actor_username VARCHAR(100),
    request_id VARCHAR(100),
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX agents_audit_agent_id_idx ON agents_audit(agent_id);
CREATE INDEX agents_audit_actor_user_id_idx ON agents_audit(actor_user_id);
